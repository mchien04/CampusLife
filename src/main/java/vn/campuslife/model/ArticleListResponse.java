package vn.campuslife.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private String registrationLink;
    private boolean isPublished;
    private boolean isFeatured;
    private boolean isPinned;
    private LocalDateTime publishedAt;
    private Long viewCount;
    private Long wishlistCount;
    private String categoryName;
    private List<String> tags;
    private List<ArticleImageResponse> images;
}
