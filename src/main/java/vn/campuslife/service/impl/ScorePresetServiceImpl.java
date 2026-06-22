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
import vn.campuslife.model.activity.series.SeriesPresetConfig;
import vn.campuslife.model.activity.series.SeriesPresetDefinitionResponse;
import vn.campuslife.model.activity.series.SeriesPresetPreviewRequest;
import vn.campuslife.model.activity.series.SeriesPresetPreviewResponse;
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

        ActivityPresetPreviewRequest previewRequest = new ActivityPresetPreviewRequest();
        previewRequest.setPresetCode(request.getPresetCode());
        previewRequest.setType(request.getType());
        previewRequest.setRequiresSubmission(request.getRequiresSubmission());
        previewRequest.setPresetConfig(request.getPresetConfig());

        ActivityPresetPreviewResponse preview = previewActivityPreset(previewRequest);
        request.setType(preview.getActivityType());
        request.setRequiresSubmission(preview.isRequiresSubmission());
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

        SeriesPresetPreviewResponse response = new SeriesPresetPreviewResponse();
        response.setPresetCode(presetCode);
        response.setScoreType(resolveSeriesScoreType(presetCode, config));
        response.setMilestonePoints(resolveSeriesMilestones(presetCode, config));
        response.setMinimumRequirementEnabled(config != null ? config.getMinimumRequirementEnabled() : null);
        response.setMinimumRequiredEvents(config != null ? config.getMinimumRequiredEvents() : null);
        response.setMinimumPenaltyPoints(config != null ? config.getMinimumPenaltyPoints() : null);
        response.setNotes(buildSeriesPresetNotes(presetCode));
        return response;
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

        ScoreType primaryScoreType = config != null && config.getPrimaryScoreType() != null
                ? config.getPrimaryScoreType()
                : defaultPrimaryScoreType(presetCode, activityType);

        List<ActivityScoreRuleRequest> rules = new ArrayList<>();
        switch (presetCode) {
            case EVENT_BASIC -> rules.add(buildRule(
                    primaryScoreType,
                    ScoreRuleTrigger.PARTICIPATION_COMPLETED,
                    ScoreRuleCalculation.FIXED_POINTS,
                    valueOrDefault(config != null ? config.getParticipationPoints() : null, BigDecimal.valueOf(5)),
                    valueOrDefault(config != null ? config.getParticipationFailPoints() : null, BigDecimal.ZERO)));
            case EVENT_WITH_SUBMISSION -> {
                if (requiresSubmission) {
                    rules.add(buildRule(
                            primaryScoreType,
                            ScoreRuleTrigger.SUBMISSION_GRADED,
                            ScoreRuleCalculation.PASS_FAIL_POINTS,
                            valueOrDefault(config != null ? config.getSubmissionPassPoints() : null,
                                    BigDecimal.valueOf(5)),
                            valueOrDefault(config != null ? config.getSubmissionFailPoints() : null, BigDecimal.ZERO)));
                    BigDecimal overduePenalty = valueOrDefault(
                            config != null ? config.getTaskOverduePenaltyPoints() : null,
                            BigDecimal.ZERO);
                    if (overduePenalty.compareTo(BigDecimal.ZERO) != 0) {
                        rules.add(buildRule(
                                primaryScoreType,
                                ScoreRuleTrigger.TASK_OVERDUE,
                                ScoreRuleCalculation.PENALTY_POINTS,
                                BigDecimal.ZERO,
                                overduePenalty));
                    }
                }
            }
            case ENTERPRISE_SEMINAR_BASIC -> rules.add(buildRule(
                    primaryScoreType,
                    ScoreRuleTrigger.PARTICIPATION_COMPLETED,
                    ScoreRuleCalculation.COUNT_COMPLETION,
                    valueOrDefault(config != null ? config.getParticipationPoints() : null, BigDecimal.ONE),
                    valueOrDefault(config != null ? config.getParticipationFailPoints() : null, BigDecimal.ZERO)));
            case ENTERPRISE_SEMINAR_WITH_BONUS -> {
                rules.add(buildRule(
                        primaryScoreType,
                        ScoreRuleTrigger.PARTICIPATION_COMPLETED,
                        ScoreRuleCalculation.COUNT_COMPLETION,
                        valueOrDefault(config != null ? config.getParticipationPoints() : null, BigDecimal.ONE),
                        valueOrDefault(config != null ? config.getParticipationFailPoints() : null, BigDecimal.ZERO)));
                ScoreType bonusScoreType = config != null && config.getBonusScoreType() != null
                        ? config.getBonusScoreType()
                        : ScoreType.REN_LUYEN;
                BigDecimal bonusPoints = valueOrDefault(config != null ? config.getBonusPoints() : null,
                        BigDecimal.valueOf(2));
                if (bonusPoints.compareTo(BigDecimal.ZERO) != 0) {
                    rules.add(buildRule(
                            bonusScoreType,
                            ScoreRuleTrigger.PARTICIPATION_COMPLETED,
                            ScoreRuleCalculation.FIXED_POINTS,
                            bonusPoints,
                            BigDecimal.ZERO));
                }
            }
            case MINIGAME_PASS_ONLY -> rules.add(buildRule(
                    primaryScoreType,
                    ScoreRuleTrigger.MINIGAME_PASSED,
                    ScoreRuleCalculation.FIXED_POINTS,
                    valueOrDefault(config != null ? config.getParticipationPoints() : null, BigDecimal.valueOf(5)),
                    BigDecimal.ZERO));
            case CUSTOM -> {
                return new ArrayList<>();
            }
        }
        if (presetCode == ActivityPresetCode.MINIGAME_PASS_ONLY) {
            BigDecimal exhaustedPenalty = valueOrDefault(
                    config != null ? config.getMinigameExhaustedPenaltyPoints() : null,
                    BigDecimal.ZERO);
            if (exhaustedPenalty.compareTo(BigDecimal.ZERO) != 0) {
                rules.add(buildRule(
                        primaryScoreType,
                        ScoreRuleTrigger.MINIGAME_EXHAUSTED_ATTEMPTS,
                        ScoreRuleCalculation.PASS_FAIL_POINTS,
                        BigDecimal.ZERO,
                        exhaustedPenalty));
            }
        }
        if (shouldGenerateNoShowRule(presetCode, activityType, config)) {
            BigDecimal noShowPenalty = resolveNoShowPenaltyPoints(presetCode, config);
            rules.add(buildRule(
                    resolveNoShowPenaltyScoreType(presetCode, primaryScoreType, config),
                    ScoreRuleTrigger.NO_SHOW,
                    ScoreRuleCalculation.PENALTY_POINTS,
                    BigDecimal.ZERO,
                    noShowPenalty));
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
            BigDecimal failPoints) {
        ActivityScoreRuleRequest request = new ActivityScoreRuleRequest();
        request.setScoreType(scoreType);
        request.setTriggerType(trigger);
        request.setCalculation(calculation);
        request.setPoints(points);
        request.setFailPoints(failPoints);
        request.setAudience(ScoreRuleAudience.ALL_PARTICIPANTS);
        request.setSemesterPolicy(ScoreSemesterPolicy.CURRENT_OPEN_SEMESTER);
        request.setEnabled(true);
        return request;
    }

    private ScoreType resolveSeriesScoreType(SeriesPresetCode presetCode, SeriesPresetConfig config) {
        if (config != null && config.getPrimaryScoreType() != null) {
            return config.getPrimaryScoreType();
        }
        return switch (presetCode) {
            case ENTERPRISE_SERIES -> ScoreType.CHUYEN_DE;
            default -> ScoreType.REN_LUYEN;
        };
    }

    private Map<Integer, Integer> resolveSeriesMilestones(SeriesPresetCode presetCode, SeriesPresetConfig config) {
        if (config != null && config.getMilestonePoints() != null && !config.getMilestonePoints().isEmpty()) {
            return new LinkedHashMap<>(config.getMilestonePoints());
        }

        Map<Integer, Integer> milestones = new LinkedHashMap<>();
        switch (presetCode) {
            case SERIES_MILESTONE_BASIC -> {
                milestones.put(3, 5);
                milestones.put(5, 10);
                milestones.put(7, 15);
            }
            case ENTERPRISE_SERIES -> {
                milestones.put(1, 1);
                milestones.put(3, 3);
                milestones.put(5, 5);
            }
            case CUSTOM -> {
                return milestones;
            }
        }
        return milestones;
    }

    private List<String> buildSeriesPresetNotes(SeriesPresetCode presetCode) {
        List<String> notes = new ArrayList<>();
        notes.add("Series preset hien tai resolve ve scoreType va milestonePoints de dung voi mo hinh series hien co.");
        if (presetCode == SeriesPresetCode.ENTERPRISE_SERIES) {
            notes.add("Phu hop khi chuoi chuyen de duoc tich luy theo so buoi hoan thanh.");
        }
        return notes;
    }

    private BigDecimal valueOrDefault(BigDecimal value, BigDecimal defaultValue) {
        return value != null ? value : defaultValue;
    }

    private boolean shouldGenerateNoShowRule(ActivityPresetCode presetCode,
                                             ActivityType activityType,
                                             ActivityPresetConfig config) {
        if (activityType == ActivityType.MINIGAME) {
            return false;
        }
        if (config != null && config.getNoShowPenaltyEnabled() != null) {
            return Boolean.TRUE.equals(config.getNoShowPenaltyEnabled());
        }
        return hasDefaultNoShowEnabled(presetCode, activityType);
    }

    private boolean hasDefaultNoShowEnabled(ActivityPresetCode presetCode, ActivityType activityType) {
        if (activityType == ActivityType.MINIGAME) {
            return false;
        }
        return presetCode == ActivityPresetCode.EVENT_BASIC
                || presetCode == ActivityPresetCode.EVENT_WITH_SUBMISSION;
    }

    private BigDecimal resolveNoShowPenaltyPoints(ActivityPresetCode presetCode, ActivityPresetConfig config) {
        if (config != null && config.getNoShowPenaltyPoints() != null) {
            return config.getNoShowPenaltyPoints();
        }
        return switch (presetCode) {
            case EVENT_BASIC -> valueOrDefault(
                    config != null ? config.getParticipationPoints() : null,
                    BigDecimal.valueOf(5));
            case EVENT_WITH_SUBMISSION -> valueOrDefault(
                    config != null ? config.getSubmissionPassPoints() : null,
                    BigDecimal.valueOf(5));
            default -> BigDecimal.ZERO;
        };
    }

    private ScoreType resolveNoShowPenaltyScoreType(ActivityPresetCode presetCode,
                                                    ScoreType primaryScoreType,
                                                    ActivityPresetConfig config) {
        if (config != null && config.getNoShowPenaltyScoreType() != null) {
            return config.getNoShowPenaltyScoreType();
        }
        if (presetCode == ActivityPresetCode.ENTERPRISE_SEMINAR_BASIC
                || presetCode == ActivityPresetCode.ENTERPRISE_SEMINAR_WITH_BONUS) {
            return ScoreType.REN_LUYEN;
        }
        return primaryScoreType;
    }
}
