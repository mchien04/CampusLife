package vn.campuslife.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArticleCommentResponse {
    private Long id;
    private Long articleId;
    private Long parentCommentId;
    private String content;
    private boolean isFlagged;
    private String flagReason;
    private boolean isHidden;
    private StudentBasicInfo student;
    private List<ArticleCommentResponse> replies;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentBasicInfo {
        private Long id;
        private String fullName;
        private String studentCode;
        private String avatarUrl;
    }
}
