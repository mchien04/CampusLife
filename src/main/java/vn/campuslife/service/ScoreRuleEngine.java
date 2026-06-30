package vn.campuslife.service;

import vn.campuslife.entity.*;
import vn.campuslife.model.score.AppliedScoreAward;
import java.util.List;

public interface ScoreRuleEngine {
    List<AppliedScoreAward> applyActivityCompleted(ActivityParticipation participation, User actor);

    void applyNoShowPenalty(ActivityRegistration registration, User actor);

    List<AppliedScoreAward> applySubmissionGraded(TaskSubmission submission, User actor);

    void applyTaskOverdue(TaskAssignment assignment, User actor);

    void applyMiniGamePassed(MiniGameAttempt attempt, User actor);

    void applyMiniGameExhaustedAttempts(MiniGameAttempt attempt, User actor);

    void applySeriesMilestone(StudentSeriesProgress progress, User actor);

    void applySeriesMinimumRequirement(ActivitySeries series, Student student, int completedCount, User actor);
}
