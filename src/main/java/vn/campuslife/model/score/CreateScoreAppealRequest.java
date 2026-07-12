package vn.campuslife.model.score;

import jakarta.validation.constraints.NotBlank;
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
public class CreateScoreAppealRequest {

    @NotNull
    private Long semesterId;

    @NotNull
    private ScoreType scoreType;

    /** Bắt buộc: ScoreEntry bị trừ (points &lt; 0, ACTIVE) đang khiếu nại. */
    @NotNull
    private Long relatedScoreEntryId;

    @NotBlank
    private String title;

    @NotBlank
    private String reason;

    private BigDecimal requestedPoints;

    /**
     * Public URLs returned from POST /api/scores/appeals/evidence.
     * Stored as relative paths server-side.
     */
    private List<String> evidenceUrls;
}
