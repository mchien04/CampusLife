package vn.campuslife.model.score;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.campuslife.enumeration.ScoreAppealStatus;
import vn.campuslife.enumeration.ScoreType;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScoreAppealDecisionRequest {

    @NotNull
    private ScoreAppealStatus decision;

    private String decisionNotes;

    /** When set with APPROVED, creates a MANUAL_ADJUSTMENT after reversing the related deduction. */
    private BigDecimal adjustedPoints;

    private ScoreType scoreType;

    private Long semesterId;
}
