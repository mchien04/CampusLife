package vn.campuslife.service;



import vn.campuslife.entity.ActivityRating;
import vn.campuslife.model.Response;

import java.util.List;
import java.util.Optional;

public interface ActivityRatingService {
    List<ActivityRating> getActivityRatingByActivityId(Long activityId);
    Optional<ActivityRating> getActivityRatingByActivityIdAndStudent(Long activityId, Long studentId);

    Response createActivityRating(Long activityId, Long studentId, float rating, String comment);
    Response getActivityRatingStats(Long activityId);

}
