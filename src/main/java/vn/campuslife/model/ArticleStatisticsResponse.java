package vn.campuslife.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArticleStatisticsResponse {
    private long totalArticles;
    private long publishedArticles;
    private long draftArticles;
    private long totalViews;
    private long totalWishlists;
    private long featuredArticles;
    private long pinnedArticles;
    private List<Map<String, Object>> topViewedArticles;
    private List<Map<String, Object>> recentlyPublished;
    private Map<String, Long> articlesByCategory;
    private Map<String, Long> articlesByMonth;
}
