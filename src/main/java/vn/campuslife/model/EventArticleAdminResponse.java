package vn.campuslife.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.campuslife.enumeration.ArticleType;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventArticleAdminResponse {
    private Long id;
    private Long activityId;
    private String activityName;
    private ArticleType articleType;
    @JsonProperty("isPrimary")
    private boolean isPrimary;
    private String title;
    private String slug;
    private String thumbnailUrl;
    private String content;
    private String seoTitle;
    private String seoDescription;
    private boolean published;
    private LocalDateTime publishedAt;
    private Long viewCount;
    private Long wishlistCount;
    private boolean featured;
    private boolean pinned;
    private int priority;
    private Long categoryId;
    private String categoryName;
    private List<String> tagNames;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
