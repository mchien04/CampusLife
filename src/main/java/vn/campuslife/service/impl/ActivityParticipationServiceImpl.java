package vn.campuslife.service.impl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.campuslife.entity.ActivityParticipation;
import vn.campuslife.repository.ActivityParticipationRepository;
import vn.campuslife.service.ActivityParticipationService;

@Service
@RequiredArgsConstructor
public class ActivityParticipationServiceImpl implements ActivityParticipationService {
    private final ActivityParticipationRepository participationRepository;


    public ActivityParticipation getParticipation(Long studentId, Long activityId) {
        return participationRepository.findStudentParticipationByActivity(studentId, activityId);
    }

}

