package vn.campuslife.model;

import lombok.Data;
import vn.campuslife.enumeration.ScoreType;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ScoreViewResponse {
    private Long studentId;
    private Long semesterId;
    private List<ScoreTypeSummary> summaries;

    @Data
    public static class ScoreTypeSummary {
        private ScoreType scoreType;
        private BigDecimal total;
        private List<ScoreItem> items;
    }

    @Data
    public static class ScoreItem {
        private BigDecimal score;
        private List<Long> activityIds; // Danh sách activity đã nhận điểm
        private String notes;
    }
}
