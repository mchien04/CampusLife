package vn.campuslife.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventArticleAdminResponse {
    private Long id;
    private Long activityId;
    private String title;
    private String slug;
    private String thumbnailUrl;
    private String content;
    private String seoTitle;
    private String seoDescription;
    private boolean published;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

