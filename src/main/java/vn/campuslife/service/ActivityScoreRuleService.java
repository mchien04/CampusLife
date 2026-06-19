package vn.campuslife.service;

import vn.campuslife.entity.ActivityScoreRule;
import vn.campuslife.enumeration.ScoreRuleTrigger;
import vn.campuslife.model.ActivityScoreRuleRequest;
import vn.campuslife.model.ActivityScoreRuleResponse;
import java.util.List;

public interface ActivityScoreRuleService {
    List<ActivityScoreRule> getEnabledRules(Long activityId, ScoreRuleTrigger trigger);
    void replaceRules(Long activityId, List<ActivityScoreRuleRequest> requests);
    List<ActivityScoreRuleResponse> getRuleResponses(Long activityId);
}
