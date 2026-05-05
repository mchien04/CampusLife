package vn.campuslife.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.campuslife.entity.EventArticle;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventArticleRepository extends JpaRepository<EventArticle, Long> {
    Optional<EventArticle> findBySlugAndIsPublishedTrue(String slug);

    Optional<EventArticle> findByActivityId(Long activityId);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    @Query(value = "SELECT ea FROM EventArticle ea WHERE ea.isPublished = true ORDER BY ea.isPinned DESC, ea.priority DESC, ea.publishedAt DESC", countQuery = "SELECT COUNT(ea) FROM EventArticle ea WHERE ea.isPublished = true")
    Page<EventArticle> findAllPublishedOrderByPinnedAndPriority(Pageable pageable);

    @Query(value = "SELECT ea FROM EventArticle ea ORDER BY ea.isPinned DESC, ea.priority DESC, ea.publishedAt DESC, ea.updatedAt DESC",
            countQuery = "SELECT COUNT(ea) FROM EventArticle ea")
    Page<EventArticle> findAllOrderByPinnedAndPriority(Pageable pageable);

    @Query("SELECT ea FROM EventArticle ea WHERE ea.isPublished = true AND ea.isFeatured = true ORDER BY ea.publishedAt DESC")
    List<EventArticle> findFeaturedArticles();

    @Query(value = "SELECT ea FROM EventArticle ea WHERE ea.isPublished = true AND ea.category.id = :categoryId ORDER BY ea.isPinned DESC, ea.priority DESC, ea.publishedAt DESC", countQuery = "SELECT COUNT(ea) FROM EventArticle ea WHERE ea.isPublished = true AND ea.category.id = :categoryId")
    Page<EventArticle> findByCategoryId(@Param("categoryId") Long categoryId, Pageable pageable);

    @Query("SELECT ea FROM EventArticle ea WHERE ea.isPublished = true AND ea.id <> :excludeId AND ea.category.id = :categoryId ORDER BY ea.isPinned DESC, ea.priority DESC, ea.publishedAt DESC")
    List<EventArticle> findRelatedByCategoryId(@Param("excludeId") Long excludeId, @Param("categoryId") Long categoryId,
            Pageable pageable);

    @Query(value = "SELECT ea FROM EventArticle ea WHERE ea.isPublished = true AND (LOWER(ea.title) LIKE :keyword OR ea.content LIKE :keyword) ORDER BY ea.isPinned DESC, ea.priority DESC, ea.publishedAt DESC", countQuery = "SELECT COUNT(ea) FROM EventArticle ea WHERE ea.isPublished = true AND (LOWER(ea.title) LIKE :keyword OR ea.content LIKE :keyword)")
    Page<EventArticle> searchArticles(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT COUNT(ea) FROM EventArticle ea WHERE ea.isPublished = true")
    long countPublishedArticles();

    @Query("SELECT SUM(ea.viewCount) FROM EventArticle ea WHERE ea.isPublished = true")
    Long sumTotalViews();
}
