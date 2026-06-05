package vn.campuslife.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.campuslife.entity.ArticleReaction;
import vn.campuslife.enumeration.ReactionType;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArticleReactionRepository extends JpaRepository<ArticleReaction, Long> {
    
    Optional<ArticleReaction> findByArticleIdAndStudentId(Long articleId, Long studentId);
    
    long countByArticleIdAndReactionType(Long articleId, ReactionType reactionType);

    @Query("SELECT r.reactionType, COUNT(r.id) FROM ArticleReaction r WHERE r.article.id = :articleId GROUP BY r.reactionType")
    List<Object[]> countReactionsByArticleId(@Param("articleId") Long articleId);
}
