package vn.campuslife.model.score;

import lombok.Data;
import vn.campuslife.enumeration.ScoreEntrySourceType;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ScoreBreakdownResponse {
    private Long semesterId;
    private String semesterName;
    private Long studentId;
    private List<SourceBreakdown> breakdowns;

    @Data
    public static class SourceBreakdown {
        private ScoreEntrySourceType sourceType;
        private BigDecimal totalPoints;
        private Long entryCount;
    }
}
