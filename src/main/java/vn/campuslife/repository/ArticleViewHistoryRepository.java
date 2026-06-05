package vn.campuslife.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.campuslife.entity.ArticleViewHistory;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ArticleViewHistoryRepository extends JpaRepository<ArticleViewHistory, Long> {

    boolean existsByStudentIdAndArticleIdAndViewedAtAfter(
            Long studentId,
            Long articleId,
            LocalDateTime viewedAt
    );

    List<ArticleViewHistory> findTop20ByStudentIdOrderByViewedAtDesc(Long studentId);

    List<ArticleViewHistory> findByStudentIdOrderByViewedAtDesc(Long studentId);

    Page<ArticleViewHistory> findByStudentIdOrderByViewedAtDesc(Long studentId, Pageable pageable);

    void deleteByStudentId(Long studentId);

    @Query("SELECT avh.article.id, COUNT(avh.id) as recentViews " +
           "FROM ArticleViewHistory avh " +
           "WHERE avh.viewedAt >= :since " +
           "GROUP BY avh.article.id " +
           "ORDER BY recentViews DESC")
    List<Object[]> findTrendingArticleIds(@Param("since") LocalDateTime since, Pageable pageable);
}