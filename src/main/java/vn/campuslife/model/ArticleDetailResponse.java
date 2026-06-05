package vn.campuslife.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.campuslife.enumeration.ScoreType;
import vn.campuslife.enumeration.ReactionType;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArticleDetailResponse {
    private ReactionType myReaction;
    private Long id;
    private String title;
    private String slug;
    private String thumbnailUrl;
    private String content;
    private String seoTitle;
    private String seoDescription;
    private boolean published;
    private LocalDateTime publishedAt;
    private String registrationStatus;
    private Long viewCount;
    private Long wishlistCount;
    private boolean isFeatured;
    private boolean isPinned;
    private int priority;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private ArticleActivityInfo activityInfo;
    private ArticleCategoryInfo category;
    private List<String> tags;
    private List<ArticleImageResponse> images;
    private List<ArticleImageResponse> coverImages;
    private boolean isWishlisted;
    private String redirectedFrom;
    private String currentSlug;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ArticleActivityInfo {
        private Long id;
        private String name;
        private String location;
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        private LocalDateTime registrationStartDate;
        private LocalDateTime registrationDeadline;
        private ScoreType scoreType;
        private String shareLink;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ArticleCategoryInfo {
        private Long id;
        private String name;
        private String slug;
    }
}
