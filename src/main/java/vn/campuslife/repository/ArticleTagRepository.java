package vn.campuslife.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.campuslife.entity.ArticleTag;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArticleTagRepository extends JpaRepository<ArticleTag, Long> {
    Optional<ArticleTag> findBySlug(String slug);
    List<ArticleTag> findByIsActiveTrue();
}
