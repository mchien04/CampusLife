package vn.campuslife.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import vn.campuslife.entity.EventArticle;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface EventArticleRepository extends JpaRepository<EventArticle, Long>, JpaSpecificationExecutor<EventArticle> {
    Optional<EventArticle> findBySlugAndIsPublishedTrue(String slug);

    List<EventArticle> findByActivityId(Long activityId);

    List<EventArticle> findByActivityIdAndIsPublishedTrue(Long activityId);

    Optional<EventArticle> findByActivityIdAndIsPrimaryTrue(Long activityId);

    Page<EventArticle> findByActivityId(Long activityId, Pageable pageable);

    @Query("SELECT ea FROM EventArticle ea JOIN ea.activity a " +
           "WHERE a.seriesId = :seriesId AND ea.isPublished = true " +
           "ORDER BY a.seriesOrder ASC, ea.publishedAt DESC")
    List<EventArticle> findPublishedBySeriesId(@Param("seriesId") Long seriesId);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    @Query(value = "SELECT ea FROM EventArticle ea WHERE ea.isPublished = true ORDER BY ea.isPinned DESC, ea.priority DESC, ea.publishedAt DESC", countQuery = "SELECT COUNT(ea) FROM EventArticle ea WHERE ea.isPublished = true")
    Page<EventArticle> findAllPublishedOrderByPinnedAndPriority(Pageable pageable);

    @Query(value = "SELECT ea FROM EventArticle ea ORDER BY ea.isPinned DESC, ea.priority DESC, ea.publishedAt DESC, ea.updatedAt DESC",
            countQuery = "SELECT COUNT(ea) FROM EventArticle ea")
    Page<EventArticle> findAllOrderByPinnedAndPriority(Pageable pageable);

    @Query("SELECT ea FROM EventArticle ea WHERE ea.isPublished = true AND ea.isFeatured = true ORDER BY ea.publishedAt DESC")
    List<EventArticle> findFeaturedArticles(Pageable pageable);

    @Query(value = "SELECT ea FROM EventArticle ea WHERE ea.isPublished = true AND ea.category.id = :categoryId ORDER BY ea.isPinned DESC, ea.priority DESC, ea.publishedAt DESC", countQuery = "SELECT COUNT(ea) FROM EventArticle ea WHERE ea.isPublished = true AND ea.category.id = :categoryId")
    Page<EventArticle> findByCategoryId(@Param("categoryId") Long categoryId, Pageable pageable);

    @Query("SELECT ea FROM EventArticle ea WHERE ea.isPublished = true AND ea.id <> :excludeId AND ea.category.id = :categoryId ORDER BY ea.isPinned DESC, ea.priority DESC, ea.publishedAt DESC")
    List<EventArticle> findRelatedByCategoryId(@Param("excludeId") Long excludeId, @Param("categoryId") Long categoryId,
            Pageable pageable);

    @Query(value = "SELECT ea FROM EventArticle ea WHERE ea.isPublished = true AND (LOWER(ea.title) LIKE :keyword OR ea.content LIKE :keyword) ORDER BY ea.isPinned DESC, ea.priority DESC, ea.publishedAt DESC", countQuery = "SELECT COUNT(ea) FROM EventArticle ea WHERE ea.isPublished = true AND (LOWER(ea.title) LIKE :keyword OR ea.content LIKE :keyword)")
    Page<EventArticle> searchArticles(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT COUNT(ea) FROM EventArticle ea WHERE ea.isPublished = true")
    long countPublishedArticles();

    @Query("SELECT COUNT(ea) FROM EventArticle ea WHERE ea.isFeatured = true")
    long countFeaturedArticles();

    @Query("SELECT SUM(ea.viewCount) FROM EventArticle ea WHERE ea.isPublished = true")
    Long sumTotalViews();

    @Query("SELECT COUNT(ea) FROM EventArticle ea WHERE ea.isPublished = true AND ea.category.id = :categoryId")
    Long countPublishedArticlesByCategory(@Param("categoryId") Long categoryId);

    @Query("SELECT COUNT(ea) FROM EventArticle ea JOIN ea.tags t WHERE ea.isPublished = true AND t.id = :tagId")
    Long countPublishedArticlesByTag(@Param("tagId") Long tagId);

    @Query(value = "SELECT ea FROM EventArticle ea JOIN ea.tags t WHERE ea.isPublished = true AND t.slug = :tagSlug ORDER BY ea.isPinned DESC, ea.priority DESC, ea.publishedAt DESC", countQuery = "SELECT COUNT(ea) FROM EventArticle ea JOIN ea.tags t WHERE ea.isPublished = true AND t.slug = :tagSlug")
    Page<EventArticle> findPublishedArticlesByTag(@Param("tagSlug") String tagSlug, Pageable pageable);

    @Query("""
            SELECT CASE WHEN COUNT(ea) > 0 THEN true ELSE false END
            FROM EventArticle ea
            WHERE ea.id = :articleId
              AND ea.ownerDepartment.id IN :deptIds
            """)
    boolean existsByIdAndOwnerDepartmentIds(@Param("articleId") Long articleId, @Param("deptIds") Set<Long> deptIds);
}
