package vn.campuslife.service;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.campuslife.entity.ActivityParticipation;
import vn.campuslife.entity.ActivityRegistration;

public interface ActivityParticipationService extends JpaRepository<ActivityParticipation, Long> {

}