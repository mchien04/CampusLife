package vn.campuslife.repository;

import org.springframework.data.jpa.repository.JpaRepository;
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
}