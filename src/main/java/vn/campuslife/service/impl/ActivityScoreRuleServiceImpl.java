package vn.campuslife.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.campuslife.entity.Activity;
import vn.campuslife.entity.ActivityScoreRule;
import vn.campuslife.entity.Department;
import vn.campuslife.enumeration.ActivityType;
import vn.campuslife.enumeration.ScoreRuleAudience;
import vn.campuslife.enumeration.ScoreSemesterPolicy;
import vn.campuslife.enumeration.ScoreRuleTrigger;
import vn.campuslife.exception.ResourceNotFoundException;
import vn.campuslife.model.score.ActivityScoreRuleRequest;
import vn.campuslife.model.score.ActivityScoreRuleResponse;
import vn.campuslife.repository.ActivityRepository;
import vn.campuslife.repository.ActivityScoreRuleRepository;
import vn.campuslife.repository.DepartmentRepository;
import vn.campuslife.repository.SemesterRepository;
import vn.campuslife.service.ActivityScoreRuleService;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ActivityScoreRuleServiceImpl implements ActivityScoreRuleService {

    private final ActivityScoreRuleRepository ruleRepository;
    private final ActivityRepository activityRepository;
    private final DepartmentRepository departmentRepository;
    private final SemesterRepository semesterRepository;

    @Override
    public List<ActivityScoreRule> getEnabledRules(Long activityId, ScoreRuleTrigger trigger) {
        return ruleRepository.findByActivityIdAndTriggerTypeAndEnabledTrue(activityId, trigger);
    }

    @Override
    @Transactional
    public void replaceRules(Long activityId, List<ActivityScoreRuleRequest> requests) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Activity not found"));
        ruleRepository.deleteByActivityId(activityId);

        if (requests != null) {
            for (ActivityScoreRuleRequest req : requests) {
                if (req != null) {
                    if ((req.getTriggerType() == ScoreRuleTrigger.TASK_OVERDUE
                            || req.getTriggerType() == ScoreRuleTrigger.MINIGAME_EXHAUSTED_ATTEMPTS
                            || req.getTriggerType() == ScoreRuleTrigger.NO_SHOW)
                            && req.getFailPoints() == null
                            && req.getPoints() != null) {
                        req.setFailPoints(req.getPoints());
                    }
                }
                validateRuleCompatibility(activity, req);
                ActivityScoreRule rule = new ActivityScoreRule();
                rule.setActivity(activity);
                rule.setScoreType(req.getScoreType());
                rule.setTriggerType(req.getTriggerType());
                rule.setCalculation(req.getCalculation());
                rule.setPoints(req.getPoints() != null ? req.getPoints() : java.math.BigDecimal.ZERO);
                rule.setFailPoints(req.getFailPoints() != null ? req.getFailPoints() : java.math.BigDecimal.ZERO);
                rule.setAudience(req.getAudience());
                rule.setSemesterPolicy(req.getSemesterPolicy());

                if (req.getExplicitSemesterId() != null) {
                    rule.setExplicitSemester(semesterRepository.findById(req.getExplicitSemesterId())
                            .orElseThrow(() -> new ResourceNotFoundException("Semester not found")));
                }

                if (req.getDepartmentIds() != null && !req.getDepartmentIds().isEmpty()) {
                    List<Department> depts = departmentRepository.findAllById(req.getDepartmentIds());
                    rule.setTargetDepartments(new LinkedHashSet<>(depts));
                }

                rule.setEnabled(req.getEnabled() != null ? req.getEnabled() : true);
                rule.setPresetGenerated(req.getIsPresetGenerated() != null ? req.getIsPresetGenerated() : false);
                ruleRepository.save(rule);
            }
        }
    }

    private void validateRuleCompatibility(Activity activity, ActivityScoreRuleRequest request) {
        if (request.getTriggerType() == null) {
            throw new IllegalArgumentException("Score rule triggerType is required");
        }
        if (request.getScoreType() == null) {
            throw new IllegalArgumentException("Score rule scoreType is required");
        }
        if (request.getAudience() == null) {
            throw new IllegalArgumentException("Score rule audience is required");
        }
        if (request.getSemesterPolicy() == null) {
            throw new IllegalArgumentException("Score rule semesterPolicy is required");
        }
        if (request.getSemesterPolicy() == ScoreSemesterPolicy.EXPLICIT_SEMESTER
                && request.getExplicitSemesterId() == null) {
            throw new IllegalArgumentException(
                    "explicitSemesterId is required when semesterPolicy is EXPLICIT_SEMESTER");
        }
        if ((request.getAudience() == ScoreRuleAudience.DEPARTMENT_ONLY
                || request.getAudience() == ScoreRuleAudience.OUTSIDE_DEPARTMENTS_ONLY)
                && (request.getDepartmentIds() == null || request.getDepartmentIds().isEmpty())) {
            throw new IllegalArgumentException("departmentIds are required for department-scoped rules");
        }
        if (!activity.isRequiresSubmission()
                && (request.getTriggerType() == ScoreRuleTrigger.SUBMISSION_GRADED
                        || request.getTriggerType() == ScoreRuleTrigger.TASK_OVERDUE)) {
            throw new IllegalArgumentException("Submission-based rules require activity.requiresSubmission = true");
        }
        if ((request.getTriggerType() == ScoreRuleTrigger.TASK_OVERDUE
                || request.getTriggerType() == ScoreRuleTrigger.MINIGAME_EXHAUSTED_ATTEMPTS
                || request.getTriggerType() == ScoreRuleTrigger.NO_SHOW)
                && request.getFailPoints() == null) {
            throw new IllegalArgumentException("Penalty-style rules must define failPoints");
        }
        if (activity.getType() == ActivityType.MINIGAME
                && request.getTriggerType() != ScoreRuleTrigger.MINIGAME_PASSED
                && request.getTriggerType() != ScoreRuleTrigger.MINIGAME_EXHAUSTED_ATTEMPTS) {
            throw new IllegalArgumentException(
                    "Minigame activity only supports MINIGAME_PASSED or MINIGAME_EXHAUSTED_ATTEMPTS rules");
        }
        if (activity.getType() != ActivityType.MINIGAME
                && (request.getTriggerType() == ScoreRuleTrigger.MINIGAME_PASSED
                        || request.getTriggerType() == ScoreRuleTrigger.MINIGAME_EXHAUSTED_ATTEMPTS)) {
            throw new IllegalArgumentException(
                    "MINIGAME_PASSED and MINIGAME_EXHAUSTED_ATTEMPTS rules are only valid for minigame activity");
        }
    }

    @Override
    public List<ActivityScoreRuleResponse> getRuleResponses(Long activityId) {
        return ruleRepository.findByActivityIdAndEnabledTrue(activityId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private ActivityScoreRuleResponse mapToResponse(ActivityScoreRule rule) {
        ActivityScoreRuleResponse res = new ActivityScoreRuleResponse();
        res.setId(rule.getId());
        res.setActivityId(rule.getActivity().getId());
        res.setScoreType(rule.getScoreType());
        res.setTriggerType(rule.getTriggerType());
        res.setCalculation(rule.getCalculation());
        res.setPoints(rule.getPoints());
        res.setFailPoints(rule.getFailPoints());
        res.setAudience(rule.getAudience());
        res.setSemesterPolicy(rule.getSemesterPolicy());
        if (rule.getExplicitSemester() != null) {
            res.setExplicitSemesterId(rule.getExplicitSemester().getId());
        }
        res.setTargetDepartmentIds(rule.getTargetDepartments().stream()
                .map(Department::getId)
                .collect(Collectors.toList()));
        res.setEnabled(rule.isEnabled());
        res.setIsPresetGenerated(rule.isPresetGenerated());
        return res;
    }
}
