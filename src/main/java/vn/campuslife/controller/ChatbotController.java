package vn.campuslife.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.campuslife.model.ChatbotMessageRequest;
import vn.campuslife.model.ChatbotMessageResponse;
import vn.campuslife.service.ChatbotService;

@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
public class ChatbotController {

    private final ChatbotService chatbotService;

    @PostMapping
    public ResponseEntity<ChatbotMessageResponse> chat(
            @RequestBody ChatbotMessageRequest request,
            Authentication authentication
    ) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(chatbotService.chat(authentication.getName(), request));
    }
}
