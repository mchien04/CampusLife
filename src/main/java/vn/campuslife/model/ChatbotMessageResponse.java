package vn.campuslife.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotMessageResponse {
    private Long conversationId;
    private String answer;
    private ChatbotResolvedActivityResponse resolvedActivity;
    private boolean needsClarification = false;
    private List<ChatbotActivityOptionResponse> activityOptions = new ArrayList<>();
}
