package vn.campuslife.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.campuslife.entity.Activity;
import vn.campuslife.entity.ActivityRegistration;
import vn.campuslife.entity.ActivityParticipation;
import vn.campuslife.entity.ActivityScoreRule;
import vn.campuslife.entity.ActivitySeries;
import vn.campuslife.entity.MiniGameAttempt;
import vn.campuslife.entity.Semester;
import vn.campuslife.entity.Student;
import vn.campuslife.entity.StudentSeriesProgress;
import vn.campuslife.entity.TaskAssignment;
import vn.campuslife.entity.TaskSubmission;
import vn.campuslife.entity.User;
import vn.campuslife.enumeration.ScoreEntrySourceType;
import vn.campuslife.enumeration.ScoreRuleAudience;
import vn.campuslife.enumeration.ScoreRuleCalculation;
import vn.campuslife.enumeration.ScoreRuleTrigger;
import vn.campuslife.enumeration.AttemptStatus;
import vn.campuslife.model.score.ScoreEntryCommand;
import vn.campuslife.repository.ActivityRepository;
import vn.campuslife.repository.SemesterRepository;
import vn.campuslife.repository.StudentSeriesProgressRepository;
import vn.campuslife.service.ActivityScoreRuleService;
import vn.campuslife.service.ScoreEntryService;
import vn.campuslife.service.ScoreRuleEngine;
import vn.campuslife.service.ScoreSemesterResolver;
import vn.campuslife.service.SemesterHelperService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScoreRuleEngineImpl implements ScoreRuleEngine {

    private final ActivityScoreRuleService ruleService;
    private final ScoreEntryService scoreEntryService;
    private final ScoreSemesterResolver semesterResolver;
    private final StudentSeriesProgressRepository progressRepository;
    private final ActivityRepository activityRepository;
    private final SemesterHelperService semesterHelperService;
    private final SemesterRepository semesterRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional
    public void applyActivityCompleted(ActivityParticipation participation, User actor) {
        Activity activity = participation.getRegistration().getActivity();
        if (activity.getSeriesId() != null) {
            log.info("Skipping individual completion points for activity {} belonging to series {}", activity.getId(),
                    activity.getSeriesId());
            return;
        }

        Student student = participation.getRegistration().getStudent();

        List<ActivityScoreRule> rules = ruleService.getEnabledRules(activity.getId(),
                ScoreRuleTrigger.PARTICIPATION_COMPLETED);

        for (ActivityScoreRule rule : rules) {
            if (!isEligible(rule, student))
                continue;

            BigDecimal points = Boolean.TRUE.equals(participation.getIsCompleted())
                    ? rule.getPoints()
                    : rule.getFailPoints();

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
    public void applyNoShowPenalty(ActivityRegistration registration, User actor) {
        if (registration == null || registration.getActivity() == null || registration.getStudent() == null) {
            return;
        }

        Activity activity = registration.getActivity();
        if (activity.getSeriesId() != null) {
            log.info("Skipping no-show penalty for activity {} belonging to series {}", activity.getId(),
                    activity.getSeriesId());
            return;
        }

        Student student = registration.getStudent();
        List<ActivityScoreRule> rules = ruleService.getEnabledRules(activity.getId(), ScoreRuleTrigger.NO_SHOW);

        for (ActivityScoreRule rule : rules) {
            if (!isEligible(rule, student)) {
                continue;
            }

            BigDecimal points = applySignForFailure(rule, rule.getFailPoints() != null ? rule.getFailPoints() : BigDecimal.ZERO);
            if (points.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }

            Semester semester = semesterResolver.resolveSemester(
                    activity,
                    rule,
                    activity.getEndDate() != null ? activity.getEndDate() : LocalDateTime.now());

            scoreEntryService.upsertEntry(ScoreEntryCommand.builder()
                    .studentId(student.getId())
                    .activityId(activity.getId())
                    .ruleId(rule.getId())
                    .semesterId(semester.getId())
                    .scoreType(rule.getScoreType())
                    .sourceType(ScoreEntrySourceType.ACTIVITY_REGISTRATION)
                    .sourceId(registration.getId())
                    .points(points)
                    .reason("No-show penalty for activity: " + activity.getName())
                    .actor(actor)
                    .build());
        }
    }

    @Override
    @Transactional
    public void applyMiniGameExhaustedAttempts(MiniGameAttempt attempt, User actor) {
        if (attempt == null || attempt.getStatus() != AttemptStatus.FAILED || attempt.getMiniGame() == null) {
            return;
        }

        Activity activity = attempt.getMiniGame().getActivity();
        if (activity == null || activity.getSeriesId() != null) {
            if (activity != null && activity.getSeriesId() != null) {
                log.info("Skipping exhausted-attempt minigame penalty for activity {} belonging to series {}",
                        activity.getId(), activity.getSeriesId());
            }
            return;
        }

        Student student = attempt.getStudent();
        List<ActivityScoreRule> rules = ruleService.getEnabledRules(
                activity.getId(),
                ScoreRuleTrigger.MINIGAME_EXHAUSTED_ATTEMPTS);

        for (ActivityScoreRule rule : rules) {
            if (!isEligible(rule, student)) {
                continue;
            }

            BigDecimal points = applySignForFailure(rule, rule.getFailPoints() != null ? rule.getFailPoints() : BigDecimal.ZERO);
            if (points.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }

            LocalDateTime occurredAt = attempt.getSubmittedAt() != null ? attempt.getSubmittedAt() : LocalDateTime.now();
            Semester semester = semesterResolver.resolveSemester(activity, rule, occurredAt);

            scoreEntryService.upsertEntry(ScoreEntryCommand.builder()
                    .studentId(student.getId())
                    .activityId(activity.getId())
                    .ruleId(rule.getId())
                    .semesterId(semester.getId())
                    .scoreType(rule.getScoreType())
                    .sourceType(ScoreEntrySourceType.MINIGAME_ATTEMPT)
                    .sourceId(attempt.getId())
                    .points(points)
                    .reason("Minigame exhausted attempts without passing for activity: " + activity.getName())
                    .actor(actor)
                    .build());
        }
    }

    @Override
    @Transactional
    public void applyTaskOverdue(TaskAssignment assignment, User actor) {
        if (assignment == null || assignment.getTask() == null || assignment.getTask().getActivity() == null) {
            return;
        }

        Activity activity = assignment.getTask().getActivity();
        if (activity.getSeriesId() != null) {
            log.info("Skipping task overdue penalty for series activity {}", activity.getId());
            return;
        }

        Student student = assignment.getStudent();
        List<ActivityScoreRule> rules = ruleService.getEnabledRules(activity.getId(), ScoreRuleTrigger.TASK_OVERDUE);

        for (ActivityScoreRule rule : rules) {
            if (!isEligible(rule, student)) {
                continue;
            }

            BigDecimal points = applySignForFailure(rule, rule.getFailPoints() != null ? rule.getFailPoints() : BigDecimal.ZERO);
            if (points.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }

            Semester semester = semesterResolver.resolveSemester(activity, rule, assignment.getTask().getDeadline());
            scoreEntryService.upsertEntry(ScoreEntryCommand.builder()
                    .studentId(student.getId())
                    .activityId(activity.getId())
                    .ruleId(rule.getId())
                    .semesterId(semester.getId())
                    .scoreType(rule.getScoreType())
                    .sourceType(ScoreEntrySourceType.TASK_ASSIGNMENT)
                    .sourceId(assignment.getId())
                    .points(points)
                    .reason("Task overdue without submission for activity: " + activity.getName())
                    .actor(actor)
                    .build());
        }
    }

    @Override
    @Transactional
    public void applySubmissionGraded(TaskSubmission submission, User actor) {
        Activity activity = submission.getTask().getActivity();
        if (activity != null && activity.getSeriesId() != null) {
            log.info("Skipping individual submission points for activity {} belonging to series {}", activity.getId(),
                    activity.getSeriesId());
            return;
        }

        Student student = submission.getStudent();

        List<ActivityScoreRule> rules = ruleService.getEnabledRules(activity.getId(),
                ScoreRuleTrigger.SUBMISSION_GRADED);

        for (ActivityScoreRule rule : rules) {
            if (!isEligible(rule, student))
                continue;

            BigDecimal points;
            if (vn.campuslife.enumeration.SubmissionStatus.GRADED.equals(submission.getStatus())
                    && Boolean.TRUE.equals(submission.getIsCompleted())) {
                points = applySignForSuccess(rule, rule.getPoints());
            } else {
                points = applySignForFailure(rule, rule.getFailPoints());
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
        if (attempt.getStatus() != AttemptStatus.PASSED)
            return;

        Activity activity = attempt.getMiniGame().getActivity();
        if (activity != null && activity.getSeriesId() != null) {
            log.info("Skipping individual minigame points for activity {} belonging to series {}", activity.getId(),
                    activity.getSeriesId());
            return;
        }

        Student student = attempt.getStudent();

        List<ActivityScoreRule> rules = ruleService.getEnabledRules(activity.getId(), ScoreRuleTrigger.MINIGAME_PASSED);

        for (ActivityScoreRule rule : rules) {
            if (!isEligible(rule, student))
                continue;

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
        if (progress == null || progress.getStudent() == null || progress.getSeries() == null) {
            return;
        }

        ActivitySeries series = progress.getSeries();
        if (series.getMilestonePoints() == null || series.getMilestonePoints().isBlank()) {
            return;
        }

        Map<String, Integer> milestonePoints;
        try {
            milestonePoints = objectMapper.readValue(series.getMilestonePoints(),
                    new TypeReference<Map<String, Integer>>() {
                    });
        } catch (Exception e) {
            log.warn("Unable to parse milestonePoints for series {}: {}", series.getId(), e.getMessage());
            return;
        }

        BigDecimal highestMilestonePoints = BigDecimal.ZERO;
        for (Map.Entry<String, Integer> entry : milestonePoints.entrySet()) {
            try {
                int requiredCount = Integer.parseInt(entry.getKey());
                if (progress.getCompletedCount() >= requiredCount) {
                    BigDecimal candidate = BigDecimal.valueOf(entry.getValue());
                    if (candidate.compareTo(highestMilestonePoints) > 0) {
                        highestMilestonePoints = candidate;
                    }
                }
            } catch (NumberFormatException ex) {
                log.warn("Invalid series milestone key '{}' for series {}", entry.getKey(), series.getId());
            }
        }

        BigDecimal currentPoints = progress.getPointsEarned() != null ? progress.getPointsEarned() : BigDecimal.ZERO;
        if (highestMilestonePoints.compareTo(currentPoints) < 0) {
            log.info("Skip lowering milestone points for progress {} from {} to {}",
                    progress.getId(), currentPoints, highestMilestonePoints);
            return;
        }

        Semester semester = resolveSeriesSemester(series);
        if (semester == null) {
            log.warn("No semester resolved for series milestone progress {}", progress.getId());
            return;
        }

        scoreEntryService.upsertEntry(ScoreEntryCommand.builder()
                .studentId(progress.getStudent().getId())
                .semesterId(semester.getId())
                .scoreType(series.getScoreType())
                .sourceType(ScoreEntrySourceType.SERIES_PROGRESS)
                .sourceId(progress.getId())
                .points(highestMilestonePoints)
                .reason("Series milestone reached: " + series.getName() + " (" + progress.getCompletedCount()
                        + " activities)")
                .actor(actor)
                .build());

        if (highestMilestonePoints.compareTo(currentPoints) != 0) {
            progress.setPointsEarned(highestMilestonePoints);
            progress.setLastUpdated(LocalDateTime.now());
            progressRepository.save(progress);
        }
    }

    @Override
    @Transactional
    public void applySeriesMinimumRequirement(ActivitySeries series, Student student, int completedCount, User actor) {
        if (series == null || student == null || student.getId() == null || series.getId() == null) {
            return;
        }
        if (!series.isMinimumRequirementEnabled()
                || series.getMinimumRequiredEvents() == null
                || series.getMinimumRequiredEvents() <= 0
                || series.getMinimumPenaltyPoints() == null
                || series.getMinimumPenaltyPoints() <= 0) {
            return;
        }

        Semester semester = resolveSeriesSemester(series);
        if (semester == null) {
            log.warn("No semester resolved for series minimum requirement {}", series.getId());
            return;
        }

        boolean metRequirement = completedCount >= series.getMinimumRequiredEvents();
        BigDecimal points = metRequirement
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(series.getMinimumPenaltyPoints()).negate();

        scoreEntryService.upsertEntry(ScoreEntryCommand.builder()
                .studentId(student.getId())
                .semesterId(semester.getId())
                .scoreType(series.getScoreType())
                .sourceType(ScoreEntrySourceType.SERIES_MINIMUM_REQUIREMENT)
                .sourceId(series.getId())
                .points(points)
                .reason(metRequirement
                        ? "Series minimum requirement met: " + series.getName()
                        : "Series minimum requirement not met: " + series.getName()
                                + " (" + completedCount + "/" + series.getMinimumRequiredEvents() + " activities)")
                .actor(actor)
                .build());
    }

    private Semester resolveSeriesSemester(ActivitySeries series) {
        if (series.getTargetSemester() != null) {
            return series.getTargetSemester();
        }
        List<Activity> seriesActivities = activityRepository.findBySeriesIdAndIsDeletedFalse(series.getId());
        Activity firstActivity = seriesActivities.stream()
                .filter(activity -> activity.getStartDate() != null)
                .min(Comparator.comparing(Activity::getStartDate))
                .orElseGet(() -> series.getMainActivity() != null ? series.getMainActivity()
                        : (seriesActivities.isEmpty() ? null : seriesActivities.get(0)));

        if (firstActivity != null) {
            Semester semester = semesterHelperService.getSemesterForActivity(firstActivity);
            if (semester != null) {
                return semester;
            }
        }

        return semesterRepository.findAll().stream()
                .filter(Semester::isOpen)
                .findFirst()
                .orElseGet(() -> semesterRepository.findAll().stream().findFirst().orElse(null));
    }

    private boolean isEligible(ActivityScoreRule rule, Student student) {
        if (rule.getAudience() == ScoreRuleAudience.ALL_PARTICIPANTS)
            return true;

        boolean inDepartment = rule.getTargetDepartments().contains(student.getDepartment());

        if (rule.getAudience() == ScoreRuleAudience.DEPARTMENT_ONLY)
            return inDepartment;
        if (rule.getAudience() == ScoreRuleAudience.OUTSIDE_DEPARTMENTS_ONLY)
            return !inDepartment;

        return false;
    }

    /**
     * Dành cho nhánh SUCCESS (pass, cộng điểm).
     * Thông thường không negate. Trường hợp đặc biệt nếu có thể bổ sung sau.
     */
    private BigDecimal applySignForSuccess(ActivityScoreRule rule, BigDecimal value) {
        if (value == null) return BigDecimal.ZERO;
        return value;
    }

    /**
     * Dành cho nhánh FAILURE (fail / penalty).
     * - PENALTY_POINTS: luôn trừ điểm → negate
     * - PASS_FAIL_POINTS: fail cũng trừ điểm → negate
     * FE truyền số dương (e.g. 4.8), BE lưu -4.8 vào score_entries.
     */
    private BigDecimal applySignForFailure(ActivityScoreRule rule, BigDecimal value) {
        if (value == null) return BigDecimal.ZERO;
        boolean shouldNegate = rule.getCalculation() == ScoreRuleCalculation.PENALTY_POINTS
                || rule.getCalculation() == ScoreRuleCalculation.PASS_FAIL_POINTS;
        if (shouldNegate && value.compareTo(BigDecimal.ZERO) > 0) {
            return value.negate();
        }
        return value;
    }
}

