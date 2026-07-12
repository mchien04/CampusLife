package vn.campuslife.model.score;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.campuslife.enumeration.ScoreType;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkManualScoreResponse {
    private Long semesterId;
    private ScoreType scoreType;
    private int total;
    private int successCount;
    private int failureCount;
    private List<BulkManualScoreItemResult> results;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BulkManualScoreItemResult {
        private Long studentId;
        private boolean success;
        private ManualScoreResponse data;
        private String error;
    }
}
