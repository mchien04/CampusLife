package vn.campuslife.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import vn.campuslife.entity.*;
import vn.campuslife.enumeration.RegistrationStatus;
import vn.campuslife.model.recommend.PythonActivityItem;
import vn.campuslife.model.recommend.PythonRecommendRequest;
import vn.campuslife.model.recommend.PythonRecommendResponse;
import vn.campuslife.model.recommend.RecommendedActivityResponse;
import vn.campuslife.repository.ActivityRegistrationRepository;
import vn.campuslife.repository.ActivityRepository;
import vn.campuslife.repository.ArticleViewHistoryRepository;
import vn.campuslife.repository.ArticleWishlistRepository;
import vn.campuslife.service.ActivityRecommendationService;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.util.*;
import org.jsoup.Jsoup;
@Service
@RequiredArgsConstructor
public class ActivityRecommendationServiceImpl implements ActivityRecommendationService {
    private final ArticleViewHistoryRepository articleViewHistoryRepository;
    private final ArticleWishlistRepository articleWishlistRepository;
    private final ActivityRegistrationRepository activityRegistrationRepository;
    private final ActivityRepository activityRepository;
    private final RestTemplate restTemplate;
    @Override
    public List<RecommendedActivityResponse> recommendForStudent(Long studentId, int limit) {
        String userProfile = buildUserProfile(studentId);
        System.out.println("===== AFTER buildUserProfile =====");
        System.out.println("userProfile is null? " + (userProfile == null));
        System.out.println("userProfile length = " + (userProfile == null ? 0 : userProfile.length()));
        System.out.println("userProfile = " + userProfile);
        if (userProfile == null || userProfile.isBlank()) {
            return List.of();
        }
        printUserProfile(userProfile);
        List<Activity> activities =
                activityRepository.findOpenActivitiesForRecommendation(LocalDateTime.now());

        List<Activity> candidateActivities = activities.stream()
                .filter(activity ->
                        !activityRegistrationRepository.existsByActivityIdAndStudentId(
                                activity.getId(),
                                studentId
                        )
                )
                .toList();

        if (candidateActivities.isEmpty()) {
            return List.of();
        }

        List<PythonActivityItem> pythonActivities = candidateActivities.stream()
                .map(activity -> {
                    PythonActivityItem item = new PythonActivityItem();
                    item.setId(activity.getId());
                    item.setContent(buildActivityContent(activity));
                    return item;
                })
                .toList();

        PythonRecommendRequest pythonRequest = new PythonRecommendRequest();
        pythonRequest.setUserProfile(userProfile);
        pythonRequest.setActivities(pythonActivities);

        String pythonUrl = "http://localhost:8000/recommend";

        PythonRecommendResponse[] pythonResponses =
                restTemplate.postForObject(
                        pythonUrl,
                        pythonRequest,
                        PythonRecommendResponse[].class
                );

        if (pythonResponses == null || pythonResponses.length == 0) {
            return List.of();
        }

        Map<Long, Activity> activityMap = candidateActivities.stream()
                .collect(Collectors.toMap(Activity::getId, activity -> activity));

        return Arrays.stream(pythonResponses)
                .filter(response -> response.getScore() != null && response.getScore() > 0)
                .sorted(
                        Comparator.comparingDouble(
                                PythonRecommendResponse::getScore
                        ).reversed()
                )
                .limit(limit)
                .map(response -> {
                    Activity activity = activityMap.get(response.getActivityId());

                    if (activity == null) {
                        return null;
                    }

                    RecommendedActivityResponse result = new RecommendedActivityResponse();
                    result.setId(activity.getId());
                    result.setName(activity.getName());
                    result.setStartDate(activity.getStartDate());
                    result.setRegistrationDeadline(activity.getRegistrationDeadline());
                    result.setRegistrationStart(activity.getRegistrationStartDate());
                    result.setLocation(activity.getLocation());
                    return result;
                })
                .filter(Objects::nonNull)
                .toList();
    }
    private String buildUserProfile(Long studentId) {
        StringBuilder profile = new StringBuilder();

        List<RegistrationStatus> statuses = List.of(
                RegistrationStatus.APPROVED,
                RegistrationStatus.ATTENDED
        );

        List<ArticleViewHistory> viewedArticles =
                articleViewHistoryRepository.findTop20ByStudentIdOrderByViewedAtDesc(studentId);

        List<ArticleWishlist> articleWishlists =
                articleWishlistRepository.findByStudentIdOrderByCreatedAtDesc(studentId);

        List<ActivityRegistration> activityRegistrations =
                activityRegistrationRepository.findByStudentIdAndStatusInForRecommendation(studentId, statuses);

        for (ArticleViewHistory history : viewedArticles) {
            EventArticle article = history.getArticle();
            appendArticleContent(profile, article);
        }

        for (ArticleWishlist wishlist : articleWishlists) {
            EventArticle article = wishlist.getArticle();
            appendArticleContent(profile, article);
        }

        for (ActivityRegistration registration : activityRegistrations) {
            Activity activity = registration.getActivity();

            if (activity == null) continue;

            appendIfNotBlank(profile, activity.getName());
            appendIfNotBlank(profile, activity.getDescription());

            if (activity.getType() != null) {
                appendIfNotBlank(profile, activity.getType().name());
            }

            if (activity.getScoreType() != null) {
                appendIfNotBlank(profile, activity.getScoreType().name());
            }
        }

        return preprocessText(profile.toString());
    }
    private void appendArticleContent(StringBuilder profile, EventArticle article) {
        if (article == null) return;

        appendIfNotBlank(profile, article.getTitle());
        appendIfNotBlank(profile, article.getContent());

        if (article.getCategory() != null) {
            appendIfNotBlank(profile, article.getCategory().getName());
        }

        if (article.getTags() != null) {
            article.getTags().forEach(tag ->
                    appendIfNotBlank(profile, tag.getName())
            );
        }
    }

