package vn.campuslife.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.campuslife.entity.ArticleImage;

import java.util.List;

@Repository
public interface ArticleImageRepository extends JpaRepository<ArticleImage, Long> {
    List<ArticleImage> findByArticleIdOrderByDisplayOrderAsc(Long articleId);
    void deleteByArticleId(Long articleId);
}
