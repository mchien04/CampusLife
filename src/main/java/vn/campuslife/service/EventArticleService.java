package vn.campuslife.service;

import org.springframework.data.domain.Page;

import java.util.List;

public interface EventArticleService {
    // Student Public APIs
    vn.campuslife.model.ArticleListResponse getPublishedArticleBySlug(String slug);

    org.springframework.data.domain.Page<vn.campuslife.model.ArticleListResponse> getAllPublishedArticles(int page, int size);

    org.springframework.data.domain.Page<vn.campuslife.model.ArticleListResponse> getAllArticlesForAdmin(int page, int size);

    org.springframework.data.domain.Page<vn.campuslife.model.ArticleListResponse> getPublishedArticlesByCategory(String categorySlug, int page, int size);

    org.springframework.data.domain.Page<vn.campuslife.model.ArticleListResponse> searchPublishedArticles(String keyword, int page, int size);

    List<vn.campuslife.model.ArticleListResponse> getFeaturedArticles();

    vn.campuslife.model.ArticleDetailResponse getArticleDetailBySlug(String slug, Long studentId);

    List<vn.campuslife.model.ArticleListResponse> getRelatedArticles(String slug, int limit);

    // Wishlist APIs
    vn.campuslife.model.Response addToWishlist(String slug, String username);

    vn.campuslife.model.Response removeFromWishlist(String slug, String username);

    org.springframework.data.domain.Page<vn.campuslife.model.ArticleWishlistItemResponse> getStudentWishlist(String username, int page, int size);

    boolean isInWishlist(String slug, String username);

    // Admin/Manager CRUD
    vn.campuslife.model.EventArticleAdminResponse createArticle(vn.campuslife.model.EventArticleUpsertRequest request);

    vn.campuslife.model.EventArticleAdminResponse updateArticle(Long articleId, vn.campuslife.model.EventArticleUpsertRequest request);

    vn.campuslife.model.EventArticleAdminResponse publishArticle(Long articleId);

    vn.campuslife.model.EventArticleAdminResponse unpublishArticle(Long articleId);

    vn.campuslife.model.EventArticleAdminResponse getArticleById(Long articleId);

    List<vn.campuslife.model.EventArticleAdminResponse> getArticlesByActivityId(Long activityId);

    vn.campuslife.model.EventArticleAdminResponse setPrimaryArticle(Long articleId);

    List<vn.campuslife.model.ArticleListResponse> getArticlesBySeriesId(Long seriesId);

    // Admin Statistics Dashboard
    vn.campuslife.model.ArticleStatisticsResponse getArticleStatistics();

    // Category Management
    List<vn.campuslife.model.ArticleCategoryResponse> getAllCategories();

    vn.campuslife.model.ArticleCategoryResponse createCategory(vn.campuslife.model.ArticleCategoryRequest request);

    vn.campuslife.model.ArticleCategoryResponse updateCategory(Long categoryId, vn.campuslife.model.ArticleCategoryRequest request);

    void deleteCategory(Long categoryId);

    // Tag Management
    List<vn.campuslife.model.ArticleTagResponse> getAllTags();

    vn.campuslife.model.ArticleTagResponse createTag(vn.campuslife.model.ArticleTagRequest request);

    void deleteTag(Long tagId);

    // Image Management
    vn.campuslife.model.ArticleImageResponse addImageToArticle(Long articleId, vn.campuslife.model.ArticleImageRequest request);

    void removeImageFromArticle(Long articleId, Long imageId);

    // Waitlist & Calendar
    vn.campuslife.model.Response registerForWaitlist(String slug, String username);

    byte[] generateIcsFile(String slug);

    // Analytics
    void trackView(String slug);

    // Phase 3 Features
    vn.campuslife.model.Response addReaction(String slug, String username, vn.campuslife.enumeration.ReactionType type);

    vn.campuslife.model.Response removeReaction(String slug, String username);

    java.util.Map<String, Long> getReactionCounts(String slug);

    vn.campuslife.model.Response trackShare(String slug);

    org.springframework.data.domain.Page<vn.campuslife.model.ArticleHistoryResponse> getReadingHistory(String username, int page, int size);

    void deleteReadingHistory(String username, Long historyId);

    void clearAllReadingHistory(String username);

    java.util.List<vn.campuslife.model.ArticleListResponse> getTrendingArticles(int days, int limit);

    org.springframework.data.domain.Page<vn.campuslife.model.ArticleListResponse> getFilteredArticlesForAdmin(
            String status, Long activityId, Long categoryId, vn.campuslife.enumeration.ArticleType articleType,
            Boolean featured, Boolean pinned, Boolean primary, String search, String dateFrom, String dateTo,
            int page, int size);

    byte[] exportArticlesToExcel(
            String status, Long activityId, Long categoryId, vn.campuslife.enumeration.ArticleType articleType,
            Boolean featured, Boolean pinned, Boolean primary, String search, String dateFrom, String dateTo);
}
