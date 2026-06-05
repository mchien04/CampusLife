package vn.campuslife.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.campuslife.entity.ArticleWishlist;
import vn.campuslife.entity.EventArticle;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArticleWishlistRepository extends JpaRepository<ArticleWishlist, Long> {
    Optional<ArticleWishlist> findByArticleIdAndStudentId(Long articleId, Long studentId);
    boolean existsByArticleIdAndStudentId(Long articleId, Long studentId);
    Page<ArticleWishlist> findByStudentIdOrderByCreatedAtDesc(Long studentId, Pageable pageable);
    List<ArticleWishlist> findByStudentIdOrderByCreatedAtDesc(Long studentId);
    long countByArticleId(Long articleId);
    void deleteByArticleIdAndStudentId(Long articleId, Long studentId);
    List<ArticleWishlist> findByArticleId(Long articleId);
}
