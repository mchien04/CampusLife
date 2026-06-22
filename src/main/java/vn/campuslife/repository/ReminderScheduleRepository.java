package vn.campuslife.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.campuslife.entity.ReminderSchedule;
import vn.campuslife.enumeration.ReminderCode;
import vn.campuslife.enumeration.ReminderStatus;
import vn.campuslife.enumeration.ReminderTargetType;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReminderScheduleRepository extends JpaRepository<ReminderSchedule, Long> {

    Optional<ReminderSchedule> findByUserIdAndTargetTypeAndTargetIdAndReminderCode(
            Long userId,
            ReminderTargetType targetType,
            Long targetId,
            ReminderCode reminderCode
    );

    List<ReminderSchedule> findByUserIdAndTargetTypeAndTargetIdAndStatusIn(
            Long userId,
            ReminderTargetType targetType,
            Long targetId,
            Collection<ReminderStatus> statuses
    );

    List<ReminderSchedule> findByTargetTypeAndTargetIdAndStatusIn(
            ReminderTargetType targetType,
            Long targetId,
            Collection<ReminderStatus> statuses
    );

    List<ReminderSchedule> findByStatus(ReminderStatus status);

    List<ReminderSchedule> findByStatusAndRemindAtAfter(ReminderStatus status, LocalDateTime remindAt);

    List<ReminderSchedule> findByStatusAndRemindAtLessThanEqual(ReminderStatus status, LocalDateTime remindAt);
}
