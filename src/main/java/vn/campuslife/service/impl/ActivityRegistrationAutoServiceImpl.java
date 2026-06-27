package vn.campuslife.service.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.campuslife.entity.Activity;
import vn.campuslife.entity.ActivityParticipation;
import vn.campuslife.entity.ActivityRegistration;
import vn.campuslife.entity.Department;
import vn.campuslife.entity.Student;
import vn.campuslife.enumeration.NotificationType;
import vn.campuslife.enumeration.ParticipationType;
import vn.campuslife.enumeration.RegistrationStatus;
import vn.campuslife.repository.ActivityParticipationRepository;
import vn.campuslife.repository.ActivityRegistrationRepository;
import vn.campuslife.repository.StudentRepository;
import vn.campuslife.service.ActivityRegistrationAutoService;
import vn.campuslife.service.NotificationService;
import vn.campuslife.util.NotificationMessageTemplate;
import vn.campuslife.util.TicketCodeUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Default implementation of {@link ActivityRegistrationAutoService}.
 *
 * <p>Logic moved verbatim from {@code ActivityServiceImpl#autoRegisterStudents},
 * so callers (Standard, Minigame, legacy Activity create/update, and the
 * shared publish endpoint) all share the same behavior:
 * <ul>
 *   <li>Skip when the activity is still a draft.</li>
 *   <li>When {@code isImportant = true}: register every active student.</li>
 *   <li>When {@code mandatoryForFacultyStudents = true}: register students of
 *       the activity's organizer departments.</li>
 *   <li>Skip students already registered (batch existence check).</li>
 *   <li>Create an initial {@link ActivityParticipation} per registration.</li>
 *   <li>Send a notification per student; notification failures never fail the
 *       whole operation.</li>
 * </ul>
 *
 * <p>Series child activities are deliberately NOT handled here — see
 * {@code ActivitySeriesServiceImpl#autoRegisterStudentsForNewActivityInSeries},
 * which registers students who already enrolled in any sibling activity of the
 * series regardless of the per-activity auto-register flags.
 */
@Service
@RequiredArgsConstructor
public class ActivityRegistrationAutoServiceImpl implements ActivityRegistrationAutoService {

    private static final Logger logger = LoggerFactory.getLogger(ActivityRegistrationAutoServiceImpl.class);

    private final StudentRepository studentRepository;
    private final ActivityRegistrationRepository activityRegistrationRepository;
    private final ActivityParticipationRepository activityParticipationRepository;
    private final NotificationService notificationService;
    private final NotificationMessageTemplate notificationMessageTemplate;

    @Override
    @Transactional
    public void autoRegisterStudents(Activity activity) {
        autoRegisterStudents(activity, activity.isImportant(), activity.isMandatoryForFacultyStudents(),
                activity.getOrganizers());
    }

    @Override
    @Transactional
    public void autoRegisterStudents(Activity activity, boolean isImportant, boolean mandatoryForFaculty,
            Set<Department> organizerDepartments) {
        try {
            if (activity.isDraft()) {
                logger.info("Skipping auto-registration for draft activity (id={}, name={}, isDraft={})",
                        activity.getId(), activity.getName(), activity.isDraft());
                return;
            }

            logger.debug(
                    "Checking auto-registration for published activity (id={}, name={}, isImportant={}, mandatoryForFaculty={})",
                    activity.getId(), activity.getName(), isImportant, mandatoryForFaculty);

            List<Student> studentsToRegister = new ArrayList<>();

            if (isImportant) {
                List<Student> allStudents = studentRepository.findByIsDeletedFalse();
                studentsToRegister.addAll(allStudents);
                logger.info("Auto-registering {} students for important activity: {}", allStudents.size(),
                        activity.getName());
            }

            if (mandatoryForFaculty
                    && organizerDepartments != null
                    && !organizerDepartments.isEmpty()) {
                List<Long> departmentIds = organizerDepartments.stream()
                        .map(Department::getId)
                        .collect(Collectors.toList());

                List<Student> facultyStudents = studentRepository.findByDepartmentIdInAndIsDeletedFalse(departmentIds);
                studentsToRegister.addAll(facultyStudents);
                logger.info("Auto-registering {} faculty students for mandatory activity: {}", facultyStudents.size(),
                        activity.getName());
            }

            if (studentsToRegister.isEmpty()) {
                return;
            }

            Set<Long> existingStudentIds = activityRegistrationRepository
                    .findStudentIdsByActivityId(activity.getId());

            List<ActivityRegistration> registrations = studentsToRegister.stream()
                    .distinct()
                    .filter(student -> !existingStudentIds.contains(student.getId()))
                    .map(student -> {
                        ActivityRegistration registration = new ActivityRegistration();
                        registration.setActivity(activity);
                        registration.setStudent(student);
                        registration.setStatus(RegistrationStatus.APPROVED);
                        registration.setRegisteredDate(LocalDateTime.now());
                        if (activity.getSeriesId() != null) {
                            registration.setSeriesId(activity.getSeriesId());
                        }
                        String code;
                        int attempts = 0;
                        do {
                            code = TicketCodeUtils.newTicketCode();
                            attempts++;
                        } while (activityRegistrationRepository.existsByTicketCode(code) && attempts < 5);
                        registration.setTicketCode(code);
                        return registration;
                    })
                    .collect(Collectors.toList());

            if (registrations.isEmpty()) {
                logger.info("All students already registered for activity: {}", activity.getName());
                return;
            }

            activityRegistrationRepository.saveAll(registrations);
            logger.info("Successfully auto-registered {} students for activity: {}", registrations.size(),
                    activity.getName());

            List<ActivityParticipation> participations = registrations.stream()
                    .map(reg -> {
                        ActivityParticipation participation = new ActivityParticipation();
                        participation.setRegistration(reg);
                        participation.setParticipationType(ParticipationType.REGISTERED);
                        participation.setPointsEarned(BigDecimal.ZERO);
                        participation.setDate(LocalDateTime.now());
                        return participation;
                    })
                    .collect(Collectors.toList());

            activityParticipationRepository.saveAll(participations);
            logger.info("Created {} initial participations for activity: {}", participations.size(),
                    activity.getName());

            try {
                String title;
                String content;
                if (isImportant) {
                    title = notificationMessageTemplate.autoRegisterImportantTitle();
                    content = notificationMessageTemplate.autoRegisterImportantContent(activity.getName());
                } else if (mandatoryForFaculty) {
                    title = notificationMessageTemplate.autoRegisterMandatoryTitle();
                    content = notificationMessageTemplate.autoRegisterMandatoryContent(activity.getName());
                } else {
                    title = notificationMessageTemplate.autoRegisterDefaultTitle();
                    content = notificationMessageTemplate.autoRegisterDefaultContent(activity.getName());
                }

                for (ActivityRegistration registration : registrations) {
                    try {
                        Long userId = registration.getStudent().getUser().getId();
                        Map<String, Object> metadata = new HashMap<>();
                        metadata.put("activityId", activity.getId());
                        metadata.put("activityName", activity.getName());
                        metadata.put("registrationId", registration.getId());
                        metadata.put("ticketCode", registration.getTicketCode());
                        metadata.put("isAutoRegistered", true);

                        notificationService.sendNotification(
                                userId,
                                title,
                                content,
                                NotificationType.ACTIVITY_REGISTRATION,
                                null,
                                metadata);
                    } catch (Exception e) {
                        logger.error(
                                "Failed to send auto-registration notification to user {} for activity {}: {}",
                                registration.getStudent().getUser().getId(), activity.getId(), e.getMessage());
                    }
                }
                logger.info("Sent auto-registration notifications to {} students for activity: {}",
                        registrations.size(), activity.getName());
            } catch (Exception e) {
                logger.error("Failed to send auto-registration notifications for activity {}: {}",
                        activity.getId(), e.getMessage(), e);
            }
        } catch (Exception e) {
            logger.error("Failed to auto-register students for activity {}: {}", activity.getId(), e.getMessage(), e);
        }
    }
}