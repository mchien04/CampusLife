package vn.campuslife.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.campuslife.entity.EventArticle;

import java.util.Optional;

@Repository
public interface EventArticleRepository extends JpaRepository<EventArticle, Long> {
    Optional<EventArticle> findBySlugAndIsPublishedTrue(String slug);

    Optional<EventArticle> findByActivityId(Long activityId);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);
}
