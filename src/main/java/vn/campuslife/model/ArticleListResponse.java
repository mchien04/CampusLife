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
public class ArticleListResponse {
    private Long id;
    private String title;
    private String slug;
    private String thumbnailUrl;
    private String seoDescription;
    private String registrationStatus;
    private Long activityId;
    private String shareLink;
    private ArticleType articleType;
    @JsonProperty("isPrimary")
    private boolean isPrimary;
    @JsonProperty("isPublished")
    private boolean isPublished;
    @JsonProperty("isFeatured")
    private boolean isFeatured;
    @JsonProperty("isPinned")
    private boolean isPinned;
    private LocalDateTime publishedAt;
    private Long viewCount;
    private Long wishlistCount;
    private String categoryName;
    private List<String> tags;
    private List<ArticleImageResponse> images;
    private Long commentCount;
}

