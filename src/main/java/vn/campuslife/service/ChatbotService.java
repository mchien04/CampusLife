package vn.campuslife.service;

import vn.campuslife.model.ChatbotMessageRequest;
import vn.campuslife.model.ChatbotMessageResponse;

public interface ChatbotService {
    ChatbotMessageResponse chat(String username, ChatbotMessageRequest request);
}
