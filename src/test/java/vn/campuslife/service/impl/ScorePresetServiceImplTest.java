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
import vn.campuslife.model.score.ActivityScoreRuleRequest;

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
}
