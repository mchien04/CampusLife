package vn.campuslife.controller.communication;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.campuslife.model.ChatbotMessageRequest;
import vn.campuslife.model.ChatbotMessageResponse;
import vn.campuslife.model.ChatbotGeminiPingResponse;
import vn.campuslife.model.ChatbotGeminiModelsResponse;
import vn.campuslife.model.ChatbotStatusResponse;
import vn.campuslife.service.ChatbotService;
import vn.campuslife.service.ai.GeminiApiClient;

@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
public class ChatbotController {

    private final ChatbotService chatbotService;
    private final GeminiApiClient geminiApiClient;

    @GetMapping("/status")
    public ResponseEntity<ChatbotStatusResponse> status(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(new ChatbotStatusResponse(geminiApiClient.isEnabled(), geminiApiClient.getModel()));
    }

    @GetMapping("/gemini/ping")
    public ResponseEntity<ChatbotGeminiPingResponse> geminiPing(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).build();
        }

        boolean enabled = geminiApiClient.isEnabled();
        if (!enabled) {
            return ResponseEntity.ok(new ChatbotGeminiPingResponse(false, false, geminiApiClient.getModel(),
                    "Gemini chưa được bật (thiếu GEMINI_API_KEY)."));
        }

        String text = geminiApiClient.generateText("Trả lời đúng 1 từ: OK").orElse("");

        if (text.startsWith("__GEMINI_HTTP__:")) {
            return ResponseEntity.ok(new ChatbotGeminiPingResponse(true, false, geminiApiClient.getModel(), text));
        }
        if (text.startsWith("__GEMINI_NETWORK__")) {
            return ResponseEntity.ok(new ChatbotGeminiPingResponse(true, false, geminiApiClient.getModel(), text));
        }
        if (text.startsWith("__GEMINI_EMPTY__")) {
            return ResponseEntity.ok(new ChatbotGeminiPingResponse(true, false, geminiApiClient.getModel(), text));
        }
        if (text.startsWith("__GEMINI_BLOCKED__:")) {
            return ResponseEntity.ok(new ChatbotGeminiPingResponse(true, false, geminiApiClient.getModel(), text));
        }
        if (text.isBlank()) {
            return ResponseEntity
                    .ok(new ChatbotGeminiPingResponse(true, false, geminiApiClient.getModel(), "No response"));
        }
        return ResponseEntity
                .ok(new ChatbotGeminiPingResponse(true, true, geminiApiClient.getEffectiveModel(), text.trim()));
    }

    @GetMapping("/gemini/models")
    public ResponseEntity<ChatbotGeminiModelsResponse> geminiModels(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).build();
        }
        ChatbotGeminiModelsResponse resp = new ChatbotGeminiModelsResponse();
        resp.setEnabled(geminiApiClient.isEnabled());
        resp.setConfiguredModel(geminiApiClient.getModel());
        resp.setEffectiveModel(geminiApiClient.getEffectiveModel());
        resp.setAvailableGenerateContentModels(geminiApiClient.listGenerateContentModels());
        return ResponseEntity.ok(resp);
    }

    @PostMapping
    public ResponseEntity<ChatbotMessageResponse> chat(
            @RequestBody ChatbotMessageRequest request,
            Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).build();
        }
        try {
            return ResponseEntity.ok(chatbotService.chat(authentication.getName(), request));
        } catch (Exception e) {
            ChatbotMessageResponse resp = new ChatbotMessageResponse();
            resp.setConversationId(request == null ? null : request.getConversationId());
            resp.setAnswer("Không thể kết nối chatbot. Bạn thử lại sau hoặc kiểm tra cấu hình.");
            resp.setNeedsClarification(false);
            return ResponseEntity.ok(resp);
        }
    }
}

