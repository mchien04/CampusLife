package vn.campuslife.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.campuslife.entity.ActivityScoreRule;
import vn.campuslife.enumeration.ScoreRuleTrigger;

import java.util.List;

@Repository
public interface ActivityScoreRuleRepository extends JpaRepository<ActivityScoreRule, Long> {
    List<ActivityScoreRule> findByActivityIdAndEnabledTrue(Long activityId);
    List<ActivityScoreRule> findByActivityId(Long activityId);
    List<ActivityScoreRule> findByActivityIdAndTriggerTypeAndEnabledTrue(Long activityId, ScoreRuleTrigger triggerType);
    void deleteByActivityId(Long activityId);
}
