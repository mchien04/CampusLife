package vn.campuslife.model.score;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.campuslife.enumeration.ScoreAppealStatus;
import vn.campuslife.enumeration.ScoreType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScoreAppealResponse {
    private Long id;
    private Long studentId;
    private String studentCode;
    private String studentFullName;
    private Long semesterId;
    private ScoreType scoreType;
    private Long relatedScoreEntryId;
    private String title;
    private String reason;
    /** Public URLs for evidence images */
    private List<String> evidenceUrls;
    private BigDecimal requestedPoints;
    private ScoreAppealStatus status;
    private String decisionNotes;
    private LocalDateTime decidedAt;
    private Long decidedById;
    private String decidedByUsername;
    private Long resultingScoreEntryId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ScoreAppealMessageResponse> messages;
}
