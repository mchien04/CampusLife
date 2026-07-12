package vn.campuslife.model.score;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.campuslife.enumeration.ScoreType;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ManualScoreRequest {

    @NotNull
    private Long studentId;

    @NotNull
    private Long semesterId;

    @NotNull
    private ScoreType scoreType;

    @NotNull
    private BigDecimal points;

    @NotBlank
    private String reason;

    private Long activityId;
}
