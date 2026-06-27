package vn.campuslife.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vn.campuslife.enumeration.ActivityPresetCode;
import vn.campuslife.enumeration.ActivityType;
import vn.campuslife.enumeration.ScoreRuleTrigger;
import vn.campuslife.enumeration.ScoreType;
import vn.campuslife.model.activity.ActivityPresetConfig;
import vn.campuslife.model.activity.ActivityPresetPreviewRequest;
import vn.campuslife.model.activity.ActivityPresetPreviewResponse;
import vn.campuslife.model.activity.ActivityPresetDefinitionResponse;
import vn.campuslife.model.activity.PresetRuleDescriptor;
import vn.campuslife.model.activity.FieldDefinition;
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
}
