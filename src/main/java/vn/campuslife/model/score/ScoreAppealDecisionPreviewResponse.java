package vn.campuslife.model.score;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.campuslife.enumeration.ScoreAppealStatus;
import vn.campuslife.enumeration.ScoreType;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScoreAppealDecisionPreviewResponse {
    private Long appealId;
    private Long studentId;
    private String studentCode;
    private String studentFullName;
    private Long semesterId;
    private ScoreType scoreType;
    private ScoreAppealStatus decision;
    private BigDecimal currentScore;
    private BigDecimal adjustedPoints;
    private BigDecimal projectedScore;
    /** true khi APPROVED và có adjustedPoints — sẽ tạo MANUAL_ADJUSTMENT nếu confirm decide */
    private boolean willCreateLedgerEntry;
    /** true khi APPROVED — sẽ reverse relatedScoreEntry (gỡ điểm trừ) */
    private boolean willReverseRelated;
    private Long relatedScoreEntryId;
    private BigDecimal relatedEntryPoints;
    private ScoreType relatedScoreType;
    /** Điểm loại bị trừ sau khi reverse */
    private BigDecimal projectedRelatedScore;
    private String note;
}
