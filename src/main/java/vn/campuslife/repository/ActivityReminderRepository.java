package vn.campuslife.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.campuslife.entity.ActivityRegistration;
import vn.campuslife.entity.ActivityReminder;
import vn.campuslife.model.ActivityReminderRespone;

import java.util.List;

public interface ActivityReminderRepository  extends JpaRepository<ActivityReminder, Long> {
    List<ActivityReminder> findByRegistrationStudentId(Long studentId);
}
