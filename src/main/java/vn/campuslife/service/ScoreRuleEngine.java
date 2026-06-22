package vn.campuslife.service;

import vn.campuslife.entity.*;

public interface ScoreRuleEngine {
    void applyActivityCompleted(ActivityParticipation participation, User actor);

    void applyNoShowPenalty(ActivityRegistration registration, User actor);

    void applySubmissionGraded(TaskSubmission submission, User actor);

    void applyTaskOverdue(TaskAssignment assignment, User actor);

    void applyMiniGamePassed(MiniGameAttempt attempt, User actor);

    void applyMiniGameExhaustedAttempts(MiniGameAttempt attempt, User actor);

    void applySeriesMilestone(StudentSeriesProgress progress, User actor);

    void applySeriesMinimumRequirement(ActivitySeries series, Student student, int completedCount, User actor);
}
