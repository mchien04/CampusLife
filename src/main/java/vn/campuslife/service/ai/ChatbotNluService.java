package vn.campuslife.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.campuslife.entity.Activity;
import vn.campuslife.enumeration.ChatbotIntent;
import vn.campuslife.enumeration.ChatbotPageContext;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ChatbotNluService {

    private final GeminiApiClient geminiApiClient;
    private final ObjectMapper objectMapper;

    public Optional<ChatbotNluResult> analyze(
            String message,
            ChatbotPageContext pageContext,
            boolean hasContextActivity,
            List<Activity> lastCandidates
    ) {
        if (!geminiApiClient.isEnabled()) {
            return Optional.empty();
        }

        String prompt = buildPrompt(message, pageContext, hasContextActivity, lastCandidates);
        Optional<String> jsonTextOpt = geminiApiClient.generateJson(prompt);
        if (jsonTextOpt.isEmpty()) {
            return Optional.empty();
        }

        try {
            String jsonText = stripCodeFence(jsonTextOpt.get());
            JsonNode node = objectMapper.readTree(jsonText);

            ChatbotIntent intent = parseIntent(node.path("intent").asText(null));
            Integer optionIndex = node.hasNonNull("optionIndex") ? node.get("optionIndex").asInt() : null;
            String activityQuery = node.hasNonNull("activityQuery") ? node.get("activityQuery").asText() : null;

            return Optional.of(new ChatbotNluResult(intent, optionIndex, activityQuery));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private String buildPrompt(
            String message,
            ChatbotPageContext pageContext,
            boolean hasContextActivity,
            List<Activity> lastCandidates
    ) {
        String candidatesText = buildCandidatesText(lastCandidates);

        return """
                Bạn là bộ phân tích ý định cho chatbot sự kiện trong trường đại học.
                Nhiệm vụ: đọc câu hỏi của sinh viên và xuất ra JSON hợp lệ (không markdown, không giải thích).

                Các intent hợp lệ:
                TIME, LOCATION, REGISTRATION, BENEFITS, REQUIREMENTS, POINTS, CONTACT, CHECKIN, SUMMARY,
                LIST_UPCOMING, LIST_OPEN_REGISTRATION, CHOOSE_OPTION, UNKNOWN

                Quy tắc:
                - Nếu người dùng chọn 1 lựa chọn trong danh sách trước đó (ví dụ "cái số 2", "chọn 3"), intent=CHOOSE_OPTION và optionIndex là số.
                - Nếu đang ở trang chi tiết sự kiện (pageContext=ACTIVITY_DETAIL) hoặc hasContextActivity=true thì không cần activityQuery.
                - Nếu đang ở GLOBAL và người dùng nhắc tên sự kiện, đặt activityQuery là cụm từ quan trọng để tìm event.
                - Nếu không rõ thì intent=UNKNOWN.

                Ngữ cảnh:
                pageContext=%s
                hasContextActivity=%s
                lastCandidates=%s

                Câu hỏi:
                %s

                JSON output schema:
                {"intent":"...","optionIndex":1,"activityQuery":"..."}
                """.formatted(
                pageContext == null ? "GLOBAL" : pageContext.name(),
                Boolean.toString(hasContextActivity),
                candidatesText,
                message
        );
    }

    private static String buildCandidatesText(List<Activity> lastCandidates) {
        if (lastCandidates == null || lastCandidates.isEmpty()) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        int n = Math.min(5, lastCandidates.size());
        for (int i = 0; i < n; i++) {
            Activity a = lastCandidates.get(i);
            if (i > 0) {
                sb.append(",");
            }
            sb.append("{\"index\":").append(i + 1)
                    .append(",\"id\":").append(a.getId())
                    .append(",\"name\":\"").append(escape(a.getName())).append("\"}");
        }
        sb.append("]");
        return sb.toString();
    }

    private static String stripCodeFence(String s) {
        String t = s.trim();
        if (t.startsWith("```")) {
            int firstNewline = t.indexOf('\n');
            if (firstNewline >= 0) {
                t = t.substring(firstNewline + 1);
            }
            int lastFence = t.lastIndexOf("```");
            if (lastFence >= 0) {
                t = t.substring(0, lastFence);
            }
        }
        return t.trim();
    }

    private static ChatbotIntent parseIntent(String s) {
        if (s == null || s.isBlank()) {
            return ChatbotIntent.UNKNOWN;
        }
        try {
            return ChatbotIntent.valueOf(s.trim().toUpperCase());
        } catch (Exception ignored) {
            return ChatbotIntent.UNKNOWN;
        }
    }

    private static String escape(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
