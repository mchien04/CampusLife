package vn.campuslife.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArticleCommentRequest {
    @NotBlank(message = "Comment content cannot be blank")
    private String content;
    
    private Long parentCommentId; // null = root comment, not null = reply
}
