package vn.campuslife.service.ai;

import vn.campuslife.enumeration.ChatbotIntent;

public record ChatbotNluResult(
        ChatbotIntent intent,
        Integer optionIndex,
        String activityQuery
) {
}
