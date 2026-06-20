package vn.campuslife.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.campuslife.entity.Activity;
import vn.campuslife.entity.ChatbotConversation;
import vn.campuslife.entity.ChatbotMessage;
import vn.campuslife.entity.EventArticle;
import vn.campuslife.entity.User;
import vn.campuslife.enumeration.ChatbotIntent;
import vn.campuslife.enumeration.ChatbotPageContext;
import vn.campuslife.enumeration.ChatbotMessageRole;
import vn.campuslife.enumeration.RegistrationStatus;
import vn.campuslife.enumeration.ScoreType;
import vn.campuslife.model.ChatbotActivityOptionResponse;
import vn.campuslife.model.ChatbotMessageRequest;
import vn.campuslife.model.ChatbotMessageResponse;
import vn.campuslife.model.ChatbotResolvedActivityResponse;
import vn.campuslife.repository.ActivityRegistrationRepository;
import vn.campuslife.repository.ActivityRepository;
import vn.campuslife.repository.ChatbotConversationRepository;
import vn.campuslife.repository.ChatbotMessageRepository;
import vn.campuslife.repository.EventArticleRepository;
import vn.campuslife.repository.UserRepository;
import vn.campuslife.service.RagService;
import vn.campuslife.service.ai.ChatbotNluResult;
import vn.campuslife.service.ai.ChatbotNluService;
import vn.campuslife.service.StudentService;
import vn.campuslife.service.ai.GeminiApiClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ChatbotServiceImpl implements vn.campuslife.service.ChatbotService {

    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Pattern TOKEN_PATTERN = Pattern.compile("[\\p{L}\\p{N}]+");
    private static final Pattern OPTION_INDEX_PATTERN = Pattern.compile("(?:so|số|chon|chọn)\\s*(\\d{1,2})");
    private static final Set<String> STOP_WORDS = Set.of(
            "su", "kien", "sự", "kiện", "cho", "minh", "mình", "toi", "tôi", "em", "anh", "chi", "chị",
            "ban", "bạn", "la", "là", "ve", "về", "nay", "này", "do", "đó", "duoc", "được", "khong", "không",
            "co", "có", "can", "cần", "hoi", "hỏi", "thong", "tin", "thông", "voi", "với", "gi", "gì");

    private final ChatbotConversationRepository conversationRepository;
    private final ChatbotMessageRepository messageRepository;
    private final UserRepository userRepository;
    private final ActivityRepository activityRepository;
    private final ActivityRegistrationRepository activityRegistrationRepository;
    private final StudentService studentService;
    private final ChatbotNluService chatbotNluService;
    private final EventArticleRepository eventArticleRepository;
    private final GeminiApiClient geminiApiClient;
    private final RagService ragService;
    @Override
    @Transactional
    public ChatbotMessageResponse chat(String username, ChatbotMessageRequest request) {
        String message = request.getMessage() == null ? "" : request.getMessage().trim();
        if (message.isBlank()) {
            return new ChatbotMessageResponse(null, "Bạn hãy nhập câu hỏi cụ thể hơn.", null, false, new ArrayList<>());
        }

        User user = userRepository.findByUsernameAndIsDeletedFalse(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        ChatbotConversation conversation = resolveConversation(user, request.getConversationId());

        EventArticle contextArticle = resolveContextArticle(request);
        Activity contextActivity = resolveContextActivity(conversation, request, contextArticle);
        if (contextActivity != null && (conversation.getContextActivity() == null
                || !conversation.getContextActivity().getId().equals(contextActivity.getId()))) {
            conversation.setContextActivity(contextActivity);
            conversationRepository.save(conversation);
        }

        messageRepository.save(new ChatbotMessage(null, conversation, ChatbotMessageRole.USER, message, false, null));

        ChatbotMessageResponse response = respond(conversation, username, message, contextActivity, contextArticle,
                request);

        messageRepository.save(new ChatbotMessage(null, conversation, ChatbotMessageRole.ASSISTANT,
                response.getAnswer(), false, null));

        response.setConversationId(conversation.getId());
        return response;
    }

    private ChatbotConversation resolveConversation(User user, Long conversationId) {
        if (conversationId == null) {
            ChatbotConversation created = new ChatbotConversation();
            created.setUser(user);
            created.setDeleted(false);
            return conversationRepository.save(created);
        }

        return conversationRepository.findByIdAndUser_IdAndIsDeletedFalse(conversationId, user.getId())
                .orElseGet(() -> {
                    ChatbotConversation created = new ChatbotConversation();
                    created.setUser(user);
                    created.setDeleted(false);
                    return conversationRepository.save(created);
                });
    }

    private EventArticle resolveContextArticle(ChatbotMessageRequest request) {
        if (request == null || request.getContextArticleSlug() == null || request.getContextArticleSlug().isBlank()) {
            return null;
        }
        return eventArticleRepository.findBySlugAndIsPublishedTrue(request.getContextArticleSlug().trim()).orElse(null);
    }

    private Activity resolveContextActivity(ChatbotConversation conversation, ChatbotMessageRequest request,
            EventArticle contextArticle) {
        if (contextArticle != null && contextArticle.getActivity() != null) {
            Activity a = contextArticle.getActivity();
            if (!a.isDeleted() && !a.isDraft()) {
                return a;
            }
        }
        Long contextActivityId = request.getContextActivityId();
        if (contextActivityId == null) {
            return request.getPageContext() == ChatbotPageContext.ACTIVITY_DETAIL ? conversation.getContextActivity()
                    : null;
        }

        Optional<Activity> activityOpt = activityRepository.findByIdAndIsDeletedFalse(contextActivityId);
        if (activityOpt.isEmpty() || activityOpt.get().isDraft()) {
            return null;
        }
        return activityOpt.get();
    }

    private ChatbotMessageResponse respond(
            ChatbotConversation conversation,
            String username,
            String message,
            Activity contextActivity,
            EventArticle contextArticle,
            ChatbotMessageRequest request) {
        String normalizedMessage = normalize(message);
        List<Activity> lastCandidates = loadLastCandidates(conversation);
        Integer optionIndex = parseOptionIndex(message);

        if (optionIndex != null && !lastCandidates.isEmpty()) {
            Activity chosen = chooseByIndex(lastCandidates, optionIndex);
            if (chosen != null) {
                clearLastCandidates(conversation);
                updateConversationContextActivity(conversation, chosen);
                return answerWithResolvedActivity(username, chosen, ChatbotIntent.SUMMARY, normalizedMessage);
            }
        }

        Optional<ChatbotNluResult> nluOpt = chatbotNluService.analyze(
                message,
                request.getPageContext(),
                contextActivity != null,
                lastCandidates);

        ChatbotIntent intent = nluOpt.map(ChatbotNluResult::intent).orElse(ChatbotIntent.UNKNOWN);
        if (intent == ChatbotIntent.CHOOSE_OPTION && nluOpt.isPresent()) {
            Integer idx = nluOpt.get().optionIndex();
            if (idx != null && !lastCandidates.isEmpty()) {
                Activity chosen = chooseByIndex(lastCandidates, idx);
                if (chosen != null) {
                    clearLastCandidates(conversation);
                    updateConversationContextActivity(conversation, chosen);
                    return answerWithResolvedActivity(username, chosen, ChatbotIntent.SUMMARY, normalizedMessage);
                }
            }
            intent = ChatbotIntent.UNKNOWN;
        }

        if (contextActivity != null) {
            if (intent == ChatbotIntent.TIME || isAskingTime(normalizedMessage)) {
                clearLastCandidates(conversation);
                return answerWithResolvedActivity(username, contextActivity, ChatbotIntent.TIME, normalizedMessage);
            }
            if (intent == ChatbotIntent.LOCATION || isAskingLocation(normalizedMessage)) {
                clearLastCandidates(conversation);
                return answerWithResolvedActivity(username, contextActivity, ChatbotIntent.LOCATION, normalizedMessage);
            }
            if (isAskingSlot(normalizedMessage)) {
                clearLastCandidates(conversation);
                return answerWithResolvedActivity(username, contextActivity, ChatbotIntent.REGISTRATION,
                        normalizedMessage);
            }
        }

        if (intent == ChatbotIntent.LIST_OPEN_REGISTRATION) {
            clearLastCandidates(conversation);
            return answerOpenRegistration();
        }
        if (intent == ChatbotIntent.LIST_UPCOMING) {
            clearLastCandidates(conversation);
            return answerUpcoming();
        }
        if (intent == ChatbotIntent.LIST_ONGOING) {
            clearLastCandidates(conversation);
            return answerOngoing();
        }
        if (intent == ChatbotIntent.LIST_PAST) {
            clearLastCandidates(conversation);
            return answerPast();
        }
        if (intent == ChatbotIntent.LIST_BY_SCORETYPE) {
            clearLastCandidates(conversation);
            ScoreType scoreType = parseScoreType(nluOpt.map(ChatbotNluResult::scoreType).orElse(null));
            if (scoreType == null) {
                return new ChatbotMessageResponse(null,
                        "Bạn muốn lọc theo loại điểm nào? (ví dụ: PARTICIPATION, SUBMISSION...)", null, false,
                        new ArrayList<>());
            }
            return answerByScoreType(scoreType);
        }
        if (intent == ChatbotIntent.ACTIVITY_FOR_ARTICLE) {
            clearLastCandidates(conversation);
            return answerActivityForArticle(contextArticle);
        }
        if (intent == ChatbotIntent.SUMMARIZE_ARTICLE) {
            clearLastCandidates(conversation);
            return answerSummarizeArticle(contextArticle);
        }

        if (isAskingOpenRegistration(normalizedMessage)) {
            clearLastCandidates(conversation);
            return answerOpenRegistration();
        }
        if (isAskingUpcoming(normalizedMessage)) {
            clearLastCandidates(conversation);
            return answerUpcoming();
        }
        if (isAskingOngoing(normalizedMessage)) {
            clearLastCandidates(conversation);
            return answerOngoing();
        }
        if (isAskingPast(normalizedMessage)) {
            clearLastCandidates(conversation);
            return answerPast();
        }
        ScoreType scoreTypeFromText = parseScoreTypeFromText(normalizedMessage);
        if (scoreTypeFromText != null && isAskingListByScoreType(normalizedMessage)) {
            clearLastCandidates(conversation);
            return answerByScoreType(scoreTypeFromText);
        }
        if (isAskingActivityForArticle(normalizedMessage)) {
            clearLastCandidates(conversation);
            return answerActivityForArticle(contextArticle);
        }
        if (isAskingSummarizeArticle(normalizedMessage)) {
            clearLastCandidates(conversation);
            return answerSummarizeArticle(contextArticle);
        }

        if (isSupportQuestion(normalizedMessage)) {
            Optional<String> ragAnswer = ragService.findAnswer(message);
            if (ragAnswer.isPresent()) {
                return new ChatbotMessageResponse(
                        null,
                        ragAnswer.get(),
                        null,
                        false,
                        new ArrayList<>()
                );
            }
        }

        Activity resolved = contextActivity;
        List<Activity> candidates = new ArrayList<>();

        if (resolved == null) {
            String activityQuery = nluOpt.map(ChatbotNluResult::activityQuery).orElse(null);
            candidates = activityQuery != null && !activityQuery.isBlank() ? searchActivities(activityQuery)
                    : searchActivities(message);
            if (candidates.isEmpty()) {
                return new ChatbotMessageResponse(null,
                        "Mình chưa tìm thấy sự kiện bạn nhắc tới. Bạn cho mình tên sự kiện (hoặc 1-2 từ khóa) rõ hơn nhé.",
                        null,
                        false,
                        new ArrayList<>());
            }

            ResolutionResult rr = chooseBestCandidate(message, candidates);
            if (rr.needsClarification) {
                List<ChatbotActivityOptionResponse> options = rr.options.stream()
                        .map(a -> new ChatbotActivityOptionResponse(a.getId(), a.getName(), a.getStartDate(),
                                a.getLocation()))
                        .toList();
                ChatbotMessageResponse resp = new ChatbotMessageResponse();
                resp.setAnswer("Bạn muốn hỏi về sự kiện nào trong các lựa chọn sau?");
                resp.setNeedsClarification(true);
                resp.setActivityOptions(options);
                storeLastCandidates(conversation, rr.options);
                return resp;
            }
            resolved = rr.resolved;
        }

        if (resolved == null) {
            return new ChatbotMessageResponse(null,
                    "Bạn muốn hỏi về sự kiện nào? Bạn có thể nhập tên sự kiện hoặc mở trang chi tiết sự kiện để hỏi.",
                    null,
                    false,
                    new ArrayList<>());
        }

        clearLastCandidates(conversation);
        updateConversationContextActivity(conversation, resolved);
        if (intent == ChatbotIntent.ARTICLE_FOR_ACTIVITY || isAskingArticleForActivity(normalizedMessage)) {
            return answerArticleForActivity(resolved);
        }
        return answerWithResolvedActivity(username, resolved, intent, normalizedMessage);
    }

    private void updateConversationContextActivity(ChatbotConversation conversation, Activity activity) {
        if (conversation == null || activity == null) {
            return;
        }
        if (conversation.getContextActivity() != null
                && conversation.getContextActivity().getId() != null
                && conversation.getContextActivity().getId().equals(activity.getId())) {
            return;
        }
        conversation.setContextActivity(activity);
        conversationRepository.save(conversation);
    }

    private ChatbotMessageResponse answerWithResolvedActivity(String username, Activity activity, ChatbotIntent intent,
            String normalizedMessage) {
        ChatbotResolvedActivityResponse resolvedActivity = new ChatbotResolvedActivityResponse(activity.getId(),
                activity.getName());
        String answer;

        if (intent == ChatbotIntent.TIME) {
            answer = formatTimeAnswer(activity);
        } else if (intent == ChatbotIntent.LOCATION) {
            answer = formatLocationAnswer(activity);
        } else if (intent == ChatbotIntent.REGISTRATION) {
            answer = formatRegistrationAnswer(username, activity);
        } else if (intent == ChatbotIntent.BENEFITS) {
            answer = formatBenefitsAnswer(activity);
        } else if (intent == ChatbotIntent.REQUIREMENTS) {
            answer = formatRequirementsAnswer(activity);
        } else if (intent == ChatbotIntent.POINTS) {
            answer = formatPointsAnswer(activity);
        } else if (intent == ChatbotIntent.CONTACT) {
            answer = formatContactAnswer(activity);
        } else if (intent == ChatbotIntent.CHECKIN) {
            answer = formatCheckInAnswer(activity);
        } else if (intent == ChatbotIntent.SUMMARY) {
            answer = formatSummaryAnswer(username, activity);
        } else {
            answer = answerForActivity(username, normalizedMessage, activity);
        }

        if (intent == ChatbotIntent.UNKNOWN) {
            answer = answerForActivity(username, normalizedMessage, activity);
        }

        ChatbotMessageResponse resp = new ChatbotMessageResponse();
        resp.setAnswer(answer);
        resp.setResolvedActivity(resolvedActivity);
        resp.setNeedsClarification(false);
        resp.setActivityOptions(new ArrayList<>());
        return resp;
    }

    private List<Activity> loadLastCandidates(ChatbotConversation conversation) {
        String raw = conversation.getLastCandidateActivityIds();
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        List<Long> ids = parseIds(raw);
        if (ids.isEmpty()) {
            return List.of();
        }
        List<Activity> activities = new ArrayList<>();
        for (Activity a : activityRepository.findAllById(ids)) {
            if (!a.isDeleted() && !a.isDraft()) {
                activities.add(a);
            }
        }
        activities.sort(Comparator.comparingInt(a -> ids.indexOf(a.getId())));
        return activities;
    }

    private void storeLastCandidates(ChatbotConversation conversation, List<Activity> options) {
        String ids = options.stream().limit(10).map(a -> String.valueOf(a.getId()))
                .collect(java.util.stream.Collectors.joining(","));
        conversation.setLastCandidateActivityIds(ids);
        conversationRepository.save(conversation);
    }

    private void clearLastCandidates(ChatbotConversation conversation) {
        if (conversation.getLastCandidateActivityIds() != null
                && !conversation.getLastCandidateActivityIds().isBlank()) {
            conversation.setLastCandidateActivityIds(null);
            conversationRepository.save(conversation);
        }
    }

    private static Integer parseOptionIndex(String message) {
        if (message == null) {
            return null;
        }
        String trimmed = message.trim().toLowerCase(Locale.ROOT);
        if (trimmed.matches("\\d{1,2}")) {
            return Integer.parseInt(trimmed);
        }
        Matcher m = OPTION_INDEX_PATTERN.matcher(trimmed);
        if (m.find()) {
            return Integer.parseInt(m.group(1));
        }
        return null;
    }

    private static Activity chooseByIndex(List<Activity> options, int index) {
        if (index < 1 || index > options.size()) {
            return null;
        }
        return options.get(index - 1);
    }

    private static List<Long> parseIds(String raw) {
        String[] parts = raw.split(",");
        List<Long> ids = new ArrayList<>();
        for (String p : parts) {
            String t = p.trim();
            if (t.isBlank()) {
                continue;
            }
            try {
                ids.add(Long.parseLong(t));
            } catch (Exception ignored) {
            }
        }
        return ids;
    }

    private ChatbotMessageResponse answerUpcoming() {
        Page<Activity> page = activityRepository.findUpcomingPublished(LocalDateTime.now(),
                PageRequest.of(0, 5, Sort.by("startDate").ascending()));
        if (page.isEmpty()) {
            return new ChatbotMessageResponse(null, "Hiện chưa có sự kiện sắp diễn ra.", null, false,
                    new ArrayList<>());
        }
        List<ChatbotActivityOptionResponse> options = page.getContent()
                .stream()
                .map(a -> new ChatbotActivityOptionResponse(
                        a.getId(),
                        a.getName(),
                        a.getStartDate(),
                        a.getLocation()
                ))
                .toList();

        return new ChatbotMessageResponse(
                null,
                formatActivityList("Các sự kiện sắp diễn ra:", page.getContent()),
                null,
                false,
                options
        );}

    private ChatbotMessageResponse answerOpenRegistration() {
        Page<Activity> page = activityRepository.findOpenRegistrationPublished(LocalDateTime.now(),
                PageRequest.of(0, 5, Sort.by("registrationDeadline").ascending()));
        if (page.isEmpty()) {
            return new ChatbotMessageResponse(null, "Hiện chưa có sự kiện nào đang mở đăng ký.", null, false,
                    new ArrayList<>());
        }

        List<ChatbotActivityOptionResponse> options = page.getContent()
                .stream()
                .map(a -> new ChatbotActivityOptionResponse(
                        a.getId(),
                        a.getName(),
                        a.getStartDate(),
                        a.getLocation()
                ))
                .toList();

        return new ChatbotMessageResponse(
                null,
                formatActivityList("Các sự kiện đang mở đăng ký:", page.getContent()),
                null,
                false,
                options
        );}


    private ChatbotMessageResponse answerOngoing() {
        Page<Activity> page = activityRepository.findOngoingPublished(LocalDateTime.now(),
                PageRequest.of(0, 5, Sort.by("startDate").ascending()));
        if (page.isEmpty()) {
            return new ChatbotMessageResponse(null, "Hiện chưa có sự kiện nào đang diễn ra.", null, false,
                    new ArrayList<>());
        }
        List<ChatbotActivityOptionResponse> options = page.getContent()
                .stream()
                .map(a -> new ChatbotActivityOptionResponse(
                        a.getId(),
                        a.getName(),
                        a.getStartDate(),
                        a.getLocation()
                ))
                .toList();

        return new ChatbotMessageResponse(
                null,
                formatActivityList("Các sự kiện đang xảy ra:", page.getContent()),
                null,
                false,
                options
        );}

    private ChatbotMessageResponse answerPast() {
        Page<Activity> page = activityRepository.findPastPublished(LocalDateTime.now(),
                PageRequest.of(0, 5, Sort.by("startDate").descending()));
        if (page.isEmpty()) {
            return new ChatbotMessageResponse(null, "Hiện chưa có dữ liệu sự kiện đã diễn ra.", null, false,
                    new ArrayList<>());
        }
        List<ChatbotActivityOptionResponse> options = page.getContent()
                .stream()
                .map(a -> new ChatbotActivityOptionResponse(
                        a.getId(),
                        a.getName(),
                        a.getStartDate(),
                        a.getLocation()
                ))
                .toList();

        return new ChatbotMessageResponse(
                null,
                formatActivityList("Các sự kiện đã diễn ra:", page.getContent()),
                null,
                false,
                options
        );}

    private ChatbotMessageResponse answerByScoreType(ScoreType scoreType) {
        Page<Activity> page = activityRepository.findPublishedByScoreType(scoreType,
                PageRequest.of(0, 10, Sort.by("startDate").descending()));
        if (page.isEmpty()) {
            return new ChatbotMessageResponse(null, "Hiện chưa có sự kiện thuộc loại điểm: " + scoreType.name(), null,
                    false, new ArrayList<>());
        }
        List<ChatbotActivityOptionResponse> options = page.getContent()
                .stream()
                .map(a -> new ChatbotActivityOptionResponse(
                        a.getId(),
                        a.getName(),
                        a.getStartDate(),
                        a.getLocation()
                ))
                .toList();

        return new ChatbotMessageResponse(
                null,
                formatActivityList("Các sự kiện theo loại điểm:", page.getContent()),
                null,
                false,
                options
        );}

    private ChatbotMessageResponse answerArticleForActivity(Activity activity) {
        List<EventArticle> articles = eventArticleRepository.findByActivityIdAndIsPublishedTrue(activity.getId());
        if (articles.isEmpty()) {
            ChatbotResolvedActivityResponse resolvedActivity = new ChatbotResolvedActivityResponse(activity.getId(),
                    activity.getName());
            return new ChatbotMessageResponse(null, "Sự kiện này hiện chưa có bài viết được đăng.", resolvedActivity,
                    false, new ArrayList<>());
        }
        EventArticle article = articles.stream()
                .filter(EventArticle::isPrimary)
                .findFirst()
                .orElse(articles.get(0));
        ChatbotResolvedActivityResponse resolvedActivity = new ChatbotResolvedActivityResponse(activity.getId(),
                activity.getName());
        String answer = "Sự kiện này có bài viết: " + article.getTitle() + "\nSlug: " + article.getSlug()
                + "\nBạn có thể xem qua API: /api/articles/" + article.getSlug();
        return new ChatbotMessageResponse(null, answer, resolvedActivity, false, new ArrayList<>());
    }

    private ChatbotMessageResponse answerActivityForArticle(EventArticle article) {
        if (article == null) {
            return new ChatbotMessageResponse(null,
                    "Bạn đang hỏi bài viết nào? Hãy mở trang bài viết hoặc gửi slug bài viết để mình kiểm tra.", null,
                    false, new ArrayList<>());
        }
        Activity a = article.getActivity();
        if (a == null) {
            return new ChatbotMessageResponse(null, "Bài viết này chưa được liên kết với sự kiện.", null, false,
                    new ArrayList<>());
        }
        ChatbotResolvedActivityResponse resolvedActivity = new ChatbotResolvedActivityResponse(a.getId(), a.getName());
        String answer = "Bài viết \"" + article.getTitle() + "\" thuộc sự kiện: " + a.getName();
        return new ChatbotMessageResponse(null, answer, resolvedActivity, false, new ArrayList<>());
    }

    private ChatbotMessageResponse answerSummarizeArticle(EventArticle article) {
        if (article == null) {
            return new ChatbotMessageResponse(null,
                    "Bạn đang hỏi bài viết nào? Hãy mở trang bài viết hoặc gửi slug bài viết để mình tóm tắt.", null,
                    false, new ArrayList<>());
        }
        if (!geminiApiClient.isEnabled()) {
            return new ChatbotMessageResponse(null, "Gemini chưa được bật nên mình chưa thể tóm tắt tự động.", null,
                    false, new ArrayList<>());
        }
        String content = article.getContent() == null ? "" : article.getContent();
        content = content.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim();
        if (content.length() > 6000) {
            content = content.substring(0, 6000);
        }
        String prompt = """
                Bạn là trợ lý tóm tắt bài viết sự kiện cho sinh viên.
                Hãy tóm tắt ngắn gọn bằng tiếng Việt, 5-8 gạch đầu dòng, tập trung vào: nội dung chính, thời gian/địa điểm (nếu có), cách đăng ký/điều kiện (nếu có).

                Tiêu đề: %s

                Nội dung:
                %s
                """
                .formatted(article.getTitle(), content);

        Optional<String> summaryOpt = geminiApiClient.generateText(prompt);
        if (summaryOpt.isEmpty() || summaryOpt.get().isBlank()) {
            return new ChatbotMessageResponse(null, "Mình chưa tóm tắt được bài viết lúc này. Bạn thử lại sau.", null,
                    false, new ArrayList<>());
        }
        String summary = summaryOpt.get().trim();
        if (summary.startsWith("__GEMINI_BLOCKED__:")) {
            String reason = summary.substring("__GEMINI_BLOCKED__:".length()).trim();
            return new ChatbotMessageResponse(null,
                    "Gemini từ chối xử lý nội dung bài viết (blockReason: " + reason
                            + "). Bạn thử tóm tắt lại sau hoặc rút ngắn nội dung.",
                    null, false, new ArrayList<>());
        }
        if (summary.startsWith("__GEMINI_EMPTY__")) {
            return new ChatbotMessageResponse(null,
                    "Gemini không trả về nội dung tóm tắt. Bạn kiểm tra lại GEMINI_API_KEY/quota, hoặc thử lại sau.",
                    null, false, new ArrayList<>());
        }
        if (summary.startsWith("__GEMINI_NETWORK__")) {
            return new ChatbotMessageResponse(null,
                    "Backend không kết nối được đến Gemini API. Bạn kiểm tra mạng/firewall/DNS của máy chạy backend.",
                    null, false, new ArrayList<>());
        }
        if (summary.startsWith("__GEMINI_HTTP__:")) {
            String detail = summary.substring("__GEMINI_HTTP__:".length()).trim();
            return new ChatbotMessageResponse(null,
                    "Gemini API trả lỗi HTTP: " + detail + ". Bạn kiểm tra lại GEMINI_API_KEY/quota/model.",
                    null, false, new ArrayList<>());
        }
        return new ChatbotMessageResponse(null, summary, null, false, new ArrayList<>());
    }

    private String formatActivityList(String title, List<Activity> activities) {
        StringBuilder sb = new StringBuilder();
        sb.append(title);
        for (int i = 0; i < activities.size(); i++) {
            Activity a = activities.get(i);
            sb.append("\n").append(i + 1).append(") ").append(a.getName());
            if (a.getStartDate() != null) {
                sb.append(" - ").append(DATE_TIME_FMT.format(a.getStartDate()));
            }
            if (a.getLocation() != null && !a.getLocation().isBlank()) {
                sb.append(" - ").append(a.getLocation());
            }
        }
        sb.append(
                "\nBạn có thể bấm vào 1 sự kiện để vào trang chi tiết rồi hỏi tiếp, hoặc nhắn tên sự kiện bạn muốn hỏi.");
        return sb.toString();
    }

    private String answerForActivity(String username, String normalized, Activity a) {
        if (isAskingLocation(normalized)) {
            return formatLocationAnswer(a);
        }
        if (isAskingContact(normalized)) {
            return formatContactAnswer(a);
        }
        if (isAskingCheckIn(normalized)) {
            return formatCheckInAnswer(a);
        }
        if (isAskingRegistration(normalized)) {
            return formatRegistrationAnswer(username, a);
        }
        if (isAskingTime(normalized)) {
            return formatTimeAnswer(a);
        }
        if (isAskingBenefits(normalized)) {
            return formatBenefitsAnswer(a);
        }
        if (isAskingRequirements(normalized)) {
            return formatRequirementsAnswer(a);
        }
        if (isAskingPoints(normalized)) {
            return formatPointsAnswer(a);
        }
        return formatSummaryAnswer(username, a);
    }

    private String formatTimeAnswer(Activity a) {
        String start = a.getStartDate() == null ? "Chưa cập nhật" : DATE_TIME_FMT.format(a.getStartDate());
        String end = a.getEndDate() == null ? "Chưa cập nhật" : DATE_TIME_FMT.format(a.getEndDate());
        return "Thời gian sự kiện:\n- Bắt đầu: " + start + "\n- Kết thúc: " + end;
    }

    private String formatLocationAnswer(Activity a) {
        if (a.getLocation() == null || a.getLocation().isBlank()) {
            return "Địa điểm hiện chưa được cập nhật.";
        }
        return "Địa điểm: " + a.getLocation();
    }

    private String formatContactAnswer(Activity a) {
        if (a.getContactInfo() == null || a.getContactInfo().isBlank()) {
            return "Thông tin liên hệ hiện chưa được cập nhật.";
        }
        return "Thông tin liên hệ: " + a.getContactInfo();
    }

    private String formatCheckInAnswer(Activity a) {
        if (a.getCheckInCode() == null || a.getCheckInCode().isBlank()) {
            return "Sự kiện này chưa có mã check-in được công bố trên hệ thống.";
        }
        return "Sự kiện có mã check-in: " + a.getCheckInCode();
    }

    private String formatBenefitsAnswer(Activity a) {
        if (a.getBenefits() == null || a.getBenefits().isBlank()) {
            return "Quyền lợi khi tham gia hiện chưa được cập nhật.";
        }
        return "Quyền lợi khi tham gia:\n" + a.getBenefits();
    }

    private String formatRequirementsAnswer(Activity a) {
        if (a.getRequirements() == null || a.getRequirements().isBlank()) {
            return "Yêu cầu tham gia hiện chưa được cập nhật.";
        }
        return "Yêu cầu tham gia:\n" + a.getRequirements();
    }

    private String formatPointsAnswer(Activity a) {
        return "Điểm được tính theo quy tắc điểm danh mới. Chi tiết vui lòng xem trong sự kiện.";
    }

    private String formatRegistrationAnswer(String username, Activity a) {
        StringBuilder sb = new StringBuilder();

        if (a.getRegistrationStartDate() == null || a.getRegistrationDeadline() == null) {
            sb.append("Thời gian đăng ký: Chưa cập nhật");
        } else {
            sb.append("Thời gian đăng ký:\n- Mở: ").append(DATE_TIME_FMT.format(a.getRegistrationStartDate()))
                    .append("\n- Hạn: ").append(DATE_TIME_FMT.format(a.getRegistrationDeadline()));
        }

        sb.append("\nCần duyệt đăng ký: ").append(a.isRequiresApproval() ? "Có" : "Không");

        if (a.getTicketQuantity() != null) {
            Long approved = activityRegistrationRepository.countByActivityIdAndStatus(a.getId(),
                    RegistrationStatus.APPROVED);
            long remaining = Math.max(0, a.getTicketQuantity().longValue() - (approved == null ? 0 : approved));
            sb.append("\nSố lượng slot: ").append(a.getTicketQuantity())
                    .append(" (đã duyệt: ").append(approved == null ? 0 : approved)
                    .append(", còn lại: ").append(remaining).append(")");
        }

        Long studentId = null;
        try {
            studentId = studentService.getStudentIdByUsername(username);
        } catch (Exception ignored) {
        }
        if (studentId != null) {
            activityRegistrationRepository.findByActivityIdAndStudentId(a.getId(), studentId).ifPresent(ar -> {
                sb.append("\nTrạng thái đăng ký của bạn: ").append(ar.getStatus().name());
            });
        }

        return sb.toString();
    }

    private String formatSummaryAnswer(String username, Activity a) {
        StringBuilder sb = new StringBuilder();
        sb.append(a.getName());
        if (a.getStartDate() != null) {
            sb.append("\nThời gian: ").append(DATE_TIME_FMT.format(a.getStartDate()));
            if (a.getEndDate() != null) {
                sb.append(" - ").append(DATE_TIME_FMT.format(a.getEndDate()));
            }
        }
        if (a.getLocation() != null && !a.getLocation().isBlank()) {
            sb.append("\nĐịa điểm: ").append(a.getLocation());
        }
        if (a.getRegistrationStartDate() != null && a.getRegistrationDeadline() != null) {
            sb.append("\nĐăng ký: ").append(DATE_TIME_FMT.format(a.getRegistrationStartDate()))
                    .append(" đến ").append(DATE_TIME_FMT.format(a.getRegistrationDeadline()));
        }
        sb.append("\nCần duyệt: ").append(a.isRequiresApproval() ? "Có" : "Không");
        sb.append("\nCần nộp minh chứng: ").append(a.isRequiresSubmission() ? "Có" : "Không");

        if (a.getTicketQuantity() != null) {
            Long approved = activityRegistrationRepository.countByActivityIdAndStatus(a.getId(),
                    RegistrationStatus.APPROVED);
            long remaining = Math.max(0, a.getTicketQuantity().longValue() - (approved == null ? 0 : approved));
            sb.append("\nSlot: ").append(a.getTicketQuantity()).append(" (còn ").append(remaining).append(")");
        }

        if (a.getShareLink() != null && !a.getShareLink().isBlank()) {
            sb.append("\nLink: ").append(a.getShareLink());
        }

        sb.append("\nBạn có thể hỏi tiếp: thời gian, địa điểm, đăng ký, yêu cầu, quyền lợi, điểm, liên hệ.");
        return sb.toString();
    }

    private static String formatDecimal(BigDecimal value) {
        BigDecimal stripped = value.stripTrailingZeros();
        return stripped.scale() < 0 ? stripped.setScale(0).toPlainString() : stripped.toPlainString();
    }

    private static boolean isAskingTime(String s) {
        return containsAny(s, "khi nao", "khi nào", "thoi gian", "thời gian", "bat dau", "bắt đầu", "ket thuc",
                "kết thúc", "dien ra");
    }

    private static boolean isAskingLocation(String s) {
        return containsAny(s, "o dau", "ở đâu", "dia diem", "địa điểm", "phong", "phòng", "online");
    }

    private static boolean isAskingRegistration(String s) {
        return containsAny(s, "dang ky", "đăng ký", "mo dang ky", "mở đăng ký", "han", "hạn", "deadline", "full",
                "con cho", "còn chỗ", "slot", "ve", "vé");
    }

    private static boolean isAskingSlot(String s) {
        return containsAny(s,
                "con slot khong", "còn slot không",
                "con cho khong", "còn chỗ không",
                "het slot chua", "hết slot chưa",
                "slot con lai", "slot còn lại",
                "con ve khong", "còn vé không");
    }

    private static boolean isAskingOpenRegistration(String s) {
        return containsAny(s, "dang mo dang ky", "đang mở đăng ký", "mo dang ky", "mở đăng ký");
    }

    private static boolean isAskingUpcoming(String s) {
        return containsAny(s, "sap dien ra", "sắp diễn ra", "su kien sap toi", "sự kiện sắp tới");
    }

    private static boolean isAskingOngoing(String s) {
        return containsAny(s, "dang dien ra", "đang diễn ra", "hien dang dien ra", "hiện đang diễn ra");
    }

    private static boolean isAskingPast(String s) {
        return containsAny(s, "da dien ra", "đã diễn ra", "da to chuc", "đã tổ chức", "su kien cu", "sự kiện cũ");
    }

    private static boolean isAskingListByScoreType(String s) {
        return containsAny(s, "theo loai diem", "theo loại điểm", "loai diem", "loại điểm", "diem ren luyen",
                "điểm rèn luyện", "cong tac xa hoi", "công tác xã hội", "chuyen de", "chuyên đề");
    }

    private static boolean isAskingArticleForActivity(String s) {
        return containsAny(s, "co bai viet", "có bài viết", "bai viet nao", "bài viết nào", "bai viet", "bài viết")
                && containsAny(s, "su kien", "sự kiện", "nay", "này", "khong", "không", "co", "có");
    }

    private static boolean isAskingActivityForArticle(String s) {
        return containsAny(s, "bai viet nay", "bài viết này", "bai viet do", "bài viết đó")
                && containsAny(s, "su kien nao", "sự kiện nào", "cua su kien", "của sự kiện");
    }

    private static boolean isAskingSummarizeArticle(String s) {
        return containsAny(s, "tom tat", "tóm tắt", "summary") && containsAny(s, "bai viet", "bài viết");
    }

    private static ScoreType parseScoreTypeFromText(String normalizedMessage) {
        if (normalizedMessage == null) {
            return null;
        }
        if (normalizedMessage.contains("ren luyen") || normalizedMessage.contains("rèn luyện")
                || normalizedMessage.contains("ren-luyen")) {
            return ScoreType.REN_LUYEN;
        }
        if (normalizedMessage.contains("cong tac xa hoi") || normalizedMessage.contains("công tác xã hội")
                || normalizedMessage.contains("xa hoi")) {
            return ScoreType.CONG_TAC_XA_HOI;
        }
        if (normalizedMessage.contains("chuyen de") || normalizedMessage.contains("chuyên đề")) {
            return ScoreType.CHUYEN_DE;
        }
        return null;
    }

    private static ScoreType parseScoreType(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String s = raw.trim().toUpperCase(Locale.ROOT);
        try {
            return ScoreType.valueOf(s);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static boolean isAskingRequirements(String s) {
        return containsAny(s, "yeu cau", "yêu cầu", "dieu kien", "điều kiện", "can chuan bi", "cần chuẩn bị",
                "chuan bi", "chuẩn bị");
    }

    private static boolean isAskingBenefits(String s) {
        return containsAny(s, "quyen loi", "quyền lợi", "duoc gi", "được gì", "loi ich", "lợi ích", "qua", "quà");
    }

    private static boolean isAskingPoints(String s) {
        return containsAny(s, "diem", "điểm", "toi da", "tối đa", "tru diem", "trừ điểm", "phat", "phạt");
    }

    private static boolean isAskingContact(String s) {
        return containsAny(s, "lien he", "liên hệ", "ho tro", "hỗ trợ", "email", "sdt", "số điện thoại",
                "so dien thoai");
    }

    private static boolean isAskingCheckIn(String s) {
        return containsAny(s, "checkin", "check-in", "check in", "qr");
    }
    private static boolean isSupportQuestion(String s) {
        return containsAny(s,
                "da tham gia", "đã tham gia",
                "tham gia roi", "tham gia rồi",
                "tham gia xong",
                "chua duoc xac nhan", "chưa được xác nhận",
                "chua xac nhan", "chưa xác nhận",
                "he thong chua cap nhat", "hệ thống chưa cập nhật",
                "chua cap nhat tham gia", "chưa cập nhật tham gia",
                "khong check in", "không check in",
                "khong check-in", "không check-in",
                "chua cong diem", "chưa cộng điểm",
                "chua cap nhat diem", "chưa cập nhật điểm",
                "minh chung", "minh chứng",
                "phai lam sao", "phải làm sao",
                "lien he", "liên hệ",
                "thong tin lien he", "thông tin liên hệ",
                "khoa dien", "khoa điện",
                "khoa cntt",
                "khoa co khi", "khoa cơ khí",
                "ban to chuc", "ban tổ chức",
                "ho tro", "hỗ trợ",
                "email khoa",
                "so dien thoai khoa", "số điện thoại khoa",
                "lien he ban to chuc", "liên hệ ban tổ chức",
                "lien he khoa", "liên hệ khoa",
                "khoa xay dung", "khoa xây dựng",
                "kenh ho tro", "kênh hỗ trợ");
    }
    private static boolean containsAny(String s, String... keywords) {
        for (String k : keywords) {
            if (s.contains(k)) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(String input) {
        return input == null ? "" : input.toLowerCase(Locale.ROOT).trim();
    }

    private List<Activity> searchActivities(String message) {
        List<String> tokens = extractTokens(message);
        if (tokens.isEmpty()) {
            return List.of();
        }

        Specification<Activity> spec = (root, query, cb) -> {
            var base = cb.and(
                    cb.isFalse(root.get("isDeleted")),
                    cb.isFalse(root.get("isDraft")));

            var orPredicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            for (String t : tokens) {
                String like = "%" + t + "%";
                orPredicates.add(cb.like(cb.lower(root.get("name")), like));
                orPredicates.add(cb.like(cb.lower(root.get("description")), like));
                orPredicates.add(cb.like(cb.lower(root.get("benefits")), like));
                orPredicates.add(cb.like(cb.lower(root.get("requirements")), like));
                orPredicates.add(cb.like(cb.lower(root.get("location")), like));
            }
            var or = cb.or(orPredicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
            return cb.and(base, or);
        };

        Page<Activity> page = activityRepository.findAll(spec,
                PageRequest.of(0, 20, Sort.by("startDate").descending()));
        return page.getContent();
    }

    private ResolutionResult chooseBestCandidate(String message, List<Activity> candidates) {
        List<String> tokens = extractTokens(message);
        List<ScoredActivity> scored = candidates.stream()
                .map(a -> new ScoredActivity(a, score(tokens, a)))
                .sorted(Comparator.comparingInt(ScoredActivity::score).reversed())
                .toList();

        if (scored.isEmpty()) {
            return new ResolutionResult(null, false, List.of());
        }
        if (scored.size() == 1) {
            return new ResolutionResult(scored.getFirst().activity, false, List.of());
        }

        int top = scored.getFirst().score;
        int second = scored.get(1).score;
        if (top >= second + 2 && top >= 3) {
            return new ResolutionResult(scored.getFirst().activity, false, List.of());
        }

        Set<Activity> options = new LinkedHashSet<>();
        for (int i = 0; i < Math.min(5, scored.size()); i++) {
            options.add(scored.get(i).activity);
        }
        return new ResolutionResult(null, true, new ArrayList<>(options));
    }

    private int score(List<String> tokens, Activity a) {
        int score = 0;
        String name = safeLower(a.getName());
        String desc = safeLower(a.getDescription());
        for (String t : tokens) {
            if (name.contains(t)) {
                score += 3;
            }
            if (desc.contains(t)) {
                score += 1;
            }
        }
        return score;
    }

    private static String safeLower(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT);
    }

    private static List<String> extractTokens(String message) {
        String normalized = normalize(message);
        Matcher m = TOKEN_PATTERN.matcher(normalized);
        List<String> tokens = new ArrayList<>();
        while (m.find()) {
            String t = m.group();
            if (t.length() < 3) {
                continue;
            }
            if (STOP_WORDS.contains(t)) {
                continue;
            }
            tokens.add(t);
            if (tokens.size() >= 8) {
                break;
            }
        }
        return tokens;
    }

    private record ScoredActivity(Activity activity, int score) {
    }

    private static class ResolutionResult {
        private final Activity resolved;
        private final boolean needsClarification;
        private final List<Activity> options;

        private ResolutionResult(Activity resolved, boolean needsClarification, List<Activity> options) {
            this.resolved = resolved;
            this.needsClarification = needsClarification;
            this.options = options;
        }
    }
}
