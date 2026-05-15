package vn.campuslife.model;

import lombok.Data;
import vn.campuslife.enumeration.ChatbotPageContext;

@Data
public class ChatbotMessageRequest {
    private Long conversationId;
    private Long contextActivityId;
    private String contextArticleSlug;
    private ChatbotPageContext pageContext = ChatbotPageContext.GLOBAL;
    private String message;
}
