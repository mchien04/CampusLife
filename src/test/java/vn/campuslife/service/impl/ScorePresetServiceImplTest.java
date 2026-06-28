package vn.campuslife.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vn.campuslife.enumeration.ActivityPresetCode;
import vn.campuslife.enumeration.ActivityType;
import vn.campuslife.enumeration.ScoreRuleAudience;
import vn.campuslife.enumeration.ScoreRuleCalculation;
import vn.campuslife.enumeration.ScoreRuleTrigger;
import vn.campuslife.enumeration.ScoreSemesterPolicy;
import vn.campuslife.enumeration.ScoreType;
import vn.campuslife.model.activity.ActivityPresetConfig;
import vn.campuslife.model.activity.ActivityPresetPreviewRequest;
import vn.campuslife.model.activity.ActivityPresetPreviewResponse;
import vn.campuslife.model.activity.ActivityPresetDefinitionResponse;
import vn.campuslife.model.activity.PresetRuleDescriptor;
import vn.campuslife.model.activity.FieldDefinition;
import vn.campuslife.model.activity.ScoreRulePreviewRow;
import vn.campuslife.model.activity.series.SeriesPresetDefinitionResponse;
import vn.campuslife.enumeration.SeriesPresetCode;
import vn.campuslife.model.score.ActivityScoreRuleRequest;
import java.util.List;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ScorePresetServiceImplTest {

    private ScorePresetServiceImpl scorePresetService;

    @BeforeEach
    void setUp() {
        scorePresetService = new ScorePresetServiceImpl();
    }

    @Test
    void previewActivityPreset_EventBasic_DefaultsNoShowToEnabledWithPrimaryScoreType() {
        ActivityPresetConfig config = new ActivityPresetConfig();
        config.setPrimaryScoreType(ScoreType.CONG_TAC_XA_HOI);
        config.setParticipationPoints(BigDecimal.valueOf(7));

        ActivityPresetPreviewRequest request = new ActivityPresetPreviewRequest();
        request.setPresetCode(ActivityPresetCode.EVENT_BASIC);
        request.setType(ActivityType.CONG_TAC_XA_HOI);
        request.setPresetConfig(config);

        ActivityPresetPreviewResponse response = scorePresetService.previewActivityPreset(request);

        Optional<ActivityScoreRuleRequest> noShowRuleOpt = response.getScoreRules().stream()
                .filter(rule -> rule.getTriggerType() == ScoreRuleTrigger.NO_SHOW)
                .findFirst();

        assertTrue(noShowRuleOpt.isPresent());
        ActivityScoreRuleRequest noShowRule = noShowRuleOpt.get();
        assertEquals(ScoreType.CONG_TAC_XA_HOI, noShowRule.getScoreType());
        assertEquals(BigDecimal.ZERO, noShowRule.getPoints());
        assertEquals(BigDecimal.valueOf(7), noShowRule.getFailPoints());
    }

    @Test
    void previewActivityPreset_EventWithSubmission_ExplicitlyDisableNoShow_DoesNotGenerateRule() {
        ActivityPresetConfig config = new ActivityPresetConfig();
        config.setNoShowPenaltyEnabled(false);
        config.setSubmissionPassPoints(BigDecimal.valueOf(6));

        ActivityPresetPreviewRequest request = new ActivityPresetPreviewRequest();
        request.setPresetCode(ActivityPresetCode.EVENT_WITH_SUBMISSION);
        request.setType(ActivityType.SUKIEN);
        request.setPresetConfig(config);

        ActivityPresetPreviewResponse response = scorePresetService.previewActivityPreset(request);

        assertTrue(response.getScoreRules().stream()
                .noneMatch(rule -> rule.getTriggerType() == ScoreRuleTrigger.NO_SHOW));
    }

    @Test
    void previewActivityPreset_EnterpriseSeminar_DefaultsNoShowToDisabled() {
        ActivityPresetPreviewRequest request = new ActivityPresetPreviewRequest();
        request.setPresetCode(ActivityPresetCode.ENTERPRISE_SEMINAR_BASIC);
        request.setType(ActivityType.CHUYEN_DE_DOANH_NGHIEP);

        ActivityPresetPreviewResponse response = scorePresetService.previewActivityPreset(request);

        assertTrue(response.getScoreRules().stream()
                .noneMatch(rule -> rule.getTriggerType() == ScoreRuleTrigger.NO_SHOW));
    }

    @Test
    void previewActivityPreset_EnterpriseSeminar_WithNoShowOverride_UsesConfiguredScoreType() {
        ActivityPresetConfig config = new ActivityPresetConfig();
        config.setNoShowPenaltyEnabled(true);
        config.setNoShowPenaltyPoints(BigDecimal.valueOf(-3));
        config.setNoShowPenaltyScoreType(ScoreType.REN_LUYEN);

        ActivityPresetPreviewRequest request = new ActivityPresetPreviewRequest();
        request.setPresetCode(ActivityPresetCode.ENTERPRISE_SEMINAR_WITH_BONUS);
        request.setType(ActivityType.CHUYEN_DE_DOANH_NGHIEP);
        request.setPresetConfig(config);

        ActivityPresetPreviewResponse response = scorePresetService.previewActivityPreset(request);

        Optional<ActivityScoreRuleRequest> noShowRuleOpt = response.getScoreRules().stream()
                .filter(rule -> rule.getTriggerType() == ScoreRuleTrigger.NO_SHOW)
                .findFirst();

        assertTrue(noShowRuleOpt.isPresent());
        ActivityScoreRuleRequest noShowRule = noShowRuleOpt.get();
        assertEquals(ScoreType.REN_LUYEN, noShowRule.getScoreType());
        assertEquals(BigDecimal.valueOf(-3), noShowRule.getFailPoints());
    }

    @Test
    void getActivityPresetDefinitions_ReturnsValidRichDescriptors() {
        List<ActivityPresetDefinitionResponse> responses = scorePresetService.getActivityPresetDefinitions();
        assertNotNull(responses);
        assertFalse(responses.isEmpty());

        Optional<ActivityPresetDefinitionResponse> eventBasicOpt = responses.stream()
                .filter(p -> p.getCode() == ActivityPresetCode.EVENT_BASIC)
                .findFirst();

        assertTrue(eventBasicOpt.isPresent());
        ActivityPresetDefinitionResponse eventBasic = eventBasicOpt.get();
        assertFalse(eventBasic.getSupportedRules().isEmpty());

        PresetRuleDescriptor partRule = eventBasic.getSupportedRules().stream()
                .filter(r -> "PARTICIPATION_COMPLETED".equals(r.getRuleKey()))
                .findFirst()
                .orElse(null);

        assertNotNull(partRule);
        assertTrue(partRule.isRequired());
        assertEquals("Cộng điểm hoàn thành", partRule.getLabel());

        FieldDefinition primaryScoreTypeField = partRule.getFieldDefinitions().stream()
                .filter(f -> "primaryScoreType".equals(f.getFieldName()))
                .findFirst()
                .orElse(null);

        assertNotNull(primaryScoreTypeField);
        assertEquals("SELECT", primaryScoreTypeField.getInputType());
        assertEquals("REN_LUYEN", primaryScoreTypeField.getDefaultValue());
        assertNotNull(primaryScoreTypeField.getOptions());
        assertTrue(primaryScoreTypeField.getOptions().contains("REN_LUYEN"));
    }

    @Test
    void getSeriesPresetDefinitions_ReturnsValidRichDescriptors() {
        List<SeriesPresetDefinitionResponse> responses = scorePresetService.getSeriesPresetDefinitions();
        assertNotNull(responses);
        assertFalse(responses.isEmpty());

        Optional<SeriesPresetDefinitionResponse> milestoneBasicOpt = responses.stream()
                .filter(p -> p.getCode() == SeriesPresetCode.SERIES_MILESTONE_BASIC)
                .findFirst();

        assertTrue(milestoneBasicOpt.isPresent());
        SeriesPresetDefinitionResponse milestoneBasic = milestoneBasicOpt.get();
        assertEquals(3, milestoneBasic.getSupportedRules().size());

        PresetRuleDescriptor milestoneRule = milestoneBasic.getSupportedRules().stream()
                .filter(r -> "MILESTONE_POINTS".equals(r.getRuleKey()))
                .findFirst()
                .orElse(null);

        assertNotNull(milestoneRule);
        assertTrue(milestoneRule.isRequired());

        PresetRuleDescriptor minReqRule = milestoneBasic.getSupportedRules().stream()
                .filter(r -> "MINIMUM_REQUIREMENT".equals(r.getRuleKey()))
                .findFirst()
                .orElse(null);

        assertNotNull(minReqRule);
        assertFalse(minReqRule.isRequired());
        assertFalse(minReqRule.isEnabledByDefault());
    }
    @Test
    void previewActivityPreset_MultipleRules_HandlesEnterpriseSeminarWithBonus() {
        ActivityPresetConfig config = new ActivityPresetConfig();
        config.setAudience(ScoreRuleAudience.ALL_PARTICIPANTS);
        config.setSemesterPolicy(ScoreSemesterPolicy.ACTIVITY_SEMESTER);
        
        config.setBonusAudience(ScoreRuleAudience.DEPARTMENT_ONLY);
        config.setBonusDepartmentIds(List.of(1L, 2L));
        config.setBonusSemesterPolicy(ScoreSemesterPolicy.EXPLICIT_SEMESTER);
        config.setBonusExplicitSemesterId(99L);

        ActivityPresetPreviewRequest request = new ActivityPresetPreviewRequest();
        request.setPresetCode(ActivityPresetCode.ENTERPRISE_SEMINAR_WITH_BONUS);
        request.setType(ActivityType.CHUYEN_DE_DOANH_NGHIEP);
        request.setPresetConfig(config);

        ActivityPresetPreviewResponse response = scorePresetService.previewActivityPreset(request);

        List<ActivityScoreRuleRequest> rules = response.getScoreRules();
        assertEquals(2, rules.size());

        ActivityScoreRuleRequest partRule = rules.stream()
                .filter(r -> r.getScoreType() == ScoreType.CHUYEN_DE) // Default for ENTERPRISE_SEMINAR_BASIC
                .findFirst().orElseThrow();
        assertEquals(ScoreRuleAudience.ALL_PARTICIPANTS, partRule.getAudience());
        assertEquals(ScoreSemesterPolicy.ACTIVITY_SEMESTER, partRule.getSemesterPolicy());
        assertNull(partRule.getExplicitSemesterId());
        assertTrue(partRule.getDepartmentIds() == null || partRule.getDepartmentIds().isEmpty());

        ActivityScoreRuleRequest bonusRule = rules.stream()
                .filter(r -> r.getScoreType() == ScoreType.REN_LUYEN) // Default for ENTERPRISE_SEMINAR_WITH_BONUS
                .findFirst().orElseThrow();
        assertEquals(ScoreRuleAudience.DEPARTMENT_ONLY, bonusRule.getAudience());
        assertEquals(ScoreSemesterPolicy.EXPLICIT_SEMESTER, bonusRule.getSemesterPolicy());
        assertEquals(99L, bonusRule.getExplicitSemesterId());
        assertNotNull(bonusRule.getDepartmentIds());
        assertTrue(bonusRule.getDepartmentIds().containsAll(List.of(1L, 2L)));
    }

    @Test
    void previewActivityPreset_RuleIsolation_EnsuresDifferentAudiencePerRule() {
        ActivityPresetConfig config = new ActivityPresetConfig();
        config.setAudience(ScoreRuleAudience.ALL_PARTICIPANTS);
        config.setNoShowPenaltyEnabled(false); // disable to isolate the 2 rules
        
        // Participation: ALL_PARTICIPANTS (from top-level)
        
        // Submission: DEPARTMENT_ONLY
        config.setSubmissionAudience(ScoreRuleAudience.DEPARTMENT_ONLY);
        config.setSubmissionDepartmentIds(List.of(1L));
        
        // Task Overdue: OUTSIDE_DEPARTMENTS_ONLY
        config.setTaskOverdueAudience(ScoreRuleAudience.OUTSIDE_DEPARTMENTS_ONLY);
        config.setTaskOverdueDepartmentIds(List.of(2L));
        
        // Enable task overdue penalty
        config.setTaskOverduePenaltyPoints(BigDecimal.valueOf(-2));

        ActivityPresetPreviewRequest request = new ActivityPresetPreviewRequest();
        request.setPresetCode(ActivityPresetCode.EVENT_WITH_SUBMISSION);
        request.setType(ActivityType.SUKIEN);
        request.setPresetConfig(config);

        ActivityPresetPreviewResponse response = scorePresetService.previewActivityPreset(request);

        List<ActivityScoreRuleRequest> rules = response.getScoreRules();
        assertEquals(2, rules.size()); // SUBMISSION_GRADED, TASK_OVERDUE

        ActivityScoreRuleRequest subRule = rules.stream()
                .filter(r -> r.getTriggerType() == ScoreRuleTrigger.SUBMISSION_GRADED)
                .findFirst().orElseThrow();
        assertEquals(ScoreRuleAudience.DEPARTMENT_ONLY, subRule.getAudience());
        assertEquals(List.of(1L), subRule.getDepartmentIds());

        ActivityScoreRuleRequest overdueRule = rules.stream()
                .filter(r -> r.getTriggerType() == ScoreRuleTrigger.TASK_OVERDUE)
                .findFirst().orElseThrow();
        assertEquals(ScoreRuleAudience.OUTSIDE_DEPARTMENTS_ONLY, overdueRule.getAudience());
        assertEquals(List.of(2L), overdueRule.getDepartmentIds());
    }

    @Test
    void previewActivityPreset_BackwardCompatibility_FallbackToTopLevelConfig() {
        ActivityPresetConfig config = new ActivityPresetConfig();
        config.setAudience(ScoreRuleAudience.DEPARTMENT_ONLY);
        config.setDepartmentIds(List.of(5L, 6L));
        config.setSemesterPolicy(ScoreSemesterPolicy.EXPLICIT_SEMESTER);
        config.setExplicitSemesterId(88L);
        config.setNoShowPenaltyEnabled(false); // only test participation
        
        // DO NOT set any per-rule overrides

        ActivityPresetPreviewRequest request = new ActivityPresetPreviewRequest();
        request.setPresetCode(ActivityPresetCode.EVENT_BASIC);
        request.setType(ActivityType.SUKIEN);
        request.setPresetConfig(config);

        ActivityPresetPreviewResponse response = scorePresetService.previewActivityPreset(request);

        List<ActivityScoreRuleRequest> rules = response.getScoreRules();
        assertEquals(1, rules.size());

        ActivityScoreRuleRequest partRule = rules.get(0);
        assertEquals(ScoreRuleAudience.DEPARTMENT_ONLY, partRule.getAudience());
        assertEquals(List.of(5L, 6L), partRule.getDepartmentIds());
        assertEquals(ScoreSemesterPolicy.EXPLICIT_SEMESTER, partRule.getSemesterPolicy());
        assertEquals(88L, partRule.getExplicitSemesterId());
    }

    @Test
    void getActivityPresetDefinitions_NoActivityAudienceInAnyPreset() {
        List<ActivityPresetDefinitionResponse> responses = scorePresetService.getActivityPresetDefinitions();

        for (ActivityPresetDefinitionResponse preset : responses) {
            boolean hasAudience = preset.getSupportedRules().stream()
                    .anyMatch(r -> "ACTIVITY_AUDIENCE".equals(r.getRuleKey()));
            assertFalse(hasAudience, "Preset " + preset.getCode() + " should not contain ACTIVITY_AUDIENCE");
        }
    }

    @Test
    void getActivityPresetDefinitions_ParticipationFailPointsNotPresent() {
        List<ActivityPresetDefinitionResponse> responses = scorePresetService.getActivityPresetDefinitions();

        List<ActivityPresetCode> participationPresets = List.of(
                ActivityPresetCode.EVENT_BASIC,
                ActivityPresetCode.ENTERPRISE_SEMINAR_BASIC,
                ActivityPresetCode.ENTERPRISE_SEMINAR_WITH_BONUS,
                ActivityPresetCode.CUSTOM);

        for (ActivityPresetCode code : participationPresets) {
            ActivityPresetDefinitionResponse preset = responses.stream()
                    .filter(p -> p.getCode() == code)
                    .findFirst().orElseThrow();

            PresetRuleDescriptor partRule = preset.getSupportedRules().stream()
                    .filter(r -> "PARTICIPATION_COMPLETED".equals(r.getRuleKey()))
                    .findFirst().orElseThrow();

            boolean hasFailField = partRule.getFieldDefinitions().stream()
                    .anyMatch(f -> "participationFailPoints".equals(f.getFieldName())
                            || "participationFailScoreType".equals(f.getFieldName()));

            assertFalse(hasFailField,
                    "PARTICIPATION_COMPLETED should NOT have participationFailPoints or participationFailScoreType for preset " + code);
        }
    }

    @Test
    void getActivityPresetDefinitions_EnterpriseSeminarBasic_HasAllSupportedRules() {
        List<ActivityPresetDefinitionResponse> responses = scorePresetService.getActivityPresetDefinitions();

        ActivityPresetDefinitionResponse preset = responses.stream()
                .filter(p -> p.getCode() == ActivityPresetCode.ENTERPRISE_SEMINAR_BASIC)
                .findFirst().orElseThrow();

        List<String> ruleKeys = preset.getSupportedRules().stream()
                .map(PresetRuleDescriptor::getRuleKey)
                .toList();

        assertTrue(ruleKeys.contains("PARTICIPATION_COMPLETED"));
        assertTrue(ruleKeys.contains("SUBMISSION_GRADED"));
        assertTrue(ruleKeys.contains("TASK_OVERDUE"));
        assertTrue(ruleKeys.contains("NO_SHOW"));

        PresetRuleDescriptor subRule = preset.getSupportedRules().stream()
                .filter(r -> "SUBMISSION_GRADED".equals(r.getRuleKey()))
                .findFirst().orElseThrow();
        assertFalse(subRule.isRequired());
        assertFalse(subRule.isEnabledByDefault());

        FieldDefinition subFailField = subRule.getFieldDefinitions().stream()
                .filter(f -> "submissionFailPoints".equals(f.getFieldName()))
                .findFirst().orElseThrow();
        assertTrue(subFailField.isRequired());

        FieldDefinition subFailScoreTypeField = subRule.getFieldDefinitions().stream()
                .filter(f -> "submissionFailScoreType".equals(f.getFieldName()))
                .findFirst().orElseThrow();
        assertFalse(subFailScoreTypeField.isRequired());
    }

    @Test
    void getActivityPresetDefinitions_FailScoreTypesOnlyForEnterprise() {
        List<ActivityPresetDefinitionResponse> responses = scorePresetService.getActivityPresetDefinitions();

        // Enterprise presets SHOULD expose both submissionFailScoreType and taskOverduePenaltyScoreType
        for (ActivityPresetCode code : List.of(ActivityPresetCode.ENTERPRISE_SEMINAR_BASIC,
                ActivityPresetCode.ENTERPRISE_SEMINAR_WITH_BONUS)) {
            ActivityPresetDefinitionResponse preset = responses.stream()
                    .filter(p -> p.getCode() == code)
                    .findFirst().orElseThrow();
            PresetRuleDescriptor subRule = preset.getSupportedRules().stream()
                    .filter(r -> "SUBMISSION_GRADED".equals(r.getRuleKey()))
                    .findFirst().orElseThrow();
            assertTrue(subRule.getFieldDefinitions().stream()
                            .anyMatch(f -> "submissionFailScoreType".equals(f.getFieldName())),
                    "Enterprise preset " + code + " should expose submissionFailScoreType");

            PresetRuleDescriptor overdueRule = preset.getSupportedRules().stream()
                    .filter(r -> "TASK_OVERDUE".equals(r.getRuleKey()))
                    .findFirst().orElseThrow();
            assertTrue(overdueRule.getFieldDefinitions().stream()
                            .anyMatch(f -> "taskOverduePenaltyScoreType".equals(f.getFieldName())),
                    "Enterprise preset " + code + " should expose taskOverduePenaltyScoreType");
        }

        // Non-enterprise presets should NOT expose these fields
        for (ActivityPresetCode code : List.of(ActivityPresetCode.EVENT_WITH_SUBMISSION,
                ActivityPresetCode.CUSTOM)) {
            ActivityPresetDefinitionResponse preset = responses.stream()
                    .filter(p -> p.getCode() == code)
                    .findFirst().orElseThrow();
            PresetRuleDescriptor subRule = preset.getSupportedRules().stream()
                    .filter(r -> "SUBMISSION_GRADED".equals(r.getRuleKey()))
                    .findFirst().orElseThrow();
            assertFalse(subRule.getFieldDefinitions().stream()
                            .anyMatch(f -> "submissionFailScoreType".equals(f.getFieldName())),
                    "Non-enterprise preset " + code + " should NOT expose submissionFailScoreType");

            PresetRuleDescriptor overdueRule = preset.getSupportedRules().stream()
                    .filter(r -> "TASK_OVERDUE".equals(r.getRuleKey()))
                    .findFirst().orElseThrow();
            assertFalse(overdueRule.getFieldDefinitions().stream()
                            .anyMatch(f -> "taskOverduePenaltyScoreType".equals(f.getFieldName())),
                    "Non-enterprise preset " + code + " should NOT expose taskOverduePenaltyScoreType");
        }
    }

    @Test
    void getActivityPresetDefinitions_EnterpriseSeminarWithBonus_HasBonusRule() {
        List<ActivityPresetDefinitionResponse> responses = scorePresetService.getActivityPresetDefinitions();

        ActivityPresetDefinitionResponse preset = responses.stream()
                .filter(p -> p.getCode() == ActivityPresetCode.ENTERPRISE_SEMINAR_WITH_BONUS)
                .findFirst().orElseThrow();

        List<String> ruleKeys = preset.getSupportedRules().stream()
                .map(PresetRuleDescriptor::getRuleKey)
                .toList();

        assertTrue(ruleKeys.contains("PARTICIPATION_COMPLETED"));
        assertTrue(ruleKeys.contains("BONUS_POINTS"));
        assertTrue(ruleKeys.contains("SUBMISSION_GRADED"));
        assertTrue(ruleKeys.contains("TASK_OVERDUE"));
        assertTrue(ruleKeys.contains("NO_SHOW"));
    }

    @Test
    void previewActivityPreset_EnterpriseSeminar_SubmissionMode_ExcludesParticipation() {
        ActivityPresetConfig config = new ActivityPresetConfig();
        config.setSubmissionEnabled(true);
        config.setSubmissionPassPoints(BigDecimal.ONE);
        config.setSubmissionFailPoints(BigDecimal.valueOf(1));

        ActivityPresetPreviewRequest request = new ActivityPresetPreviewRequest();
        request.setPresetCode(ActivityPresetCode.ENTERPRISE_SEMINAR_BASIC);
        request.setType(ActivityType.CHUYEN_DE_DOANH_NGHIEP);
        request.setPresetConfig(config);

        ActivityPresetPreviewResponse response = scorePresetService.previewActivityPreset(request);

        assertTrue(response.isRequiresSubmission());

        List<ActivityScoreRuleRequest> rules = response.getScoreRules();
        assertTrue(rules.stream().anyMatch(r -> r.getTriggerType() == ScoreRuleTrigger.SUBMISSION_GRADED));
        assertTrue(rules.stream().noneMatch(r -> r.getTriggerType() == ScoreRuleTrigger.PARTICIPATION_COMPLETED));

        ActivityScoreRuleRequest subRule = rules.stream()
                .filter(r -> r.getTriggerType() == ScoreRuleTrigger.SUBMISSION_GRADED)
                .findFirst().orElseThrow();
        assertEquals(ScoreRuleCalculation.PASS_FAIL_POINTS, subRule.getCalculation());
        assertEquals(BigDecimal.ONE, subRule.getPoints());
        assertEquals(BigDecimal.valueOf(1), subRule.getFailPoints());
    }



    @Test
    void previewActivityPreset_EnterpriseSeminarSubmissionMode_PreviewRowsShowBothScoreTypes() {
        ActivityPresetConfig config = new ActivityPresetConfig();
        config.setSubmissionEnabled(true);
        config.setSubmissionPassPoints(BigDecimal.ONE);
        config.setSubmissionFailPoints(BigDecimal.valueOf(2));
        config.setSubmissionFailScoreType(ScoreType.REN_LUYEN);
        config.setTaskOverduePenaltyPoints(BigDecimal.valueOf(3));

        ActivityPresetPreviewRequest request = new ActivityPresetPreviewRequest();
        request.setPresetCode(ActivityPresetCode.ENTERPRISE_SEMINAR_BASIC);
        request.setType(ActivityType.CHUYEN_DE_DOANH_NGHIEP);
        request.setPresetConfig(config);

        ActivityPresetPreviewResponse response = scorePresetService.previewActivityPreset(request);
        List<ScoreRulePreviewRow> rows = response.getPreviewRows();

        assertFalse(rows.isEmpty());

        ScoreRulePreviewRow passRow = rows.stream()
                .filter(r -> "SUBMISSION_GRADED".equals(r.getTriggerType()) && "PASS".equals(r.getScenario()))
                .findFirst().orElseThrow();
        assertEquals("CHUYEN_DE", passRow.getScoreType());
        assertEquals(BigDecimal.ONE, passRow.getPoints());
        assertEquals("ALL_PARTICIPANTS", passRow.getAudience());
        assertEquals("ACTIVITY_SEMESTER", passRow.getSemester());

        ScoreRulePreviewRow failRow = rows.stream()
                .filter(r -> "SUBMISSION_GRADED".equals(r.getTriggerType()) && "FAIL".equals(r.getScenario()))
                .findFirst().orElseThrow();
        assertEquals("REN_LUYEN", failRow.getScoreType());
        assertEquals(BigDecimal.valueOf(-2), failRow.getPoints());

        ScoreRulePreviewRow overdueRow = rows.stream()
                .filter(r -> "TASK_OVERDUE".equals(r.getTriggerType()))
                .findFirst().orElseThrow();
        assertEquals("REN_LUYEN", overdueRow.getScoreType());
        assertEquals(BigDecimal.valueOf(-3), overdueRow.getPoints());
    }

    @Test
    void previewActivityPreset_EnterpriseSeminar_WithTaskOverduePenalty_UsesRenLuyenByDefault() {
        ActivityPresetConfig config = new ActivityPresetConfig();
        config.setSubmissionEnabled(true);
        config.setTaskOverduePenaltyPoints(BigDecimal.valueOf(3));

        ActivityPresetPreviewRequest request = new ActivityPresetPreviewRequest();
        request.setPresetCode(ActivityPresetCode.ENTERPRISE_SEMINAR_BASIC);
        request.setType(ActivityType.CHUYEN_DE_DOANH_NGHIEP);
        request.setPresetConfig(config);

        ActivityPresetPreviewResponse response = scorePresetService.previewActivityPreset(request);

        ActivityScoreRuleRequest overdueRule = response.getScoreRules().stream()
                .filter(r -> r.getTriggerType() == ScoreRuleTrigger.TASK_OVERDUE)
                .findFirst().orElseThrow();
        assertEquals(ScoreType.REN_LUYEN, overdueRule.getScoreType(),
                "Enterprise task overdue penalty should default to REN_LUYEN to avoid deducting CHUYEN_DE");
        assertEquals(BigDecimal.valueOf(3), overdueRule.getFailPoints());
    }

    @Test
    void previewActivityPreset_EnterpriseSeminar_ParticipationMode_ExcludesSubmission() {
        ActivityPresetConfig config = new ActivityPresetConfig();
        config.setSubmissionEnabled(false);

        ActivityPresetPreviewRequest request = new ActivityPresetPreviewRequest();
        request.setPresetCode(ActivityPresetCode.ENTERPRISE_SEMINAR_BASIC);
        request.setType(ActivityType.CHUYEN_DE_DOANH_NGHIEP);
        request.setPresetConfig(config);

        ActivityPresetPreviewResponse response = scorePresetService.previewActivityPreset(request);

        assertFalse(response.isRequiresSubmission());

        assertTrue(response.getScoreRules().stream()
                .anyMatch(r -> r.getTriggerType() == ScoreRuleTrigger.PARTICIPATION_COMPLETED));
        assertTrue(response.getScoreRules().stream()
                .noneMatch(r -> r.getTriggerType() == ScoreRuleTrigger.SUBMISSION_GRADED));
    }

    @Test
    void previewActivityPreset_EnterpriseSeminar_MapsSubmissionFailScoreTypeCorrectly() {
        ActivityPresetConfig config = new ActivityPresetConfig();
        config.setSubmissionEnabled(true);
        config.setSubmissionPassPoints(BigDecimal.ONE);
        config.setSubmissionFailPoints(BigDecimal.valueOf(1));
        config.setSubmissionFailScoreType(ScoreType.REN_LUYEN); // fail score type override!

        ActivityPresetPreviewRequest request = new ActivityPresetPreviewRequest();
        request.setPresetCode(ActivityPresetCode.ENTERPRISE_SEMINAR_BASIC);
        request.setType(ActivityType.CHUYEN_DE_DOANH_NGHIEP);
        request.setPresetConfig(config);

        ActivityPresetPreviewResponse response = scorePresetService.previewActivityPreset(request);

        List<ActivityScoreRuleRequest> rules = response.getScoreRules();
        assertTrue(rules.stream().noneMatch(r -> r.getTriggerType() == ScoreRuleTrigger.PARTICIPATION_COMPLETED),
                "PARTICIPATION_COMPLETED should be excluded in submission mode");

        ActivityScoreRuleRequest subRule = rules.stream()
                .filter(r -> r.getTriggerType() == ScoreRuleTrigger.SUBMISSION_GRADED)
                .findFirst().orElseThrow();
        assertEquals(ScoreType.CHUYEN_DE, subRule.getScoreType());
        assertEquals(BigDecimal.ONE, subRule.getPoints());
        assertEquals(ScoreType.REN_LUYEN, subRule.getFailScoreType());
    }

    @Test
    void previewActivityPreset_EnterpriseSeminar_SubmissionFailScoreTypeDefaultsToRenLuyen() {
        ActivityPresetConfig config = new ActivityPresetConfig();
        config.setSubmissionEnabled(true);

        ActivityPresetPreviewRequest request = new ActivityPresetPreviewRequest();
        request.setPresetCode(ActivityPresetCode.ENTERPRISE_SEMINAR_BASIC);
        request.setType(ActivityType.CHUYEN_DE_DOANH_NGHIEP);
        request.setPresetConfig(config);

        ActivityPresetPreviewResponse response = scorePresetService.previewActivityPreset(request);

        List<ActivityScoreRuleRequest> rules = response.getScoreRules();
        assertTrue(rules.stream().noneMatch(r -> r.getTriggerType() == ScoreRuleTrigger.PARTICIPATION_COMPLETED),
                "PARTICIPATION_COMPLETED should be excluded in submission mode");

        ActivityScoreRuleRequest subRule = rules.stream()
                .filter(r -> r.getTriggerType() == ScoreRuleTrigger.SUBMISSION_GRADED)
                .findFirst().orElseThrow();
        assertEquals(ScoreType.CHUYEN_DE, subRule.getScoreType());
        assertEquals(BigDecimal.ONE, subRule.getPoints(),
                "Enterprise submission pass should default to 1 CHUYEN_DE session");
        assertEquals(ScoreType.REN_LUYEN, subRule.getFailScoreType(),
                "Enterprise submission fail should default to REN_LUYEN to avoid deducting CHUYEN_DE sessions");
    }

    @Test
    void previewActivityPreset_EnterpriseSeminarWithBonus_SubmissionMode_BonusRuleUsesSubmissionGraded() {
        ActivityPresetConfig config = new ActivityPresetConfig();
        config.setSubmissionEnabled(true);
        config.setBonusPoints(BigDecimal.valueOf(3));
        config.setBonusScoreType(ScoreType.REN_LUYEN);

        ActivityPresetPreviewRequest request = new ActivityPresetPreviewRequest();
        request.setPresetCode(ActivityPresetCode.ENTERPRISE_SEMINAR_WITH_BONUS);
        request.setType(ActivityType.CHUYEN_DE_DOANH_NGHIEP);
        request.setPresetConfig(config);

        ActivityPresetPreviewResponse response = scorePresetService.previewActivityPreset(request);
        List<ActivityScoreRuleRequest> rules = response.getScoreRules();

        ActivityScoreRuleRequest bonusRule = rules.stream()
                .filter(r -> r.getScoreType() == ScoreType.REN_LUYEN)
                .findFirst().orElseThrow();
        assertEquals(ScoreRuleTrigger.SUBMISSION_GRADED, bonusRule.getTriggerType(),
                "Bonus rule trigger should match submission mode (SUBMISSION_GRADED)");
        assertEquals(BigDecimal.valueOf(3), bonusRule.getPoints());
    }

    @Test
    void previewActivityPreset_EnterpriseSeminarWithBonus_ParticipationMode_BonusRuleUsesParticipationCompleted() {
        ActivityPresetConfig config = new ActivityPresetConfig();
        config.setSubmissionEnabled(false);
        config.setBonusPoints(BigDecimal.valueOf(3));
        config.setBonusScoreType(ScoreType.REN_LUYEN);

        ActivityPresetPreviewRequest request = new ActivityPresetPreviewRequest();
        request.setPresetCode(ActivityPresetCode.ENTERPRISE_SEMINAR_WITH_BONUS);
        request.setType(ActivityType.CHUYEN_DE_DOANH_NGHIEP);
        request.setPresetConfig(config);

        ActivityPresetPreviewResponse response = scorePresetService.previewActivityPreset(request);
        List<ActivityScoreRuleRequest> rules = response.getScoreRules();

        ActivityScoreRuleRequest bonusRule = rules.stream()
                .filter(r -> r.getScoreType() == ScoreType.REN_LUYEN)
                .findFirst().orElseThrow();
        assertEquals(ScoreRuleTrigger.PARTICIPATION_COMPLETED, bonusRule.getTriggerType(),
                "Bonus rule trigger should match participation mode (PARTICIPATION_COMPLETED)");
        assertEquals(BigDecimal.valueOf(3), bonusRule.getPoints());
    }

    @Test
    void previewActivityPreset_EnterpriseSeminarWithBonus_SubmissionMode_PreviewRowsIncludeBonus() {
        ActivityPresetConfig config = new ActivityPresetConfig();
        config.setSubmissionEnabled(true);
        config.setSubmissionPassPoints(BigDecimal.ONE);
        config.setSubmissionFailPoints(BigDecimal.valueOf(2));
        config.setSubmissionFailScoreType(ScoreType.REN_LUYEN);
        config.setBonusScoreType(ScoreType.REN_LUYEN);
        config.setBonusPoints(BigDecimal.valueOf(3));

        ActivityPresetPreviewRequest request = new ActivityPresetPreviewRequest();
        request.setPresetCode(ActivityPresetCode.ENTERPRISE_SEMINAR_WITH_BONUS);
        request.setType(ActivityType.CHUYEN_DE_DOANH_NGHIEP);
        request.setPresetConfig(config);

        ActivityPresetPreviewResponse response = scorePresetService.previewActivityPreset(request);
        List<ScoreRulePreviewRow> rows = response.getPreviewRows();

        assertFalse(rows.isEmpty());

        long chuyenDeRows = rows.stream()
                .filter(r -> "CHUYEN_DE".equals(r.getScoreType()))
                .count();
        assertTrue(chuyenDeRows > 0, "Should have at least one CHUYEN_DE row");

        long bonusRows = rows.stream()
                .filter(r -> "REN_LUYEN".equals(r.getScoreType()))
                .count();
        assertTrue(bonusRows > 0, "Should have at least one REN_LUYEN row (bonus)");

        ScoreRulePreviewRow passRow = rows.stream()
                .filter(r -> "SUBMISSION_GRADED".equals(r.getTriggerType()) && "PASS".equals(r.getScenario()) && "CHUYEN_DE".equals(r.getScoreType()))
                .findFirst().orElseThrow(() -> new AssertionError("Missing PASS row with CHUYEN_DE"));
        assertEquals(BigDecimal.ONE, passRow.getPoints());

        ScoreRulePreviewRow bonusRow = rows.stream()
                .filter(r -> "SUBMISSION_GRADED".equals(r.getTriggerType()) && "BONUS".equals(r.getScenario()) && "REN_LUYEN".equals(r.getScoreType()))
                .findFirst().orElseThrow(() -> new AssertionError("Missing bonus row with REN_LUYEN"));
        assertEquals(BigDecimal.valueOf(3), bonusRow.getPoints());
    }

    @Test
    void previewActivityPreset_EnterpriseSeminarWithBonus_ParticipationMode_PreviewRowsIncludeBonus() {
        ActivityPresetConfig config = new ActivityPresetConfig();
        config.setSubmissionEnabled(false);
        config.setParticipationPoints(BigDecimal.ONE);
        config.setBonusScoreType(ScoreType.REN_LUYEN);
        config.setBonusPoints(BigDecimal.valueOf(2));

        ActivityPresetPreviewRequest request = new ActivityPresetPreviewRequest();
        request.setPresetCode(ActivityPresetCode.ENTERPRISE_SEMINAR_WITH_BONUS);
        request.setType(ActivityType.CHUYEN_DE_DOANH_NGHIEP);
        request.setPresetConfig(config);

        ActivityPresetPreviewResponse response = scorePresetService.previewActivityPreset(request);
        List<ScoreRulePreviewRow> rows = response.getPreviewRows();

        assertFalse(rows.isEmpty());

        long chuyenDeRows = rows.stream()
                .filter(r -> "CHUYEN_DE".equals(r.getScoreType()))
                .count();
        assertTrue(chuyenDeRows > 0, "Should have at least one CHUYEN_DE row");

        long bonusRows = rows.stream()
                .filter(r -> "REN_LUYEN".equals(r.getScoreType()))
                .count();
        assertTrue(bonusRows > 0, "Should have at least one REN_LUYEN row (bonus)");

        ScoreRulePreviewRow mainRow = rows.stream()
                .filter(r -> "PARTICIPATION_COMPLETED".equals(r.getTriggerType()) && "CHUYEN_DE".equals(r.getScoreType()))
                .findFirst().orElseThrow(() -> new AssertionError("Missing PARTICIPATION_COMPLETED row with CHUYEN_DE"));
        assertEquals(BigDecimal.ONE, mainRow.getPoints());

        ScoreRulePreviewRow bonusRow = rows.stream()
                .filter(r -> "PARTICIPATION_COMPLETED".equals(r.getTriggerType()) && "BONUS".equals(r.getScenario()) && "REN_LUYEN".equals(r.getScoreType()))
                .findFirst().orElseThrow(() -> new AssertionError("Missing bonus row with REN_LUYEN"));
        assertEquals(BigDecimal.valueOf(2), bonusRow.getPoints());
    }
}