    private void appendIfNotBlank(StringBuilder profile, String text) {
        if (text != null && !text.isBlank()) {
            profile.append(text).append(" ");
        }
    }
    private double calculateKeywordScore(String userProfile, Activity activity) {

        String content =
                (activity.getName() + " " +
                        activity.getDescription())
                        .toLowerCase();
        Set<String> profileWords  = new HashSet<>(Arrays.asList(userProfile.split("\\s+")));
        Set<String> activityWords  = new HashSet<>(Arrays.asList(content.split("\\s+")));
        long matched =
                activityWords.stream()
                        .filter(profileWords::contains)
                        .count();
        return (double) matched / activityWords.size();

    }
    private String buildActivityContent(Activity activity) {
        StringBuilder content = new StringBuilder();

        appendIfNotBlank(content, activity.getName());
        appendIfNotBlank(content, activity.getDescription());

        if (activity.getType() != null) {
            appendIfNotBlank(content, activity.getType().name().replace("_", " "));
        }

        if (activity.getScoreType() != null) {
            appendIfNotBlank(content, activity.getScoreType().name().replace("_", " "));
        }

        appendIfNotBlank(content, activity.getLocation());
        appendIfNotBlank(content, activity.getBenefits());
        appendIfNotBlank(content, activity.getRequirements());

        return preprocessText(content.toString());
    }
    private String preprocessText(String text) {
        if (text == null) {
            return "";
        }

        text = Jsoup.parse(text).text();

        return text
                .toLowerCase()
                .replace("_", " ")
                .replaceAll("[^\\p{L}\\p{N}\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
    private void printUserProfile(String userProfile) {
        System.out.println("\n========== USER PROFILE ==========");
        System.out.println("Length: " + userProfile.length());
        System.out.println(userProfile);
        System.out.println("==================================\n");
    }

}
