package vn.campuslife.service;

import vn.campuslife.entity.Activity;
import vn.campuslife.entity.ActivityScoreRule;
import vn.campuslife.entity.Semester;
import java.time.LocalDateTime;

public interface ScoreSemesterResolver {
    Semester resolveSemester(Activity activity, ActivityScoreRule rule, LocalDateTime eventTime);
}
