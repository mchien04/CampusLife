package vn.campuslife.service.impl;

import org.springframework.stereotype.Service;
import vn.campuslife.enumeration.ActivityPresetCode;
import vn.campuslife.enumeration.ActivityType;
import vn.campuslife.enumeration.ScoreRuleAudience;
import vn.campuslife.enumeration.ScoreRuleCalculation;
import vn.campuslife.enumeration.ScoreRuleTrigger;
import vn.campuslife.enumeration.ScoreSemesterPolicy;
import vn.campuslife.enumeration.ScoreType;
import vn.campuslife.enumeration.SeriesPresetCode;
import vn.campuslife.model.activity.ActivityPresetConfig;
import vn.campuslife.model.activity.ActivityPresetDefinitionResponse;
import vn.campuslife.model.activity.ActivityPresetPreviewRequest;
import vn.campuslife.model.activity.ActivityPresetPreviewResponse;
import vn.campuslife.model.activity.CreateActivityRequest;
import vn.campuslife.model.activity.StandardActivityCreateRequest;
import vn.campuslife.model.activity.StandardActivityUpdateRequest;
import vn.campuslife.model.activity.series.CreateSeriesRequest;
import vn.campuslife.model.activity.series.SeriesPresetConfig;
import vn.campuslife.model.activity.series.SeriesPresetDefinitionResponse;
import vn.campuslife.model.activity.series.SeriesPresetPreviewRequest;
import vn.campuslife.model.activity.series.SeriesPresetPreviewResponse;
import vn.campuslife.model.activity.series.UpdateSeriesRequest;
import vn.campuslife.model.activity.FieldDefinition;
import vn.campuslife.model.activity.PresetRuleDescriptor;
import vn.campuslife.model.score.ActivityScoreRuleRequest;
import vn.campuslife.service.ScorePresetService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ScorePresetServiceImpl implements ScorePresetService {

    @Override
    public List<ActivityPresetDefinitionResponse> getActivityPresetDefinitions() {
        List<ActivityPresetDefinitionResponse> presets = new ArrayList<>();
        presets.add(activityPreset(
                ActivityPresetCode.EVENT_BASIC,
                "Su kien thuong",
                "Su kien check-in/check-out va chot ket qua tham gia de cong diem hoac tru diem khi danh gia khong dat.",
                false,
                List.of(ActivityType.SUKIEN, ActivityType.CONG_TAC_XA_HOI),
                List.of(
                        "Mac dinh sinh rule tham gia hoan thanh.",
                        "Mac dinh bat NO_SHOW va tru cung score type chinh.",
                        "Co the dung failPoints de tru diem khi manager danh gia khong dat completion.")));
        presets.add(activityPreset(
                ActivityPresetCode.EVENT_WITH_SUBMISSION,
                "Su kien co bai nop",
                "Su kien yeu cau nop bai, cham pass/fail va co the tru diem khi qua han chua nop.",
                true,
                List.of(ActivityType.SUKIEN, ActivityType.CONG_TAC_XA_HOI, ActivityType.CHUYEN_DE_DOANH_NGHIEP),
                List.of(
                        "Mac dinh sinh rule cham bai pass/fail.",
                        "Mac dinh bat NO_SHOW; penalty no-show co the cau hinh rieng va khong lien quan TASK_OVERDUE.",
                        "Co the them rule TASK_OVERDUE de tru diem khi assignment qua han chua nop.")));
        presets.add(activityPreset(
                ActivityPresetCode.ENTERPRISE_SEMINAR_BASIC,
                "Chuyen de doanh nghiep",
                "Tich luy buoi chuyen de doanh nghiep theo moi lan tham gia.",
                false,
                List.of(ActivityType.CHUYEN_DE_DOANH_NGHIEP),
                List.of(
                        "Mac dinh cong 1 diem CHUYEN_DE cho moi lan tham gia hoan thanh.",
                        "Mac dinh tat NO_SHOW de tranh tru nguoc vao diem tich luy CHUYEN_DE.",
                        "Neu can tru diem ren luyen khi khong dat, co the dung failPoints tren cung rule.")));
        presets.add(activityPreset(
                ActivityPresetCode.ENTERPRISE_SEMINAR_WITH_BONUS,
                "Chuyen de co diem thuong",
                "Vua tich luy diem chuyen de, vua cong them diem thuong khac nhu ren luyen.",
                false,
                List.of(ActivityType.CHUYEN_DE_DOANH_NGHIEP),
                List.of(
                        "Sinh 2 rule: 1 rule CHUYEN_DE va 1 rule bonus.",
                        "Mac dinh tat NO_SHOW; neu bat thi nen tru sang score type khac nhu REN_LUYEN.",
                        "Dung khi su kien chuyen de vua tich luy so buoi, vua cong them diem thuong.")));
        presets.add(activityPreset(
                ActivityPresetCode.MINIGAME_PASS_ONLY,
                "Minigame vuot nguong",
                "Chi cong diem khi vuot nguong minigame. Diem chi lay tu lan PASS.",
                false,
                List.of(ActivityType.MINIGAME),
                List.of(
                        "Mac dinh sinh rule MINIGAME_PASSED.",
                        "Co the them rule MINIGAME_EXHAUSTED_ATTEMPTS de xu ly truong hop het luot ma van khong pass.",
                        "Khong tao rule SUBMISSION_GRADED hoac PARTICIPATION_COMPLETED cho minigame.")));
        presets.add(activityPreset(
                ActivityPresetCode.CUSTOM,
                "Tu tuy bien",
                "FE tu gui scoreRules thu cong khi can cau hinh dac biet.",
                null,
                List.of(ActivityType.values()),
                List.of("Khong tu dong sinh rule.")));
        return presets;
    }

    @Override
    public ActivityPresetPreviewResponse previewActivityPreset(ActivityPresetPreviewRequest request) {
        ActivityPresetCode presetCode = request != null && request.getPresetCode() != null
                ? request.getPresetCode()
                : ActivityPresetCode.CUSTOM;
        ActivityPresetConfig config = request != null ? request.getPresetConfig() : null;
        ActivityType activityType = request != null ? request.getType() : null;

        ActivityPresetPreviewResponse response = new ActivityPresetPreviewResponse();
        response.setPresetCode(presetCode);
        response.setActivityType(resolveActivityType(presetCode, activityType));
        response.setRequiresSubmission(
                resolveRequiresSubmission(presetCode, request != null ? request.getRequiresSubmission() : null));
        response.setScoreRules(
                buildActivityRules(presetCode, config, response.getActivityType(), response.isRequiresSubmission()));
        response.setNotes(
                buildActivityPresetNotes(presetCode, response.getActivityType(), response.isRequiresSubmission()));
        return response;
    }

    @Override
    public void applyActivityPreset(CreateActivityRequest request) {
        if (request == null || request.getPresetCode() == null
                || request.getPresetCode() == ActivityPresetCode.CUSTOM) {
            return;
        }

        // Policy A: Reject conflicting input
        if (request.getScoreRules() != null && !request.getScoreRules().isEmpty()) {
            throw new IllegalArgumentException(
                    "Cannot send custom scoreRules with preset " + request.getPresetCode() +
                    ". Use CUSTOM preset for manual rules.");
        }

        ActivityPresetPreviewRequest previewRequest = new ActivityPresetPreviewRequest();
        previewRequest.setPresetCode(request.getPresetCode());
        previewRequest.setType(request.getType());
        previewRequest.setRequiresSubmission(request.getRequiresSubmission());
        previewRequest.setPresetConfig(request.getPresetConfig());

        ActivityPresetPreviewResponse preview = previewActivityPreset(previewRequest);
        request.setType(preview.getActivityType());
        request.setRequiresSubmission(preview.isRequiresSubmission());
        // Mark rules as preset-generated
        preview.getScoreRules().forEach(r -> r.setIsPresetGenerated(true));
        request.setScoreRules(preview.getScoreRules());
    }

    @Override
    public void applyActivityPreset(StandardActivityCreateRequest request) {
        if (request == null || request.getPresetCode() == null
                || request.getPresetCode() == ActivityPresetCode.CUSTOM) {
            return;
        }

        // Policy A: Reject conflicting input
        if (request.getScoreRules() != null && !request.getScoreRules().isEmpty()) {
            throw new IllegalArgumentException(
                    "Cannot send custom scoreRules with preset " + request.getPresetCode() +
                    ". Use CUSTOM preset for manual rules.");
        }

        ActivityPresetPreviewRequest previewRequest = new ActivityPresetPreviewRequest();
        previewRequest.setPresetCode(request.getPresetCode());
        previewRequest.setType(request.getType());
        previewRequest.setRequiresSubmission(request.getRequiresSubmission());
        previewRequest.setPresetConfig(request.getPresetConfig());

        ActivityPresetPreviewResponse preview = previewActivityPreset(previewRequest);
        request.setType(preview.getActivityType());
        request.setRequiresSubmission(preview.isRequiresSubmission());
        // Mark rules as preset-generated
        preview.getScoreRules().forEach(r -> r.setIsPresetGenerated(true));
        request.setScoreRules(preview.getScoreRules());
    }

    @Override
    public void applyActivityPreset(StandardActivityUpdateRequest request) {
        applyActivityPreset(request, null);
    }

    @Override
    public void applyActivityPreset(StandardActivityUpdateRequest request, ActivityType effectiveType) {
        if (request == null || request.getPresetCode() == null
                || request.getPresetCode() == ActivityPresetCode.CUSTOM) {
            return;
        }

        // Policy A: Reject conflicting input
        if (request.getScoreRules() != null && !request.getScoreRules().isEmpty()) {
            throw new IllegalArgumentException(
                    "Cannot send custom scoreRules with preset " + request.getPresetCode() +
                    ". Use CUSTOM preset for manual rules.");
        }

        ActivityPresetPreviewRequest previewRequest = new ActivityPresetPreviewRequest();
        previewRequest.setPresetCode(request.getPresetCode());
        previewRequest.setType(effectiveType);
        previewRequest.setRequiresSubmission(request.getRequiresSubmission());
        previewRequest.setPresetConfig(request.getPresetConfig());

        ActivityPresetPreviewResponse preview = previewActivityPreset(previewRequest);
        // Do not update type on update request
        request.setRequiresSubmission(preview.isRequiresSubmission());
        // Mark rules as preset-generated
        preview.getScoreRules().forEach(r -> r.setIsPresetGenerated(true));
        request.setScoreRules(preview.getScoreRules());
    }

    @Override
    public List<SeriesPresetDefinitionResponse> getSeriesPresetDefinitions() {
        List<SeriesPresetDefinitionResponse> presets = new ArrayList<>();
        presets.add(seriesPreset(
                SeriesPresetCode.SERIES_MILESTONE_BASIC,
                "Series milestone co ban",
                "Dinh nghia cac moc hoan thanh va diem thuong tuong ung cho chuoi su kien.",
                List.of(
                        "Mac dinh scoreType la REN_LUYEN.",
                        "Phu hop cho chuoi su kien thong thuong can moc 3/5/7.")));
        presets.add(seriesPreset(
                SeriesPresetCode.ENTERPRISE_SERIES,
                "Series chuyen de doanh nghiep",
                "Chuoi chuyen de doanh nghiep tich luy theo so buoi hoan thanh.",
                List.of(
                        "Mac dinh scoreType la CHUYEN_DE.",
                        "Moc diem mac dinh tang dan theo so buoi da tham gia.")));
        presets.add(seriesPreset(
                SeriesPresetCode.CUSTOM,
                "Series tuy bien",
                "Tu cau hinh scoreType va milestonePoints.",
                List.of("Khong tu dong sinh moc milestone.")));
        return presets;
    }

    @Override
    public SeriesPresetPreviewResponse previewSeriesPreset(SeriesPresetPreviewRequest request) {
        SeriesPresetCode presetCode = request != null && request.getPresetCode() != null
                ? request.getPresetCode()
                : SeriesPresetCode.CUSTOM;
        SeriesPresetConfig config = request != null ? request.getPresetConfig() : null;

        SeriesPresetConfig defaults = getDefaultSeriesConfig(presetCode);
        SeriesPresetConfig merged = mergeSeriesConfig(config, defaults);

        SeriesPresetPreviewResponse response = new SeriesPresetPreviewResponse();
        response.setPresetCode(presetCode);
        response.setScoreType(merged.getPrimaryScoreType());
        response.setMilestonePoints(merged.getMilestonePoints());
        response.setMinimumRequirementEnabled(merged.getMinimumRequirementEnabled());
        response.setMinimumRequiredEvents(merged.getMinimumRequiredEvents());
        response.setMinimumPenaltyPoints(merged.getMinimumPenaltyPoints());
        response.setNotes(buildSeriesPresetNotes(presetCode));
        return response;
    }

    @Override
    public void applySeriesPreset(CreateSeriesRequest request) {
        if (request == null || request.getPresetCode() == null
                || request.getPresetCode() == SeriesPresetCode.CUSTOM) {
            return;
        }

        SeriesPresetPreviewRequest previewRequest = new SeriesPresetPreviewRequest();
        previewRequest.setPresetCode(request.getPresetCode());
        previewRequest.setPresetConfig(request.getPresetConfig());

        SeriesPresetPreviewResponse preview = previewSeriesPreset(previewRequest);

        if (request.getScoreType() == null && preview.getScoreType() != null) {
            request.setScoreType(preview.getScoreType());
        }
        if ((request.getMilestonePoints() == null || request.getMilestonePoints().isEmpty())
                && preview.getMilestonePoints() != null) {
            request.setMilestonePoints(new LinkedHashMap<>(preview.getMilestonePoints()));
        }
        if (request.getMinimumRequirementEnabled() == null && preview.getMinimumRequirementEnabled() != null) {
            request.setMinimumRequirementEnabled(preview.getMinimumRequirementEnabled());
        }
        if (request.getMinimumRequiredEvents() == null && preview.getMinimumRequiredEvents() != null) {
            request.setMinimumRequiredEvents(preview.getMinimumRequiredEvents());
        }
        if (request.getMinimumPenaltyPoints() == null && preview.getMinimumPenaltyPoints() != null) {
            request.setMinimumPenaltyPoints(preview.getMinimumPenaltyPoints());
        }
    }

    @Override
    public void applySeriesPreset(UpdateSeriesRequest request) {
        if (request == null || request.getPresetCode() == null
                || request.getPresetCode() == SeriesPresetCode.CUSTOM) {
            return;
        }

        SeriesPresetPreviewRequest previewRequest = new SeriesPresetPreviewRequest();
        previewRequest.setPresetCode(request.getPresetCode());
        previewRequest.setPresetConfig(request.getPresetConfig());

        SeriesPresetPreviewResponse preview = previewSeriesPreset(previewRequest);

        if (request.getScoreType() == null && preview.getScoreType() != null) {
            request.setScoreType(preview.getScoreType());
        }
        if ((request.getMilestonePoints() == null || request.getMilestonePoints().isEmpty())
                && preview.getMilestonePoints() != null) {
            request.setMilestonePoints(new LinkedHashMap<>(preview.getMilestonePoints()));
        }
        if (request.getMinimumRequirementEnabled() == null && preview.getMinimumRequirementEnabled() != null) {
            request.setMinimumRequirementEnabled(preview.getMinimumRequirementEnabled());
        }
        if (request.getMinimumRequiredEvents() == null && preview.getMinimumRequiredEvents() != null) {
            request.setMinimumRequiredEvents(preview.getMinimumRequiredEvents());
        }
        if (request.getMinimumPenaltyPoints() == null && preview.getMinimumPenaltyPoints() != null) {
            request.setMinimumPenaltyPoints(preview.getMinimumPenaltyPoints());
        }
    }

    private ActivityPresetDefinitionResponse activityPreset(
            ActivityPresetCode code,
            String displayName,
            String description,
            Boolean defaultRequiresSubmission,
            List<ActivityType> recommendedActivityTypes,
            List<String> notes) {
        ActivityPresetDefinitionResponse response = new ActivityPresetDefinitionResponse();
        response.setCode(code);
        response.setDisplayName(displayName);
        response.setDescription(description);
        response.setDefaultRequiresSubmission(defaultRequiresSubmission);
        response.setRecommendedActivityTypes(recommendedActivityTypes);
        response.setNotes(notes);

        ActivityPresetConfig defaults = getDefaultActivityConfig(code, recommendedActivityTypes.isEmpty() ? null : recommendedActivityTypes.get(0));
        response.setSupportedRules(buildSupportedRulesForActivity(code, defaults));

        return response;
    }

    private SeriesPresetDefinitionResponse seriesPreset(
            SeriesPresetCode code,
            String displayName,
            String description,
            List<String> notes) {
        SeriesPresetDefinitionResponse response = new SeriesPresetDefinitionResponse();
        response.setCode(code);
        response.setDisplayName(displayName);
        response.setDescription(description);
        response.setNotes(notes);

        SeriesPresetConfig defaults = getDefaultSeriesConfig(code);
        response.setSupportedRules(buildSupportedRulesForSeries(code, defaults));

        return response;
    }

    private ActivityType resolveActivityType(ActivityPresetCode presetCode, ActivityType requestedType) {
        if (requestedType != null) {
            return requestedType;
        }
        return switch (presetCode) {
            case MINIGAME_PASS_ONLY -> ActivityType.MINIGAME;
            case ENTERPRISE_SEMINAR_BASIC, ENTERPRISE_SEMINAR_WITH_BONUS -> ActivityType.CHUYEN_DE_DOANH_NGHIEP;
            default -> ActivityType.SUKIEN;
        };
    }

    private boolean resolveRequiresSubmission(ActivityPresetCode presetCode, Boolean requested) {
        if (presetCode == ActivityPresetCode.EVENT_WITH_SUBMISSION) {
            return true;
        }
        return Boolean.TRUE.equals(requested);
    }

    private List<ActivityScoreRuleRequest> buildActivityRules(
            ActivityPresetCode presetCode,
            ActivityPresetConfig config,
            ActivityType activityType,
            boolean requiresSubmission) {
        if (presetCode == ActivityPresetCode.CUSTOM) {
            return new ArrayList<>();
        }

        ActivityPresetConfig defaults = getDefaultActivityConfig(presetCode, activityType);
        ActivityPresetConfig merged = mergeActivityConfig(presetCode, config, defaults);

        List<ActivityScoreRuleRequest> rules = new ArrayList<>();
        ScoreType primaryScoreType = merged.getPrimaryScoreType();

        switch (presetCode) {
            case EVENT_BASIC -> rules.add(buildRule(
                    primaryScoreType,
                    ScoreRuleTrigger.PARTICIPATION_COMPLETED,
                    ScoreRuleCalculation.FIXED_POINTS,
                    merged.getParticipationPoints(),
                    merged.getParticipationFailPoints(),
                    merged));
            case EVENT_WITH_SUBMISSION -> {
                if (requiresSubmission) {
                    rules.add(buildRule(
                            primaryScoreType,
                            ScoreRuleTrigger.SUBMISSION_GRADED,
                            ScoreRuleCalculation.PASS_FAIL_POINTS,
                            merged.getSubmissionPassPoints(),
                            merged.getSubmissionFailPoints(),
                            merged));
                    if (merged.getTaskOverduePenaltyPoints().compareTo(BigDecimal.ZERO) != 0) {
                        rules.add(buildRule(
                                primaryScoreType,
                                ScoreRuleTrigger.TASK_OVERDUE,
                                ScoreRuleCalculation.PENALTY_POINTS,
                                BigDecimal.ZERO,
                                merged.getTaskOverduePenaltyPoints(),
                                merged));
                    }
                }
            }
            case ENTERPRISE_SEMINAR_BASIC -> rules.add(buildRule(
                    primaryScoreType,
                    ScoreRuleTrigger.PARTICIPATION_COMPLETED,
                    ScoreRuleCalculation.COUNT_COMPLETION,
                    merged.getParticipationPoints(),
                    merged.getParticipationFailPoints(),
                    merged));
            case ENTERPRISE_SEMINAR_WITH_BONUS -> {
                rules.add(buildRule(
                        primaryScoreType,
                        ScoreRuleTrigger.PARTICIPATION_COMPLETED,
                        ScoreRuleCalculation.COUNT_COMPLETION,
                        merged.getParticipationPoints(),
                        merged.getParticipationFailPoints(),
                        merged));
                if (merged.getBonusPoints().compareTo(BigDecimal.ZERO) != 0) {
                    rules.add(buildRule(
                            merged.getBonusScoreType(),
                            ScoreRuleTrigger.PARTICIPATION_COMPLETED,
                            ScoreRuleCalculation.FIXED_POINTS,
                            merged.getBonusPoints(),
                            BigDecimal.ZERO,
                            merged));
                }
            }
            case MINIGAME_PASS_ONLY -> {
                rules.add(buildRule(
                        primaryScoreType,
                        ScoreRuleTrigger.MINIGAME_PASSED,
                        ScoreRuleCalculation.FIXED_POINTS,
                        merged.getParticipationPoints(),
                        BigDecimal.ZERO,
                        merged));
                if (merged.getMinigameExhaustedPenaltyPoints().compareTo(BigDecimal.ZERO) != 0) {
                    rules.add(buildRule(
                            primaryScoreType,
                            ScoreRuleTrigger.MINIGAME_EXHAUSTED_ATTEMPTS,
                            ScoreRuleCalculation.PASS_FAIL_POINTS,
                            BigDecimal.ZERO,
                            merged.getMinigameExhaustedPenaltyPoints(),
                            merged));
                }
            }
            case CUSTOM -> {
                return new ArrayList<>();
            }
        }

        if (merged.getNoShowPenaltyEnabled()) {
            ScoreType penaltyScoreType = merged.getNoShowPenaltyScoreType() != null
                    ? merged.getNoShowPenaltyScoreType()
                    : primaryScoreType;
            rules.add(buildRule(
                    penaltyScoreType,
                    ScoreRuleTrigger.NO_SHOW,
                    ScoreRuleCalculation.PENALTY_POINTS,
                    BigDecimal.ZERO,
                    merged.getNoShowPenaltyPoints(),
                    merged));
        }
        return rules;
    }

    private List<String> buildActivityPresetNotes(ActivityPresetCode presetCode, ActivityType activityType,
            boolean requiresSubmission) {
        List<String> notes = new ArrayList<>();
        notes.add(
                "Preset chi sinh scoreRules de FE/BE thao tac nhanh hon; admin van co the chuyen sang custom khi can.");
        if (presetCode == ActivityPresetCode.EVENT_WITH_SUBMISSION && requiresSubmission) {
            notes.add("Penalty qua han chua nop duoc map qua trigger TASK_OVERDUE.");
        }
        if (hasDefaultNoShowEnabled(presetCode, activityType)) {
            notes.add("Preset nay mac dinh bat NO_SHOW, FE co the tat bang noShowPenaltyEnabled=false.");
        } else if (activityType != ActivityType.MINIGAME) {
            notes.add("Preset nay mac dinh tat NO_SHOW; neu bat, nen cau hinh ro score type va penalty points.");
        }
        if (activityType == ActivityType.MINIGAME) {
            notes.add("Minigame co the dung trigger MINIGAME_PASSED va MINIGAME_EXHAUSTED_ATTEMPTS.");
        }
        if (presetCode == ActivityPresetCode.ENTERPRISE_SEMINAR_BASIC
                || presetCode == ActivityPresetCode.ENTERPRISE_SEMINAR_WITH_BONUS) {
            notes.add(
                    "Diem CHUYEN_DE co the duoc cau hinh nhu diem tich luy so buoi thong qua rule tham gia hoan thanh.");
        }
        return notes;
    }

    private ScoreType defaultPrimaryScoreType(ActivityPresetCode presetCode, ActivityType activityType) {
        if (activityType == ActivityType.CHUYEN_DE_DOANH_NGHIEP
                || presetCode == ActivityPresetCode.ENTERPRISE_SEMINAR_BASIC
                || presetCode == ActivityPresetCode.ENTERPRISE_SEMINAR_WITH_BONUS) {
            return ScoreType.CHUYEN_DE;
        }
        return ScoreType.REN_LUYEN;
    }

    private ActivityScoreRuleRequest buildRule(
            ScoreType scoreType,
            ScoreRuleTrigger trigger,
            ScoreRuleCalculation calculation,
            BigDecimal points,
            BigDecimal failPoints,
            ActivityPresetConfig config) {
        ActivityScoreRuleRequest request = new ActivityScoreRuleRequest();
        request.setScoreType(scoreType);
        request.setTriggerType(trigger);
        request.setCalculation(calculation);
        request.setPoints(points);
        request.setFailPoints(failPoints);
        request.setAudience(resolveAudience(trigger, config));
        request.setSemesterPolicy(resolveSemesterPolicy(trigger, config));
        request.setExplicitSemesterId(resolveExplicitSemesterId(trigger, config));
        request.setDepartmentIds(resolveDepartmentIds(trigger, config));
        request.setEnabled(true);
        return request;
    }

    private ScoreRuleAudience resolveAudience(ScoreRuleTrigger trigger, ActivityPresetConfig config) {
        ScoreRuleAudience override = getTriggerAudienceOverride(trigger, config);
        return override != null ? override
                : (config.getAudience() != null ? config.getAudience() : ScoreRuleAudience.ALL_PARTICIPANTS);
    }

    private ScoreSemesterPolicy resolveSemesterPolicy(ScoreRuleTrigger trigger, ActivityPresetConfig config) {
        ScoreSemesterPolicy override = getTriggerSemesterPolicyOverride(trigger, config);
        return override != null ? override
                : (config.getSemesterPolicy() != null ? config.getSemesterPolicy() : ScoreSemesterPolicy.ACTIVITY_SEMESTER);
    }

    private Long resolveExplicitSemesterId(ScoreRuleTrigger trigger, ActivityPresetConfig config) {
        Long override = getTriggerExplicitSemesterId(trigger, config);
        return override != null ? override : config.getExplicitSemesterId();
    }

    private List<Long> resolveDepartmentIds(ScoreRuleTrigger trigger, ActivityPresetConfig config) {
        List<Long> override = getTriggerDepartmentIds(trigger, config);
        return override != null ? override : config.getDepartmentIds();
    }

    private ScoreRuleAudience getTriggerAudienceOverride(ScoreRuleTrigger trigger, ActivityPresetConfig config) {
        return switch (trigger) {
            case SUBMISSION_GRADED -> config.getSubmissionAudience();
            case PARTICIPATION_COMPLETED -> config.getParticipationAudience();
            case NO_SHOW -> config.getNoShowAudience();
            case TASK_OVERDUE -> config.getTaskOverdueAudience();
            case MINIGAME_PASSED -> config.getMinigamePassedAudience();
            case MINIGAME_EXHAUSTED_ATTEMPTS -> config.getMinigameExhaustedAudience();
            default -> null;
        };
    }

    private ScoreSemesterPolicy getTriggerSemesterPolicyOverride(ScoreRuleTrigger trigger, ActivityPresetConfig config) {
        return switch (trigger) {
            case SUBMISSION_GRADED -> config.getSubmissionSemesterPolicy();
            case PARTICIPATION_COMPLETED -> config.getParticipationSemesterPolicy();
            case NO_SHOW -> config.getNoShowSemesterPolicy();
            case TASK_OVERDUE -> config.getTaskOverdueSemesterPolicy();
            case MINIGAME_PASSED -> config.getMinigamePassedSemesterPolicy();
            case MINIGAME_EXHAUSTED_ATTEMPTS -> config.getMinigameExhaustedSemesterPolicy();
            default -> null;
        };
    }

    private Long getTriggerExplicitSemesterId(ScoreRuleTrigger trigger, ActivityPresetConfig config) {
        return switch (trigger) {
            case SUBMISSION_GRADED -> config.getSubmissionExplicitSemesterId();
            case PARTICIPATION_COMPLETED -> config.getParticipationExplicitSemesterId();
            case NO_SHOW -> config.getNoShowExplicitSemesterId();
            case TASK_OVERDUE -> config.getTaskOverdueExplicitSemesterId();
            case MINIGAME_PASSED -> config.getMinigamePassedExplicitSemesterId();
            case MINIGAME_EXHAUSTED_ATTEMPTS -> config.getMinigameExhaustedExplicitSemesterId();
            default -> null;
        };
    }

    private List<Long> getTriggerDepartmentIds(ScoreRuleTrigger trigger, ActivityPresetConfig config) {
        return switch (trigger) {
            case SUBMISSION_GRADED -> config.getSubmissionDepartmentIds();
            case PARTICIPATION_COMPLETED -> config.getParticipationDepartmentIds();
            case NO_SHOW -> config.getNoShowDepartmentIds();
            case TASK_OVERDUE -> config.getTaskOverdueDepartmentIds();
            case MINIGAME_PASSED -> config.getMinigamePassedDepartmentIds();
            case MINIGAME_EXHAUSTED_ATTEMPTS -> config.getMinigameExhaustedDepartmentIds();
            default -> null;
        };
    }

    private List<String> buildSeriesPresetNotes(SeriesPresetCode presetCode) {
        List<String> notes = new ArrayList<>();
        notes.add("Series preset hien tai resolve ve scoreType va milestonePoints de dung voi mo hinh series hien co.");
        if (presetCode == SeriesPresetCode.ENTERPRISE_SERIES) {
            notes.add("Phu hop khi chuoi chuyen de duoc tich luy theo so buoi hoan thanh.");
        }
        return notes;
    }

    private static final List<String> SCORE_TYPE_OPTIONS = List.of("REN_LUYEN", "CONG_TAC_XA_HOI", "CHUYEN_DE");

    private ActivityPresetConfig mergeActivityConfig(ActivityPresetCode presetCode, ActivityPresetConfig incoming, ActivityPresetConfig defaults) {
        ActivityPresetConfig merged = new ActivityPresetConfig();
        if (incoming == null) {
            copyActivityConfig(defaults, merged);
            return merged;
        }
        merged.setPrimaryScoreType(incoming.getPrimaryScoreType() != null ? incoming.getPrimaryScoreType() : defaults.getPrimaryScoreType());
        merged.setParticipationPoints(incoming.getParticipationPoints() != null ? incoming.getParticipationPoints() : defaults.getParticipationPoints());
        merged.setParticipationFailPoints(incoming.getParticipationFailPoints() != null ? incoming.getParticipationFailPoints() : defaults.getParticipationFailPoints());
        merged.setNoShowPenaltyEnabled(incoming.getNoShowPenaltyEnabled() != null ? incoming.getNoShowPenaltyEnabled() : defaults.getNoShowPenaltyEnabled());
        
        merged.setSubmissionPassPoints(incoming.getSubmissionPassPoints() != null ? incoming.getSubmissionPassPoints() : defaults.getSubmissionPassPoints());
        merged.setSubmissionFailPoints(incoming.getSubmissionFailPoints() != null ? incoming.getSubmissionFailPoints() : defaults.getSubmissionFailPoints());
        
        BigDecimal noShowPoints = incoming.getNoShowPenaltyPoints();
        if (noShowPoints == null) {
            if (presetCode == ActivityPresetCode.EVENT_BASIC
                    || presetCode == ActivityPresetCode.MINIGAME_PASS_ONLY) {
                noShowPoints = merged.getParticipationPoints();
            } else if (presetCode == ActivityPresetCode.EVENT_WITH_SUBMISSION) {
                noShowPoints = merged.getSubmissionPassPoints();
            } else {
                noShowPoints = defaults.getNoShowPenaltyPoints();
            }
        }
        merged.setNoShowPenaltyPoints(noShowPoints);
        
        merged.setNoShowPenaltyScoreType(incoming.getNoShowPenaltyScoreType() != null ? incoming.getNoShowPenaltyScoreType() : defaults.getNoShowPenaltyScoreType());
        merged.setTaskOverduePenaltyPoints(incoming.getTaskOverduePenaltyPoints() != null ? incoming.getTaskOverduePenaltyPoints() : defaults.getTaskOverduePenaltyPoints());
        merged.setMinigameExhaustedPenaltyPoints(incoming.getMinigameExhaustedPenaltyPoints() != null ? incoming.getMinigameExhaustedPenaltyPoints() : defaults.getMinigameExhaustedPenaltyPoints());
        merged.setBonusScoreType(incoming.getBonusScoreType() != null ? incoming.getBonusScoreType() : defaults.getBonusScoreType());
        merged.setBonusPoints(incoming.getBonusPoints() != null ? incoming.getBonusPoints() : defaults.getBonusPoints());
        merged.setAudience(incoming.getAudience() != null ? incoming.getAudience() : defaults.getAudience());
        merged.setSemesterPolicy(incoming.getSemesterPolicy() != null ? incoming.getSemesterPolicy() : defaults.getSemesterPolicy());
        merged.setExplicitSemesterId(incoming.getExplicitSemesterId() != null ? incoming.getExplicitSemesterId() : defaults.getExplicitSemesterId());
        merged.setDepartmentIds(incoming.getDepartmentIds() != null ? incoming.getDepartmentIds() : defaults.getDepartmentIds());

        // Per-rule audience overrides
        merged.setSubmissionAudience(incoming.getSubmissionAudience() != null ? incoming.getSubmissionAudience() : defaults.getSubmissionAudience());
        merged.setSubmissionSemesterPolicy(incoming.getSubmissionSemesterPolicy() != null ? incoming.getSubmissionSemesterPolicy() : defaults.getSubmissionSemesterPolicy());
        merged.setSubmissionExplicitSemesterId(incoming.getSubmissionExplicitSemesterId() != null ? incoming.getSubmissionExplicitSemesterId() : defaults.getSubmissionExplicitSemesterId());
        merged.setSubmissionDepartmentIds(incoming.getSubmissionDepartmentIds() != null ? incoming.getSubmissionDepartmentIds() : defaults.getSubmissionDepartmentIds());

        merged.setParticipationAudience(incoming.getParticipationAudience() != null ? incoming.getParticipationAudience() : defaults.getParticipationAudience());
        merged.setParticipationSemesterPolicy(incoming.getParticipationSemesterPolicy() != null ? incoming.getParticipationSemesterPolicy() : defaults.getParticipationSemesterPolicy());
        merged.setParticipationExplicitSemesterId(incoming.getParticipationExplicitSemesterId() != null ? incoming.getParticipationExplicitSemesterId() : defaults.getParticipationExplicitSemesterId());
        merged.setParticipationDepartmentIds(incoming.getParticipationDepartmentIds() != null ? incoming.getParticipationDepartmentIds() : defaults.getParticipationDepartmentIds());

        merged.setNoShowAudience(incoming.getNoShowAudience() != null ? incoming.getNoShowAudience() : defaults.getNoShowAudience());
        merged.setNoShowSemesterPolicy(incoming.getNoShowSemesterPolicy() != null ? incoming.getNoShowSemesterPolicy() : defaults.getNoShowSemesterPolicy());
        merged.setNoShowExplicitSemesterId(incoming.getNoShowExplicitSemesterId() != null ? incoming.getNoShowExplicitSemesterId() : defaults.getNoShowExplicitSemesterId());
        merged.setNoShowDepartmentIds(incoming.getNoShowDepartmentIds() != null ? incoming.getNoShowDepartmentIds() : defaults.getNoShowDepartmentIds());

        merged.setTaskOverdueAudience(incoming.getTaskOverdueAudience() != null ? incoming.getTaskOverdueAudience() : defaults.getTaskOverdueAudience());
        merged.setTaskOverdueSemesterPolicy(incoming.getTaskOverdueSemesterPolicy() != null ? incoming.getTaskOverdueSemesterPolicy() : defaults.getTaskOverdueSemesterPolicy());
        merged.setTaskOverdueExplicitSemesterId(incoming.getTaskOverdueExplicitSemesterId() != null ? incoming.getTaskOverdueExplicitSemesterId() : defaults.getTaskOverdueExplicitSemesterId());
        merged.setTaskOverdueDepartmentIds(incoming.getTaskOverdueDepartmentIds() != null ? incoming.getTaskOverdueDepartmentIds() : defaults.getTaskOverdueDepartmentIds());

        merged.setBonusAudience(incoming.getBonusAudience() != null ? incoming.getBonusAudience() : defaults.getBonusAudience());
        merged.setBonusSemesterPolicy(incoming.getBonusSemesterPolicy() != null ? incoming.getBonusSemesterPolicy() : defaults.getBonusSemesterPolicy());
        merged.setBonusExplicitSemesterId(incoming.getBonusExplicitSemesterId() != null ? incoming.getBonusExplicitSemesterId() : defaults.getBonusExplicitSemesterId());
        merged.setBonusDepartmentIds(incoming.getBonusDepartmentIds() != null ? incoming.getBonusDepartmentIds() : defaults.getBonusDepartmentIds());

        merged.setMinigamePassedAudience(incoming.getMinigamePassedAudience() != null ? incoming.getMinigamePassedAudience() : defaults.getMinigamePassedAudience());
        merged.setMinigamePassedSemesterPolicy(incoming.getMinigamePassedSemesterPolicy() != null ? incoming.getMinigamePassedSemesterPolicy() : defaults.getMinigamePassedSemesterPolicy());
        merged.setMinigamePassedExplicitSemesterId(incoming.getMinigamePassedExplicitSemesterId() != null ? incoming.getMinigamePassedExplicitSemesterId() : defaults.getMinigamePassedExplicitSemesterId());
        merged.setMinigamePassedDepartmentIds(incoming.getMinigamePassedDepartmentIds() != null ? incoming.getMinigamePassedDepartmentIds() : defaults.getMinigamePassedDepartmentIds());

        merged.setMinigameExhaustedAudience(incoming.getMinigameExhaustedAudience() != null ? incoming.getMinigameExhaustedAudience() : defaults.getMinigameExhaustedAudience());
        merged.setMinigameExhaustedSemesterPolicy(incoming.getMinigameExhaustedSemesterPolicy() != null ? incoming.getMinigameExhaustedSemesterPolicy() : defaults.getMinigameExhaustedSemesterPolicy());
        merged.setMinigameExhaustedExplicitSemesterId(incoming.getMinigameExhaustedExplicitSemesterId() != null ? incoming.getMinigameExhaustedExplicitSemesterId() : defaults.getMinigameExhaustedExplicitSemesterId());
        merged.setMinigameExhaustedDepartmentIds(incoming.getMinigameExhaustedDepartmentIds() != null ? incoming.getMinigameExhaustedDepartmentIds() : defaults.getMinigameExhaustedDepartmentIds());

        return merged;
    }

    private void copyActivityConfig(ActivityPresetConfig source, ActivityPresetConfig target) {
        if (source == null) return;
        target.setPrimaryScoreType(source.getPrimaryScoreType());
        target.setParticipationPoints(source.getParticipationPoints());
        target.setParticipationFailPoints(source.getParticipationFailPoints());
        target.setNoShowPenaltyEnabled(source.getNoShowPenaltyEnabled());
        target.setNoShowPenaltyPoints(source.getNoShowPenaltyPoints());
        target.setNoShowPenaltyScoreType(source.getNoShowPenaltyScoreType());
        target.setSubmissionPassPoints(source.getSubmissionPassPoints());
        target.setSubmissionFailPoints(source.getSubmissionFailPoints());
        target.setTaskOverduePenaltyPoints(source.getTaskOverduePenaltyPoints());
        target.setMinigameExhaustedPenaltyPoints(source.getMinigameExhaustedPenaltyPoints());
        target.setBonusScoreType(source.getBonusScoreType());
        target.setBonusPoints(source.getBonusPoints());
        target.setAudience(source.getAudience());
        target.setSemesterPolicy(source.getSemesterPolicy());
        target.setExplicitSemesterId(source.getExplicitSemesterId());
        target.setDepartmentIds(source.getDepartmentIds() != null ? new ArrayList<>(source.getDepartmentIds()) : new ArrayList<>());

        // Per-rule audience overrides
        target.setSubmissionAudience(source.getSubmissionAudience());
        target.setSubmissionSemesterPolicy(source.getSubmissionSemesterPolicy());
        target.setSubmissionExplicitSemesterId(source.getSubmissionExplicitSemesterId());
        target.setSubmissionDepartmentIds(source.getSubmissionDepartmentIds() != null ? new ArrayList<>(source.getSubmissionDepartmentIds()) : null);

        target.setParticipationAudience(source.getParticipationAudience());
        target.setParticipationSemesterPolicy(source.getParticipationSemesterPolicy());
        target.setParticipationExplicitSemesterId(source.getParticipationExplicitSemesterId());
        target.setParticipationDepartmentIds(source.getParticipationDepartmentIds() != null ? new ArrayList<>(source.getParticipationDepartmentIds()) : null);

        target.setNoShowAudience(source.getNoShowAudience());
        target.setNoShowSemesterPolicy(source.getNoShowSemesterPolicy());
        target.setNoShowExplicitSemesterId(source.getNoShowExplicitSemesterId());
        target.setNoShowDepartmentIds(source.getNoShowDepartmentIds() != null ? new ArrayList<>(source.getNoShowDepartmentIds()) : null);

        target.setTaskOverdueAudience(source.getTaskOverdueAudience());
        target.setTaskOverdueSemesterPolicy(source.getTaskOverdueSemesterPolicy());
        target.setTaskOverdueExplicitSemesterId(source.getTaskOverdueExplicitSemesterId());
        target.setTaskOverdueDepartmentIds(source.getTaskOverdueDepartmentIds() != null ? new ArrayList<>(source.getTaskOverdueDepartmentIds()) : null);

        target.setBonusAudience(source.getBonusAudience());
        target.setBonusSemesterPolicy(source.getBonusSemesterPolicy());
        target.setBonusExplicitSemesterId(source.getBonusExplicitSemesterId());
        target.setBonusDepartmentIds(source.getBonusDepartmentIds() != null ? new ArrayList<>(source.getBonusDepartmentIds()) : null);

        target.setMinigamePassedAudience(source.getMinigamePassedAudience());
        target.setMinigamePassedSemesterPolicy(source.getMinigamePassedSemesterPolicy());
        target.setMinigamePassedExplicitSemesterId(source.getMinigamePassedExplicitSemesterId());
        target.setMinigamePassedDepartmentIds(source.getMinigamePassedDepartmentIds() != null ? new ArrayList<>(source.getMinigamePassedDepartmentIds()) : null);

        target.setMinigameExhaustedAudience(source.getMinigameExhaustedAudience());
        target.setMinigameExhaustedSemesterPolicy(source.getMinigameExhaustedSemesterPolicy());
        target.setMinigameExhaustedExplicitSemesterId(source.getMinigameExhaustedExplicitSemesterId());
        target.setMinigameExhaustedDepartmentIds(source.getMinigameExhaustedDepartmentIds() != null ? new ArrayList<>(source.getMinigameExhaustedDepartmentIds()) : null);
    }

    private ActivityPresetConfig getDefaultActivityConfig(ActivityPresetCode presetCode, ActivityType activityType) {
        ActivityPresetConfig defaults = new ActivityPresetConfig();
        ActivityType resolvedType = resolveActivityType(presetCode, activityType);

        defaults.setPrimaryScoreType(defaultPrimaryScoreType(presetCode, resolvedType));
        defaults.setParticipationPoints(BigDecimal.valueOf(5));
        defaults.setParticipationFailPoints(BigDecimal.ZERO);
        defaults.setNoShowPenaltyEnabled(hasDefaultNoShowEnabled(presetCode, resolvedType));
        defaults.setNoShowPenaltyPoints(BigDecimal.valueOf(5));
        defaults.setNoShowPenaltyScoreType(null);
        defaults.setAudience(ScoreRuleAudience.ALL_PARTICIPANTS);
        defaults.setSemesterPolicy(ScoreSemesterPolicy.ACTIVITY_SEMESTER);
        defaults.setExplicitSemesterId(null);
        defaults.setDepartmentIds(new ArrayList<>());

        switch (presetCode) {
            case EVENT_BASIC -> {
            }
            case EVENT_WITH_SUBMISSION -> {
                defaults.setSubmissionPassPoints(BigDecimal.valueOf(5));
                defaults.setSubmissionFailPoints(BigDecimal.ZERO);
                defaults.setTaskOverduePenaltyPoints(BigDecimal.ZERO);
            }
            case ENTERPRISE_SEMINAR_BASIC -> {
                defaults.setParticipationPoints(BigDecimal.ONE);
                defaults.setNoShowPenaltyEnabled(false);
                defaults.setNoShowPenaltyScoreType(ScoreType.REN_LUYEN);
            }
            case ENTERPRISE_SEMINAR_WITH_BONUS -> {
                defaults.setParticipationPoints(BigDecimal.ONE);
                defaults.setBonusScoreType(ScoreType.REN_LUYEN);
                defaults.setBonusPoints(BigDecimal.valueOf(2));
                defaults.setNoShowPenaltyEnabled(false);
                defaults.setNoShowPenaltyScoreType(ScoreType.REN_LUYEN);
            }
            case MINIGAME_PASS_ONLY -> {
                defaults.setMinigameExhaustedPenaltyPoints(BigDecimal.ZERO);
            }
            case CUSTOM -> {
                defaults.setParticipationPoints(null);
                defaults.setParticipationFailPoints(null);
                defaults.setNoShowPenaltyEnabled(false);
                defaults.setNoShowPenaltyPoints(null);
            }
        }
        return defaults;
    }

    private boolean hasDefaultNoShowEnabled(ActivityPresetCode presetCode, ActivityType activityType) {
        if (activityType == ActivityType.MINIGAME) {
            return presetCode == ActivityPresetCode.MINIGAME_PASS_ONLY;
        }
        return presetCode == ActivityPresetCode.EVENT_BASIC
                || presetCode == ActivityPresetCode.EVENT_WITH_SUBMISSION;
    }

    private SeriesPresetConfig mergeSeriesConfig(SeriesPresetConfig incoming, SeriesPresetConfig defaults) {
        SeriesPresetConfig merged = new SeriesPresetConfig();
        if (incoming == null) {
            copySeriesConfig(defaults, merged);
            return merged;
        }
        merged.setPrimaryScoreType(incoming.getPrimaryScoreType() != null ? incoming.getPrimaryScoreType() : defaults.getPrimaryScoreType());
        merged.setMilestonePoints(incoming.getMilestonePoints() != null && !incoming.getMilestonePoints().isEmpty()
                ? new LinkedHashMap<>(incoming.getMilestonePoints())
                : new LinkedHashMap<>(defaults.getMilestonePoints()));
        merged.setMinimumRequirementEnabled(incoming.getMinimumRequirementEnabled() != null ? incoming.getMinimumRequirementEnabled() : defaults.getMinimumRequirementEnabled());
        merged.setMinimumRequiredEvents(incoming.getMinimumRequiredEvents() != null ? incoming.getMinimumRequiredEvents() : defaults.getMinimumRequiredEvents());
        merged.setMinimumPenaltyPoints(incoming.getMinimumPenaltyPoints() != null ? incoming.getMinimumPenaltyPoints() : defaults.getMinimumPenaltyPoints());
        merged.setAudience(incoming.getAudience() != null ? incoming.getAudience() : defaults.getAudience());
        merged.setDepartmentIds(incoming.getDepartmentIds() != null && !incoming.getDepartmentIds().isEmpty()
                ? incoming.getDepartmentIds()
                : defaults.getDepartmentIds());
        return merged;
    }

    private void copySeriesConfig(SeriesPresetConfig source, SeriesPresetConfig target) {
        if (source == null) return;
        target.setPrimaryScoreType(source.getPrimaryScoreType());
        target.setMilestonePoints(new LinkedHashMap<>(source.getMilestonePoints()));
        target.setMinimumRequirementEnabled(source.getMinimumRequirementEnabled());
        target.setMinimumRequiredEvents(source.getMinimumRequiredEvents());
        target.setMinimumPenaltyPoints(source.getMinimumPenaltyPoints());
        target.setAudience(source.getAudience());
        target.setDepartmentIds(source.getDepartmentIds() != null
                ? new ArrayList<>(source.getDepartmentIds())
                : new ArrayList<>());
    }

    private SeriesPresetConfig getDefaultSeriesConfig(SeriesPresetCode presetCode) {
        SeriesPresetConfig defaults = new SeriesPresetConfig();
        defaults.setPrimaryScoreType(ScoreType.REN_LUYEN);
        defaults.setAudience(ScoreRuleAudience.ALL_PARTICIPANTS);
        defaults.setDepartmentIds(new ArrayList<>());

        Map<Integer, Integer> milestones = new LinkedHashMap<>();
        switch (presetCode) {
            case SERIES_MILESTONE_BASIC -> {
                milestones.put(3, 5);
                milestones.put(5, 10);
                milestones.put(7, 15);
                defaults.setMinimumRequirementEnabled(false);
                defaults.setMinimumRequiredEvents(3);
                defaults.setMinimumPenaltyPoints(5);
            }
            case ENTERPRISE_SERIES -> {
                defaults.setPrimaryScoreType(ScoreType.CHUYEN_DE);
                milestones.put(1, 1);
                milestones.put(3, 3);
                milestones.put(5, 5);
                defaults.setMinimumRequirementEnabled(false);
                defaults.setMinimumRequiredEvents(3);
                defaults.setMinimumPenaltyPoints(5);
            }
            case CUSTOM -> {
                defaults.setMinimumRequirementEnabled(false);
                defaults.setMinimumRequiredEvents(0);
                defaults.setMinimumPenaltyPoints(0);
            }
        }
        defaults.setMilestonePoints(milestones);
        return defaults;
    }

    private List<PresetRuleDescriptor> buildSupportedRulesForActivity(ActivityPresetCode code, ActivityPresetConfig defaults) {
        if (code == ActivityPresetCode.CUSTOM) {
            return buildAllRuleDescriptors();
        }

        List<PresetRuleDescriptor> rules = new ArrayList<>();

        switch (code) {
            case EVENT_BASIC -> {
                rules.add(participationCompletedDescriptor(defaults).build());
                rules.add(buildNoShowDescriptor(defaults));
            }
            case EVENT_WITH_SUBMISSION -> {
                rules.add(submissionGradedDescriptor(defaults).build());
                rules.add(taskOverdueDescriptor(defaults).build());
                rules.add(buildNoShowDescriptor(defaults));
            }
            case ENTERPRISE_SEMINAR_BASIC -> {
                rules.add(participationCompletedDescriptor(defaults).build());
                rules.add(buildNoShowDescriptor(defaults));
            }
            case ENTERPRISE_SEMINAR_WITH_BONUS -> {
                rules.add(participationCompletedDescriptor(defaults).build());
                rules.add(bonusPointsDescriptor(defaults).build());
                rules.add(buildNoShowDescriptor(defaults));
            }
            case MINIGAME_PASS_ONLY -> {
                rules.add(minigamePassedDescriptor(defaults).build());
                rules.add(minigameExhaustedDescriptor(defaults).build());
            }
        }

        rules.add(buildActivityAudienceDescriptor(defaults));
        return rules;
    }

    private List<PresetRuleDescriptor> buildAllRuleDescriptors() {
        ActivityPresetConfig empty = new ActivityPresetConfig();
        empty.setPrimaryScoreType(ScoreType.REN_LUYEN);
        empty.setAudience(ScoreRuleAudience.ALL_PARTICIPANTS);
        empty.setSemesterPolicy(ScoreSemesterPolicy.ACTIVITY_SEMESTER);
        empty.setDepartmentIds(new ArrayList<>());
        empty.setParticipationPoints(BigDecimal.ZERO);
        empty.setParticipationFailPoints(BigDecimal.ZERO);
        empty.setSubmissionPassPoints(BigDecimal.ZERO);
        empty.setSubmissionFailPoints(BigDecimal.ZERO);
        empty.setTaskOverduePenaltyPoints(BigDecimal.ZERO);
        empty.setMinigameExhaustedPenaltyPoints(BigDecimal.ZERO);
        empty.setBonusScoreType(ScoreType.REN_LUYEN);
        empty.setBonusPoints(BigDecimal.ZERO);
        empty.setNoShowPenaltyEnabled(false);
        empty.setNoShowPenaltyPoints(BigDecimal.ZERO);

        List<PresetRuleDescriptor> rules = new ArrayList<>();
        rules.add(participationCompletedDescriptor(empty).suggestedCombinations(List.of(ScoreRuleTrigger.NO_SHOW)).build());
        rules.add(submissionGradedDescriptor(empty).suggestedCombinations(List.of(ScoreRuleTrigger.TASK_OVERDUE, ScoreRuleTrigger.NO_SHOW)).build());
        rules.add(taskOverdueDescriptor(empty).suggestedCombinations(List.of(ScoreRuleTrigger.SUBMISSION_GRADED, ScoreRuleTrigger.NO_SHOW)).build());
        rules.add(buildNoShowDescriptor(empty));
        rules.get(rules.size() - 1).setSuggestedCombinations(List.of(ScoreRuleTrigger.PARTICIPATION_COMPLETED, ScoreRuleTrigger.SUBMISSION_GRADED, ScoreRuleTrigger.TASK_OVERDUE, ScoreRuleTrigger.MINIGAME_PASSED));
        rules.add(minigamePassedDescriptor(empty).suggestedCombinations(List.of(ScoreRuleTrigger.MINIGAME_EXHAUSTED_ATTEMPTS, ScoreRuleTrigger.NO_SHOW)).build());
        rules.add(minigameExhaustedDescriptor(empty).suggestedCombinations(List.of(ScoreRuleTrigger.MINIGAME_PASSED)).build());
        rules.add(bonusPointsDescriptor(empty).suggestedCombinations(List.of(ScoreRuleTrigger.PARTICIPATION_COMPLETED)).build());
        rules.add(buildActivityAudienceDescriptor(empty));
        return rules;
    }

    private PresetRuleDescriptor.PresetRuleDescriptorBuilder participationCompletedDescriptor(ActivityPresetConfig defaults) {
        return PresetRuleDescriptor.builder()
                .ruleKey("PARTICIPATION_COMPLETED")
                .label("Cộng điểm hoàn thành")
                .description("Tự động cộng điểm cho sinh viên khi check-in/check-out thành công.")
                .required(true)
                .enabledByDefault(true)
                .fieldDefinitions(List.of(
                        FieldDefinition.builder()
                                .fieldName("primaryScoreType")
                                .label("Loại điểm chính")
                                .inputType("SELECT")
                                .required(true)
                                .defaultValue(defaults.getPrimaryScoreType().name())
                                .visibility("ALWAYS")
                                .options(SCORE_TYPE_OPTIONS)
                                .build(),
                        FieldDefinition.builder()
                                .fieldName("participationPoints")
                                .label("Điểm hoàn thành")
                                .inputType("NUMBER")
                                .required(true)
                                .defaultValue(defaults.getParticipationPoints())
                                .visibility("ALWAYS")
                                .build(),
                        FieldDefinition.builder()
                                .fieldName("participationFailPoints")
                                .label("Điểm trừ khi đánh giá không đạt (mặc định)")
                                .inputType("NUMBER")
                                .required(true)
                                .defaultValue(defaults.getParticipationFailPoints())
                                .visibility("ALWAYS")
                                .build()
                ))
                .suggestedCombinations(List.of(ScoreRuleTrigger.NO_SHOW));
    }

    private PresetRuleDescriptor.PresetRuleDescriptorBuilder submissionGradedDescriptor(ActivityPresetConfig defaults) {
        return PresetRuleDescriptor.builder()
                .ruleKey("SUBMISSION_GRADED")
                .label("Điểm chấm bài nộp")
                .description("Cộng điểm cho sinh viên khi bài nộp được đánh giá đạt (Pass).")
                .required(true)
                .enabledByDefault(true)
                .fieldDefinitions(List.of(
                        FieldDefinition.builder()
                                .fieldName("primaryScoreType")
                                .label("Loại điểm chính")
                                .inputType("SELECT")
                                .required(true)
                                .defaultValue(defaults.getPrimaryScoreType().name())
                                .visibility("ALWAYS")
                                .options(SCORE_TYPE_OPTIONS)
                                .build(),
                        FieldDefinition.builder()
                                .fieldName("submissionPassPoints")
                                .label("Điểm đạt (Pass)")
                                .inputType("NUMBER")
                                .required(true)
                                .defaultValue(defaults.getSubmissionPassPoints())
                                .visibility("ALWAYS")
                                .build(),
                        FieldDefinition.builder()
                                .fieldName("submissionFailPoints")
                                .label("Điểm không đạt (Fail)")
                                .inputType("NUMBER")
                                .required(true)
                                .defaultValue(defaults.getSubmissionFailPoints())
                                .visibility("ALWAYS")
                                .build()
                ))
                .suggestedCombinations(List.of(ScoreRuleTrigger.TASK_OVERDUE, ScoreRuleTrigger.NO_SHOW));
    }

    private PresetRuleDescriptor.PresetRuleDescriptorBuilder taskOverdueDescriptor(ActivityPresetConfig defaults) {
        return PresetRuleDescriptor.builder()
                .ruleKey("TASK_OVERDUE")
                .label("Phạt nộp trễ")
                .description("Trừ điểm khi sinh viên nộp bài sau thời hạn quy định.")
                .required(false)
                .enabledByDefault(false)
                .fieldDefinitions(List.of(
                        FieldDefinition.builder()
                                .fieldName("taskOverduePenaltyPoints")
                                .label("Điểm phạt nộp trễ")
                                .inputType("NUMBER")
                                .required(true)
                                .defaultValue(defaults.getTaskOverduePenaltyPoints())
                                .visibility("rule_enabled")
                                .build()
                ))
                .suggestedCombinations(List.of(ScoreRuleTrigger.SUBMISSION_GRADED, ScoreRuleTrigger.NO_SHOW));
    }

    private PresetRuleDescriptor.PresetRuleDescriptorBuilder bonusPointsDescriptor(ActivityPresetConfig defaults) {
        return PresetRuleDescriptor.builder()
                .ruleKey("BONUS_POINTS")
                .label("Cộng điểm thưởng")
                .description("Cộng thêm điểm thưởng loại khác (ví dụ: Rèn luyện) khi tham gia chuyên đề.")
                .required(false)
                .enabledByDefault(true)
                .fieldDefinitions(List.of(
                        FieldDefinition.builder()
                                .fieldName("bonusScoreType")
                                .label("Loại điểm thưởng")
                                .inputType("SELECT")
                                .required(true)
                                .defaultValue(defaults.getBonusScoreType().name())
                                .visibility("rule_enabled")
                                .options(SCORE_TYPE_OPTIONS)
                                .build(),
                        FieldDefinition.builder()
                                .fieldName("bonusPoints")
                                .label("Điểm thưởng cộng thêm")
                                .inputType("NUMBER")
                                .required(true)
                                .defaultValue(defaults.getBonusPoints())
                                .visibility("rule_enabled")
                                .build()
                ))
                .suggestedCombinations(List.of(ScoreRuleTrigger.PARTICIPATION_COMPLETED));
    }

    private PresetRuleDescriptor.PresetRuleDescriptorBuilder minigamePassedDescriptor(ActivityPresetConfig defaults) {
        return PresetRuleDescriptor.builder()
                .ruleKey("MINIGAME_PASSED")
                .label("Điểm hoàn thành Minigame")
                .description("Cộng điểm khi vượt qua Minigame đạt yêu cầu.")
                .required(true)
                .enabledByDefault(true)
                .fieldDefinitions(List.of(
                        FieldDefinition.builder()
                                .fieldName("primaryScoreType")
                                .label("Loại điểm chính")
                                .inputType("SELECT")
                                .required(true)
                                .defaultValue(defaults.getPrimaryScoreType().name())
                                .visibility("ALWAYS")
                                .options(SCORE_TYPE_OPTIONS)
                                .build(),
                        FieldDefinition.builder()
                                .fieldName("participationPoints")
                                .label("Điểm vượt qua")
                                .inputType("NUMBER")
                                .required(true)
                                .defaultValue(defaults.getParticipationPoints())
                                .visibility("ALWAYS")
                                .build()
                ))
                .suggestedCombinations(List.of(ScoreRuleTrigger.MINIGAME_EXHAUSTED_ATTEMPTS, ScoreRuleTrigger.NO_SHOW));
    }

    private PresetRuleDescriptor.PresetRuleDescriptorBuilder minigameExhaustedDescriptor(ActivityPresetConfig defaults) {
        return PresetRuleDescriptor.builder()
                .ruleKey("MINIGAME_EXHAUSTED_ATTEMPTS")
                .label("Phạt hết lượt chơi")
                .description("Trừ điểm khi dùng hết lượt chơi tối đa mà vẫn không vượt qua minigame.")
                .required(false)
                .enabledByDefault(false)
                .fieldDefinitions(List.of(
                        FieldDefinition.builder()
                                .fieldName("minigameExhaustedPenaltyPoints")
                                .label("Điểm phạt hết lượt")
                                .inputType("NUMBER")
                                .required(true)
                                .defaultValue(defaults.getMinigameExhaustedPenaltyPoints())
                                .visibility("rule_enabled")
                                .build()
                ))
                .suggestedCombinations(List.of(ScoreRuleTrigger.MINIGAME_PASSED));
    }

    private PresetRuleDescriptor buildActivityAudienceDescriptor(ActivityPresetConfig defaults) {
        return PresetRuleDescriptor.builder()
                .ruleKey("ACTIVITY_AUDIENCE")
                .label("Giới hạn đối tượng nhận điểm")
                .description("Kiểm soát việc student thuộc khoa nào sẽ được cộng/trừ điểm từ sự kiện này.")
                .required(false)
                .enabledByDefault(false)
                .fieldDefinitions(List.of(
                        FieldDefinition.builder()
                                .fieldName("audience")
                                .label("Đối tượng áp dụng")
                                .inputType("SELECT")
                                .required(true)
                                .defaultValue(defaults.getAudience() != null ? defaults.getAudience().name() : "ALL_PARTICIPANTS")
                                .visibility("ALWAYS")
                                .options(List.of("ALL_PARTICIPANTS", "DEPARTMENT_ONLY", "OUTSIDE_DEPARTMENTS_ONLY"))
                                .build(),
                        FieldDefinition.builder()
                                .fieldName("departmentIds")
                                .label("Danh sách Khoa")
                                .inputType("MULTI_SELECT")
                                .required(false)
                                .defaultValue(defaults.getDepartmentIds())
                                .visibility("audience_department_scoped")
                                .build(),
                        FieldDefinition.builder()
                                .fieldName("semesterPolicy")
                                .label("Học kỳ cộng điểm")
                                .inputType("SELECT")
                                .required(true)
                                .defaultValue(defaults.getSemesterPolicy() != null ? defaults.getSemesterPolicy().name() : "ACTIVITY_SEMESTER")
                                .visibility("ALWAYS")
                                .options(List.of("ACTIVITY_SEMESTER", "EXPLICIT_SEMESTER"))
                                .build(),
                        FieldDefinition.builder()
                                .fieldName("explicitSemesterId")
                                .label("Học kỳ chỉ định")
                                .inputType("SELECT")
                                .required(false)
                                .defaultValue(defaults.getExplicitSemesterId())
                                .visibility("semester_policy_explicit")
                                .build()
                ))
                .build();
    }

    private PresetRuleDescriptor buildNoShowDescriptor(ActivityPresetConfig defaults) {
        return PresetRuleDescriptor.builder()
                .ruleKey("NO_SHOW")
                .label("Phạt vắng mặt (No-show)")
                .description("Trừ điểm khi sinh viên đã đăng ký nhưng không đến tham gia sự kiện.")
                .required(false)
                .enabledByDefault(defaults.getNoShowPenaltyEnabled())
                .fieldDefinitions(List.of(
                        FieldDefinition.builder()
                                .fieldName("noShowPenaltyEnabled")
                                .label("Bật phạt vắng mặt")
                                .inputType("BOOLEAN")
                                .required(true)
                                .defaultValue(defaults.getNoShowPenaltyEnabled())
                                .visibility("ALWAYS")
                                .build(),
                        FieldDefinition.builder()
                                .fieldName("noShowPenaltyPoints")
                                .label("Số điểm phạt")
                                .inputType("NUMBER")
                                .required(true)
                                .defaultValue(defaults.getNoShowPenaltyPoints())
                                .visibility("rule_enabled")
                                .build(),
                        FieldDefinition.builder()
                                .fieldName("noShowPenaltyScoreType")
                                .label("Loại điểm phạt (để trống để mặc định theo Loại điểm chính)")
                                .inputType("SELECT")
                                .required(false)
                                .defaultValue(defaults.getNoShowPenaltyScoreType() != null ? defaults.getNoShowPenaltyScoreType().name() : null)
                                .visibility("rule_enabled")
                                .options(SCORE_TYPE_OPTIONS)
                                .build()
                ))
                .build();
    }

    private List<PresetRuleDescriptor> buildSupportedRulesForSeries(SeriesPresetCode code, SeriesPresetConfig defaults) {
        List<PresetRuleDescriptor> rules = new ArrayList<>();

        rules.add(PresetRuleDescriptor.builder()
                .ruleKey("MILESTONE_POINTS")
                .label("Điểm mốc tích luỹ (Milestones)")
                .description("Cộng điểm thưởng khi sinh viên đạt các mốc số lượng sự kiện con đã hoàn thành.")
                .required(true)
                .enabledByDefault(true)
                .fieldDefinitions(List.of(
                        FieldDefinition.builder()
                                .fieldName("primaryScoreType")
                                .label("Loại điểm mốc")
                                .inputType("SELECT")
                                .required(true)
                                .defaultValue(defaults.getPrimaryScoreType().name())
                                .visibility("ALWAYS")
                                .options(SCORE_TYPE_OPTIONS)
                                .build(),
                        FieldDefinition.builder()
                                .fieldName("milestonePoints")
                                .label("Cấu hình mốc điểm")
                                .inputType("MAP")
                                .required(true)
                                .defaultValue(defaults.getMilestonePoints())
                                .visibility("ALWAYS")
                                .build()
                ))
                .build());

        rules.add(PresetRuleDescriptor.builder()
                .ruleKey("MINIMUM_REQUIREMENT")
                .label("Yêu cầu tối thiểu")
                .description("Phạt điểm nếu đăng ký tham gia chuỗi nhưng không đạt số sự kiện tối thiểu.")
                .required(false)
                .enabledByDefault(defaults.getMinimumRequirementEnabled())
                .fieldDefinitions(List.of(
                        FieldDefinition.builder()
                                .fieldName("minimumRequirementEnabled")
                                .label("Bật yêu cầu tối thiểu")
                                .inputType("BOOLEAN")
                                .required(true)
                                .defaultValue(defaults.getMinimumRequirementEnabled())
                                .visibility("ALWAYS")
                                .build(),
                        FieldDefinition.builder()
                                .fieldName("minimumRequiredEvents")
                                .label("Số lượng sự kiện tối thiểu")
                                .inputType("NUMBER")
                                .required(true)
                                .defaultValue(defaults.getMinimumRequiredEvents())
                                .visibility("rule_enabled")
                                .build(),
                        FieldDefinition.builder()
                                .fieldName("minimumPenaltyPoints")
                                .label("Số điểm phạt")
                                .inputType("NUMBER")
                                .required(true)
                                .defaultValue(defaults.getMinimumPenaltyPoints())
                                .visibility("rule_enabled")
                                .build()
                ))
                .build());

        rules.add(PresetRuleDescriptor.builder()
                .ruleKey("SERIES_AUDIENCE")
                .label("Giới hạn đối tượng nhận điểm")
                .description("Kiểm soát việc student thuộc khoa nào sẽ được cộng/trừ điểm từ chuỗi này.")
                .required(false)
                .enabledByDefault(false)
                .fieldDefinitions(List.of(
                        FieldDefinition.builder()
                                .fieldName("audience")
                                .label("Đối tượng áp dụng")
                                .inputType("SELECT")
                                .required(true)
                                .defaultValue(defaults.getAudience() != null ? defaults.getAudience().name() : "ALL_PARTICIPANTS")
                                .visibility("ALWAYS")
                                .options(List.of("ALL_PARTICIPANTS", "DEPARTMENT_ONLY", "OUTSIDE_DEPARTMENTS_ONLY"))
                                .build(),
                        FieldDefinition.builder()
                                .fieldName("departmentIds")
                                .label("Danh sách Khoa")
                                .inputType("MULTI_SELECT")
                                .required(false)
                                .defaultValue(defaults.getDepartmentIds())
                                .visibility("audience_department_scoped")
                                .build()
                ))
                .build());

        return rules;
    }
}
