package vn.campuslife.model.activity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScoreRulePreviewRow {
    private String triggerType;
    private String scenario;      // PASS, FAIL, PENALTY, BONUS, REWARD
    private String scoreType;
    private BigDecimal points;
    private String audience;
    private String semester;
    private String description;
}
