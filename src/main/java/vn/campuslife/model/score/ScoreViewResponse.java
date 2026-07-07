package vn.campuslife.model.score;

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
        private BigDecimal total;           // Điểm của học kỳ HIỆN TẠI
        private BigDecimal cumulativeTotal; // Tổng đã tích lũy suốt các kỳ (chỉ có với loại tích lũy)
        private List<ScoreItem> items;
    }

    @Data
    public static class ScoreItem {
        private BigDecimal score;
        private String notes;
    }
}
