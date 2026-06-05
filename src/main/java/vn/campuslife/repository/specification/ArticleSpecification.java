package vn.campuslife.repository.specification;

import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import vn.campuslife.entity.EventArticle;
import vn.campuslife.enumeration.ArticleType;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ArticleSpecification {

    public static Specification<EventArticle> filterArticles(
            String status,
            Long activityId,
            Long categoryId,
            ArticleType articleType,
            Boolean featured,
            Boolean pinned,
            Boolean primary,
            String search,
            LocalDateTime dateFrom,
            LocalDateTime dateTo) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null && !status.equalsIgnoreCase("ALL")) {
                if (status.equalsIgnoreCase("PUBLISHED")) {
                    predicates.add(cb.isTrue(root.get("isPublished")));
                } else if (status.equalsIgnoreCase("DRAFT")) {
                    predicates.add(cb.isFalse(root.get("isPublished")));
                }
            }

            if (activityId != null) {
                predicates.add(cb.equal(root.get("activity").get("id"), activityId));
            }

            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }

            if (articleType != null) {
                predicates.add(cb.equal(root.get("articleType"), articleType));
            }

            if (featured != null) {
                predicates.add(cb.equal(root.get("isFeatured"), featured));
            }

            if (pinned != null) {
                predicates.add(cb.equal(root.get("isPinned"), pinned));
            }

            if (primary != null) {
                predicates.add(cb.equal(root.get("isPrimary"), primary));
            }

            if (search != null && !search.isBlank()) {
                String searchPattern = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), searchPattern),
                        cb.like(cb.lower(root.get("content")), searchPattern)
                ));
            }

            if (dateFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("publishedAt"), dateFrom));
            }

            if (dateTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("publishedAt"), dateTo));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
