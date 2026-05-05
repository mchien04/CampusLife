package vn.campuslife.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArticleWishlistItemResponse {
    private Long id;
    private Long articleId;
    private String title;
    private String slug;
    private String thumbnailUrl;
    private String seoDescription;
    private boolean isPublished;
    private LocalDateTime publishedAt;
    private String registrationStatus;
    private LocalDateTime wishlistedAt;
}
