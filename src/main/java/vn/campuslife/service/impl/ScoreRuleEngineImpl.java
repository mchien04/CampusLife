package vn.campuslife.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.campuslife.entity.*;
import vn.campuslife.enumeration.ScoreEntrySourceType;
import vn.campuslife.enumeration.ScoreRuleTrigger;
import vn.campuslife.enumeration.ScoreRuleAudience;
import vn.campuslife.enumeration.ScoreRuleCalculation;
import vn.campuslife.model.ScoreEntryCommand;
import vn.campuslife.service.ActivityScoreRuleService;
import vn.campuslife.service.ScoreEntryService;
import vn.campuslife.service.ScoreRuleEngine;
import vn.campuslife.service.ScoreSemesterResolver;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScoreRuleEngineImpl implements ScoreRuleEngine {

    private final ActivityScoreRuleService ruleService;
    private final ScoreEntryService scoreEntryService;
    private final ScoreSemesterResolver semesterResolver;

    @Override
    @Transactional
    public void applyActivityCompleted(ActivityParticipation participation, User actor) {
        if (participation.getIsCompleted() == null || !participation.getIsCompleted()) return;
        
        Activity activity = participation.getRegistration().getActivity();
        if (activity.getSeriesId() != null) {
            log.info("Skipping individual completion points for activity {} belonging to series {}", activity.getId(), activity.getSeriesId());
            return;
        }
        
        Student student = participation.getRegistration().getStudent();
        
        List<ActivityScoreRule> rules = ruleService.getEnabledRules(activity.getId(), ScoreRuleTrigger.PARTICIPATION_COMPLETED);
        
        for (ActivityScoreRule rule : rules) {
            if (!isEligible(rule, student)) continue;
            
            BigDecimal points = rule.getPoints();
            
            Semester semester = semesterResolver.resolveSemester(activity, rule, participation.getDate());
            
            scoreEntryService.upsertEntry(ScoreEntryCommand.builder()
                    .studentId(student.getId())
                    .activityId(activity.getId())
                    .ruleId(rule.getId())
                    .semesterId(semester.getId())
                    .scoreType(rule.getScoreType())
                    .sourceType(ScoreEntrySourceType.ACTIVITY_PARTICIPATION)
                    .sourceId(participation.getId())
                    .points(points)
                    .reason("Completed activity: " + activity.getName())
                    .actor(actor)
                    .build());
        }
    }

    @Override
    @Transactional
    public void applySubmissionGraded(TaskSubmission submission, User actor) {
        Activity activity = submission.getTask().getActivity();
        if (activity != null && activity.getSeriesId() != null) {
            log.info("Skipping individual submission points for activity {} belonging to series {}", activity.getId(), activity.getSeriesId());
            return;
        }
        
        Student student = submission.getStudent();
        
        List<ActivityScoreRule> rules = ruleService.getEnabledRules(activity.getId(), ScoreRuleTrigger.SUBMISSION_GRADED);
        
        for (ActivityScoreRule rule : rules) {
            if (!isEligible(rule, student)) continue;
            
            BigDecimal points;
            if (vn.campuslife.enumeration.SubmissionStatus.GRADED.equals(submission.getStatus()) && Boolean.TRUE.equals(submission.getIsCompleted())) {
                points = rule.getPoints();
            } else {
                points = rule.getFailPoints();
            }
            
            Semester semester = semesterResolver.resolveSemester(activity, rule, submission.getSubmittedAt());
            
            scoreEntryService.upsertEntry(ScoreEntryCommand.builder()
                    .studentId(student.getId())
                    .activityId(activity.getId())
                    .ruleId(rule.getId())
                    .semesterId(semester.getId())
                    .scoreType(rule.getScoreType())
                    .sourceType(ScoreEntrySourceType.TASK_SUBMISSION)
                    .sourceId(submission.getId())
                    .points(points)
                    .reason("Graded submission for activity: " + activity.getName())
                    .actor(actor)
                    .build());
        }
    }

    @Override
    @Transactional
    public void applyMiniGamePassed(MiniGameAttempt attempt, User actor) {
        if (!"PASSED".equals(attempt.getStatus())) return;
        
        Activity activity = attempt.getMiniGame().getActivity();
        if (activity != null && activity.getSeriesId() != null) {
            log.info("Skipping individual minigame points for activity {} belonging to series {}", activity.getId(), activity.getSeriesId());
            return;
        }
        
        Student student = attempt.getStudent();
        
        List<ActivityScoreRule> rules = ruleService.getEnabledRules(activity.getId(), ScoreRuleTrigger.MINIGAME_PASSED);
        
        for (ActivityScoreRule rule : rules) {
            if (!isEligible(rule, student)) continue;
            
            BigDecimal points = rule.getPoints();
            
            Semester semester = semesterResolver.resolveSemester(activity, rule, attempt.getSubmittedAt());
            
            scoreEntryService.upsertEntry(ScoreEntryCommand.builder()
                    .studentId(student.getId())
                    .activityId(activity.getId())
                    .ruleId(rule.getId())
                    .semesterId(semester.getId())
                    .scoreType(rule.getScoreType())
                    .sourceType(ScoreEntrySourceType.MINIGAME_ATTEMPT)
                    .sourceId(attempt.getId())
                    .points(points)
                    .reason("Passed minigame for activity: " + activity.getName())
                    .actor(actor)
                    .build());
        }
    }

    @Override
    @Transactional
    public void applySeriesMilestone(StudentSeriesProgress progress, User actor) {
        // We will implement this after checking ActivitySeriesServiceImpl
    }

    private boolean isEligible(ActivityScoreRule rule, Student student) {
        if (rule.getAudience() == ScoreRuleAudience.ALL_PARTICIPANTS) return true;
        
        boolean inDepartment = rule.getTargetDepartments().contains(student.getDepartment());
        
        if (rule.getAudience() == ScoreRuleAudience.DEPARTMENT_ONLY) return inDepartment;
        if (rule.getAudience() == ScoreRuleAudience.OUTSIDE_DEPARTMENTS_ONLY) return !inDepartment;
        
        return false;
    }
}


