package vn.campuslife.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.campuslife.entity.EventArticleSlugHistory;

import java.util.Optional;

@Repository
public interface EventArticleSlugHistoryRepository extends JpaRepository<EventArticleSlugHistory, Long> {
    Optional<EventArticleSlugHistory> findByOldSlug(String oldSlug);
}
