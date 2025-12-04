package vn.campuslife.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.campuslife.enumeration.ScoreType;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScoreHistoryViewResponse {
    private Long studentId;
    private String studentCode;
    private String studentName;
    private Long semesterId;
    private String semesterName;
    private ScoreType scoreType;
    private BigDecimal currentScore;
    private List<ScoreHistoryDetailResponse> scoreHistories;
    private List<ActivityParticipationDetailResponse> activityParticipations;
    private Long totalRecords;
    private Integer page;
    private Integer size;
    private Integer totalPages;
}

