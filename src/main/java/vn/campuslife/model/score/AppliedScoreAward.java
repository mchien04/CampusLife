package vn.campuslife.model.score;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.campuslife.entity.ScoreEntry;
import vn.campuslife.enumeration.ScoreRuleTrigger;
import vn.campuslife.enumeration.ScoreType;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppliedScoreAward {
    private Long ruleId;
    private ScoreType scoreType;
    private String scoreTypeLabel;
    private BigDecimal points;
    private String displayUnit;
    private String displayText;
    private ScoreRuleTrigger triggerType;
    private Long scoreEntryId;

    public static AppliedScoreAward fromEntry(ScoreEntry entry) {
        if (entry == null) return null;
        ScoreType scoreType = entry.getScoreType();
        BigDecimal points = entry.getPoints();
        
        String label = getScoreTypeLabel(scoreType);
        String unit = getDisplayUnit(scoreType);
        String text = getDisplayText(points, scoreType);
        
        return AppliedScoreAward.builder()
                .ruleId(entry.getRule() != null ? entry.getRule().getId() : null)
                .scoreType(scoreType)
                .scoreTypeLabel(label)
                .points(points)
                .displayUnit(unit)
                .displayText(text)
                .triggerType(entry.getRule() != null ? entry.getRule().getTriggerType() : null)
                .scoreEntryId(entry.getId())
                .build();
    }

    private static String getScoreTypeLabel(ScoreType scoreType) {
        if (scoreType == null) return "";
        switch (scoreType) {
            case REN_LUYEN: return "Điểm rèn luyện";
            case CONG_TAC_XA_HOI: return "Điểm công tác xã hội";
            case CHUYEN_DE: return "Điểm chuyên đề";
            default: return scoreType.name();
        }
    }

    private static String getDisplayUnit(ScoreType scoreType) {
        if (scoreType == null) return "";
        switch (scoreType) {
            case REN_LUYEN: return "điểm";
            case CONG_TAC_XA_HOI: return "điểm";
            case CHUYEN_DE: return "buổi";
            default: return "điểm";
        }
    }

    private static String getDisplayText(BigDecimal points, ScoreType scoreType) {
        String label = getScoreTypeLabel(scoreType).toLowerCase();
        String unit = getDisplayUnit(scoreType);
        String sign = points.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";
        
        // Strip trailing zeros for display: e.g., 5.0 -> 5
        String pointsStr = points.stripTrailingZeros().toPlainString();
        
        if (scoreType == ScoreType.CHUYEN_DE) {
            return sign + pointsStr + " " + unit + " chuyên đề";
        }
        return sign + pointsStr + " " + unit + " " + label;
    }
}
