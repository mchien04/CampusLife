package vn.campuslife.model.statistics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MiniGameStatisticsResponse {
    private Long totalMiniGames;
    private Long totalAttempts;
    private Long passedAttempts;
    private Long failedAttempts;
    private Double passRate; // passedAttempts / totalAttempts
    private Map<Long, MiniGameDetailItem> miniGameDetails; // miniGameId -> details
    private List<PopularMiniGameItem> popularMiniGames;
    private Map<Long, BigDecimal> averageScoreByMiniGame; // miniGameId -> average score
    private Map<Long, Double> averageCorrectAnswersByMiniGame; // miniGameId -> average correct answers

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MiniGameDetailItem {
        private Long miniGameId;
        private String title;
        private Long totalAttempts;
        private Long passedAttempts;
        private Long failedAttempts;
        private Double passRate;
        private BigDecimal averageScore;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PopularMiniGameItem {
        private Long miniGameId;
        private String title;
        private Long attemptCount;
        private Long uniqueStudentCount;
    }
}

