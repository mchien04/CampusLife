package vn.campuslife.model.score;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.campuslife.enumeration.ScoreType;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkManualScoreRequest {

    /** Học kỳ tích điểm — bắt buộc, áp dụng cho tất cả sinh viên trong batch. */
    @NotNull
    private Long semesterId;

    @NotNull
    private ScoreType scoreType;

    @NotBlank
    private String reason;

    private Long activityId;

    @NotEmpty
    @Valid
    private List<BulkManualScoreEntryRequest> entries;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BulkManualScoreEntryRequest {
        @NotNull
        private Long studentId;

        @NotNull
        private BigDecimal points;

        /** Optional override lý do cho từng sinh viên. */
        private String reason;
    }
}
