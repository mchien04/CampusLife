package vn.campuslife.service;

import vn.campuslife.entity.*;

public interface ScoreRuleEngine {
    void applyActivityCompleted(ActivityParticipation participation, User actor);
    void applySubmissionGraded(TaskSubmission submission, User actor);
    void applyMiniGamePassed(MiniGameAttempt attempt, User actor);
    void applySeriesMilestone(StudentSeriesProgress progress, User actor);
}
