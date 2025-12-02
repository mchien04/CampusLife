package vn.campuslife.service;

import org.springframework.stereotype.Service;
import vn.campuslife.entity.ActivityParticipation;
@Service
public interface ActivityParticipationService {
    ActivityParticipation getParticipation(Long studentId, Long activityId);


}