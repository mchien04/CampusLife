package vn.campuslife.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.campuslife.entity.*;
import vn.campuslife.enumeration.NotificationType;
import vn.campuslife.enumeration.PreparationTaskStatus;
import vn.campuslife.exception.*;
import vn.campuslife.model.TaskStatsRespone;
import vn.campuslife.model.preparation.*;
import vn.campuslife.repository.*;
import vn.campuslife.service.NotificationService;
import vn.campuslife.service.PreparationService;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PreparationServiceImpl implements PreparationService {

    private final ActivityRepository activityRepository;
    private final StudentRepository studentRepository;
    private final ActivityOrganizerRepository activityOrganizerRepository;
    private final PreparationTaskRepository preparationTaskRepository;
    private final PreparationTaskRepository taskRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public void togglePreparation(Long activityId, boolean enabled) {
        Activity activity = getActiveActivity(activityId);
        activity.setHasPreparation(enabled);
        activityRepository.save(activity);
    }

    @Override
    public TaskStatsRespone getStudentStats(Long studentId) {
        return taskRepository.getStatsByStudentId(studentId);
    }

    @Override
    @Transactional(readOnly = true)
    public PreparationDashboardDto getPreparationDashboard(Long activityId) {
        Activity activity = getActiveActivity(activityId);
        if (!activity.isHasPreparation()) {
            throw new FeatureNotEnabledException("Preparation feature is not enabled for this activity");
        }

        List<PreparationTaskDto> tasks = preparationTaskRepository.findByActivityIdOrderByDeadlineAscIdAsc(activityId)
                .stream()
                .map(this::toTaskDto)
                .toList();

        return new PreparationDashboardDto(activityId, true, tasks, null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> listMyPreparationActivityIds(String username) {
        Student student = studentRepository.findByUserUsernameAndIsDeletedFalse(username)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        return activityOrganizerRepository.findPreparationEnabledActivityIdsByStudentId(student.getId());
    }

    @Override
    @Transactional
    public PreparationTaskDto assignTask(CreatePreparationTaskRequest request) {
        if (request.getActivityId() == null) {
            throw new BadRequestException("Activity ID is required");
        }
        Activity activity = getActiveActivity(request.getActivityId());
        if (!activity.isHasPreparation()) {
            throw new FeatureNotEnabledException("Preparation feature is not enabled for this activity");
        }

        Student assignee = studentRepository.findByIdAndIsDeletedFalse(request.getOwnerId())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        boolean organizer = activityOrganizerRepository.existsByActivityIdAndStudentId(activity.getId(),
                assignee.getId());
        if (!organizer) {
            throw new BadRequestException("Assignee must be an organizer of this activity");
        }

        PreparationTask task = new PreparationTask();
        task.setActivity(activity);
        task.setOwner(assignee);
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setDeadline(request.getDeadline());
        task.setBudgetLimit(request.getBudgetLimit());
        task.setFinancial(Boolean.TRUE.equals(request.getIsFinancial()));
        task.setStatus(PreparationTaskStatus.PENDING);
        PreparationTask saved = preparationTaskRepository.save(task);

        return toTaskDto(saved);
    }

    @Override
    @Transactional
    public PreparationTaskDto updateMyTaskStatus(Long taskId, PreparationTaskStatus status, String username) {
        Student student = studentRepository.findByUserUsernameAndIsDeletedFalse(username)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        PreparationTask task = preparationTaskRepository.findByIdAndOwnerId(taskId, student.getId())
                .orElseThrow(() -> new ForbiddenException("You are not allowed to update this task"));

        task.setStatus(status);
        PreparationTask saved = preparationTaskRepository.save(task);
        return toTaskDto(saved);
    }

    @Override
    @Transactional
    public void addOrganizer(Long activityId, Long studentId) {
        Activity activity = getActiveActivity(activityId);
        Student student = studentRepository.findByIdAndIsDeletedFalse(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        if (activityOrganizerRepository.existsByActivityIdAndStudentId(activityId, studentId)) {
            throw new BadRequestException("Student is already an organizer of this activity");
        }

        ActivityOrganizer organizer = new ActivityOrganizer();
        organizer.setActivity(activity);
        organizer.setStudent(student);
        activityOrganizerRepository.save(organizer);

        if (student.getUser() != null) {
            String title = "Bạn đã được thêm vào BTC";
            String content = "Hoạt động: " + activity.getName();
            Map<String, Object> metadata = Map.of(
                    "activityId", activity.getId(),
                    "role", "ORGANIZER");
            notificationService.sendNotification(student.getUser().getId(), title, content, NotificationType.GENERAL,
                    null, metadata);
        }
    }

    @Override
    @Transactional
    public void removeOrganizer(Long activityId, Long studentId) {
        long deleted = activityOrganizerRepository.deleteByActivityIdAndStudentId(activityId, studentId);
        if (deleted == 0) {
            throw new ResourceNotFoundException("Organizer mapping not found");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrganizerDto> listOrganizers(Long activityId) {
        getActiveActivity(activityId);
        return activityOrganizerRepository.findByActivityId(activityId).stream()
                .map(ao -> new OrganizerDto(
                        ao.getStudent() != null ? ao.getStudent().getId() : null,
                        ao.getStudent() != null ? ao.getStudent().getFullName() : null))
                .toList();
    }

    private Activity getActiveActivity(Long activityId) {
        return activityRepository.findByIdAndIsDeletedFalse(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Activity not found"));
    }

    private PreparationTaskDto toTaskDto(PreparationTask task) {
        Student owner = task.getOwner();
        String ownerName = owner != null ? owner.getFullName() : null;
        Long activityId = task.getActivity() != null ? task.getActivity().getId() : null;
        Long ownerId = owner != null ? owner.getId() : null;
        return new PreparationTaskDto(
                task.getId(),
                activityId,
                ownerId,
                ownerName,
                task.getTitle(),
                task.getDescription(),
                task.getDeadline(),
                task.getBudgetLimit(),
                task.getAllocatedAmount(),
                task.isFinancial(),
                task.getStatus());
    }
}
