package vn.campuslife.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.campuslife.entity.ArticleComment;

import java.util.List;

@Repository
public interface ArticleCommentRepository extends JpaRepository<ArticleComment, Long> {
    
    Page<ArticleComment> findByArticleIdAndParentCommentIsNullAndIsHiddenFalseOrderByCreatedAtDesc(Long articleId, Pageable pageable);
    
    Page<ArticleComment> findByArticleIdAndParentCommentIsNullOrderByCreatedAtDesc(Long articleId, Pageable pageable);

    List<ArticleComment> findByParentCommentIdAndIsHiddenFalseOrderByCreatedAtAsc(Long parentCommentId);

    List<ArticleComment> findByParentCommentIdOrderByCreatedAtAsc(Long parentCommentId);

    int countByParentCommentIdAndIsHiddenFalse(Long parentCommentId);
}
