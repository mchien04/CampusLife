package vn.campuslife.service.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.campuslife.entity.*;
import vn.campuslife.enumeration.NotificationType;
import vn.campuslife.enumeration.PreparationTaskMemberRole;
import vn.campuslife.enumeration.ExpenseStatus;
import vn.campuslife.enumeration.PreparationTaskStatus;
import vn.campuslife.enumeration.WorkloadWarningType;
import vn.campuslife.exception.*;
import vn.campuslife.model.TaskStatsRespone;
import vn.campuslife.model.preparation.*;
import vn.campuslife.repository.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import vn.campuslife.service.NotificationService;
import vn.campuslife.service.PreparationService;
import vn.campuslife.service.PreparationFinanceService;
import java.math.RoundingMode;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Comparator;
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
    private final PreparationTaskMemberRepository preparationTaskMemberRepository;
    private final NotificationService notificationService;
    private final ActivityBudgetRepository activityBudgetRepository;
    private final TaskAllocationRepository taskAllocationRepository;
    private final FundAdvanceRepository fundAdvanceRepository;
    private final ObjectMapper objectMapper;
    private final PreparationFinanceService financeService;

    @Override
    @Transactional
    public void togglePreparation(Long activityId, boolean enabled) {
        Activity activity = getActiveActivity(activityId);
        activity.setHasPreparation(enabled);
        activityRepository.save(activity);
    }

    // new
    @Override
    public TaskStatsRespone getStudentStats(Long studentId) {
        return taskRepository.getStatsByStudentId(studentId);
    }

    @Override
    @Transactional(readOnly = true)
    public PreparationTaskDto getTaskDetail(Long id) {
        PreparationTask task = preparationTaskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy công việc với ID: " + id));

        return this.toTaskDto(task);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MyPreparationTaskDto> getPreparationTasks(Long activityId, String username) {
        Activity activity = getActiveActivity(activityId);
        if (!activity.isHasPreparation()) {
            throw new FeatureNotEnabledException("Preparation feature is not enabled for this activity");
        }

        Student student = studentRepository.findByUserUsernameAndIsDeletedFalse(username)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        if (!activityOrganizerRepository.existsByActivityIdAndStudentId(activityId, student.getId())) {
            throw new ForbiddenException("Organizer permission required");
        }

        List<PreparationTaskMember> memberships = preparationTaskMemberRepository
                .findByStudentIdAndActivityIdOrderByTaskDeadlineAscIdAsc(student.getId(), activityId);

        java.util.Map<Long, PreparationTaskMemberRole> roleByTaskId = memberships.stream()
                .filter(m -> m.getTask() != null && m.getTask().getId() != null)
                .collect(java.util.stream.Collectors.toMap(
                        m -> m.getTask().getId(),
                        PreparationTaskMember::getRole,
                        (a, b) -> a));

        List<PreparationTask> ownerTasks = preparationTaskRepository
                .findByActivityIdAndOwnerIdOrderByDeadlineAscIdAsc(activityId, student.getId());

        for (PreparationTask t : ownerTasks) {
            if (t != null && t.getId() != null) {
                roleByTaskId.putIfAbsent(t.getId(), PreparationTaskMemberRole.LEADER);
            }
        }

        java.util.Map<Long, PreparationTask> taskById = new java.util.HashMap<>();

        for (PreparationTaskMember m : memberships) {
            if (m.getTask() != null && m.getTask().getId() != null) {
                taskById.put(m.getTask().getId(), m.getTask());
            }
        }

        for (PreparationTask t : ownerTasks) {
            if (t != null && t.getId() != null) {
                taskById.putIfAbsent(t.getId(), t);
            }
        }

        return taskById.values().stream()
                .sorted(java.util.Comparator.comparing(
                        PreparationTask::getDeadline,
                        java.util.Comparator.nullsLast(java.time.LocalDateTime::compareTo))
                        .thenComparing(PreparationTask::getId))
                .map(t -> {
                    PreparationTaskDto dto = toTaskDto(t);
                    PreparationTaskMemberRole role = roleByTaskId.getOrDefault(
                            t.getId(),
                            PreparationTaskMemberRole.MEMBER
                    );

                    return new MyPreparationTaskDto(
                            dto.getId(),
                            dto.getActivityId(),
                            dto.getOwnerId(),
                            dto.getOwnerName(),
                            dto.getTitle(),
                            dto.getDescription(),
                            dto.getDeadline(),
                            dto.getAllocatedAmount(),
                            Boolean.TRUE.equals(dto.getIsFinancial()),
                            Boolean.TRUE.equals(dto.getIsCheckinScanner()),
                            dto.getStatus(),
                            role,
                            dto.getCompletionProofUrls()
                    );
                })
                .toList();
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

        return activityBudgetRepository.findByActivityId(activityId)
                .map(budget -> new PreparationDashboardDto(activityId, true, tasks, toActivityBudgetDto(budget), null))
                .orElseGet(
                        () -> new PreparationDashboardDto(activityId, true, tasks, null, "No ActivityBudget assigned"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PreparationSummaryResponse> getPreparationsSummary(List<Long> activityIds) {
        if (activityIds == null || activityIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<Activity> activities = activityRepository.findAllById(activityIds);
        List<PreparationSummaryResponse> result = new ArrayList<>();

        for (Activity activity : activities) {
            Long activityId = activity.getId();
            boolean enabled = activity.isHasPreparation();
            long pendingTasks = 0;
            long waitingExpenses = 0;
            String remainingAmount = null;

            if (enabled && !activity.isDeleted()) {
                pendingTasks = preparationTaskRepository.findByActivityIdOrderByDeadlineAscIdAsc(activityId)
                        .stream()
                        .filter(t -> t.getStatus() == PreparationTaskStatus.PENDING)
                        .count();
                waitingExpenses = financeService.listExpensesByActivity(activityId, ExpenseStatus.PENDING_ADMIN).size();
                
                if (activityBudgetRepository.findByActivityId(activityId).isPresent()) {
                    try {
                        FinancialReportDto report = financeService.getFinancialReport(activityId);
                        if (report != null && report.getCategories() != null) {
                            java.math.BigDecimal totalRemaining = java.math.BigDecimal.ZERO;
                            for (vn.campuslife.model.preparation.BudgetCategoryDto c : report.getCategories()) {
                                if (c.getRemainingAmount() != null) {
                                    totalRemaining = totalRemaining.add(c.getRemainingAmount());
                                }
                            }
                            remainingAmount = totalRemaining.toString();
                        }
                    } catch (Exception e) {
                        // Ignore any unexpected errors to prevent transaction rollback
                    }
                }
            }

            result.add(new PreparationSummaryResponse(activityId, enabled, pendingTasks, waitingExpenses, remainingAmount));
        }

        return result;
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
        task.setFinancial(Boolean.TRUE.equals(request.getIsFinancial()));
        task.setCheckinScanner(Boolean.TRUE.equals(request.getIsCheckinScanner()));
        task.setStatus(PreparationTaskStatus.PENDING);
        PreparationTask saved = preparationTaskRepository.save(task);

        PreparationTaskMember leader = preparationTaskMemberRepository
                .findByTaskIdAndStudentId(saved.getId(), assignee.getId())
                .orElseGet(() -> {
                    PreparationTaskMember created = new PreparationTaskMember();
                    created.setTask(saved);
                    created.setStudent(assignee);
                    return created;
                });
        leader.setRole(PreparationTaskMemberRole.LEADER);
        preparationTaskMemberRepository.save(leader);

        if (assignee.getUser() != null) {
            String notificationTitle = "Bạn được giao nhiệm vụ chuẩn bị mới";
            String notificationContent = "Nhiệm vụ: " + saved.getTitle() + " (Hoạt động: " + activity.getName() + ")";
            if (saved.isCheckinScanner()) {
                notificationTitle = "Bạn được phân công nhiệm vụ Quét QR Check-in";
                notificationContent = "Nhiệm vụ quét QR cho sự kiện: " + activity.getName();
            }
            java.util.Map<String, Object> metadata = java.util.Map.of(
                    "taskId", saved.getId(),
                    "activityId", activity.getId(),
                    "isCheckinScanner", saved.isCheckinScanner());
            notificationService.sendNotification(assignee.getUser().getId(), notificationTitle, notificationContent, NotificationType.GENERAL,
                    null, metadata);
        }

        return toTaskDto(saved);
    }

    @Override
    @Transactional
    public PreparationTaskDto updateMyTaskStatus(Long taskId, PreparationTaskStatus status, String username) {
        if (status == PreparationTaskStatus.ACCEPTED) {
            return acceptTask(taskId, username);
        }
        if (status == PreparationTaskStatus.COMPLETION_REQUESTED) {
            return requestCompleteTask(taskId, null, username);
        }
        throw new BadRequestException("Status update is not supported");
    }

    @Override
    @Transactional(readOnly = true)
    public List<PreparationTaskMemberDto> listTaskMembers(Long taskId) {
        PreparationTask task = preparationTaskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        return preparationTaskMemberRepository.findByTaskIdOrderByRoleAscCreatedAtAsc(taskId).stream()
                .map(m -> new PreparationTaskMemberDto(
                        m.getStudent() != null ? m.getStudent().getId() : null,
                        m.getStudent() != null ? m.getStudent().getFullName() : null,
                        m.getRole()))
                .toList();
    }

    @Override
    @Transactional
    public void removeTaskMember(Long taskId, Long studentId) {
        PreparationTask task = preparationTaskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        PreparationTaskMember member = preparationTaskMemberRepository.findByTaskIdAndStudentId(taskId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Task member not found"));
        if (member.getRole() == PreparationTaskMemberRole.LEADER && task.isFinancial()) {
            long leaderCount = preparationTaskMemberRepository.countByTaskIdAndRole(taskId,
                    PreparationTaskMemberRole.LEADER);
            if (leaderCount <= 1) {
                throw new BadRequestException("Financial task must have at least one leader");
            }
        }
        preparationTaskMemberRepository.delete(member);
    }

    @Override
    @Transactional
    public void promoteTaskLeader(Long taskId, Long studentId) {
        PreparationTask task = preparationTaskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        Long activityId = task.getActivity() != null ? task.getActivity().getId() : null;
        if (activityId == null) {
            throw new BadRequestException("Task has no activity");
        }
        if (!activityOrganizerRepository.existsByActivityIdAndStudentId(activityId, studentId)) {
            throw new BadRequestException("Student is not an organizer of this activity");
        }
        Student student = studentRepository.findByIdAndIsDeletedFalse(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        PreparationTaskMember member = preparationTaskMemberRepository.findByTaskIdAndStudentId(taskId, studentId)
                .orElseGet(() -> {
                    PreparationTaskMember created = new PreparationTaskMember();
                    created.setTask(task);
                    created.setStudent(student);
                    return created;
                });
        member.setRole(PreparationTaskMemberRole.LEADER);
        preparationTaskMemberRepository.save(member);

        if (student.getUser() != null) {
            String notificationTitle = "Bạn được phân công tham gia nhiệm vụ chuẩn bị";
            String notificationContent = "Nhiệm vụ: " + task.getTitle() + " (Hoạt động: " + task.getActivity().getName() + ")";
            if (task.isCheckinScanner()) {
                notificationTitle = "Bạn được phân công quét QR Check-in";
                notificationContent = "Nhiệm vụ quét QR cho sự kiện: " + task.getActivity().getName();
            }
            java.util.Map<String, Object> metadata = java.util.Map.of(
                    "taskId", task.getId(),
                    "activityId", task.getActivity().getId(),
                    "isCheckinScanner", task.isCheckinScanner());
            notificationService.sendNotification(student.getUser().getId(), notificationTitle, notificationContent, NotificationType.GENERAL,
                    null, metadata);
        }
    }

    @Override
    @Transactional
    public void demoteTaskLeader(Long taskId, Long studentId) {
        PreparationTask task = preparationTaskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        PreparationTaskMember member = preparationTaskMemberRepository.findByTaskIdAndStudentId(taskId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Task member not found"));
        if (member.getRole() != PreparationTaskMemberRole.LEADER) {
            return;
        }
        if (task.isFinancial()) {
            long leaderCount = preparationTaskMemberRepository.countByTaskIdAndRole(taskId,
                    PreparationTaskMemberRole.LEADER);
            if (leaderCount <= 1) {
                throw new BadRequestException("Financial task must have at least one leader");
            }
        }
        member.setRole(PreparationTaskMemberRole.MEMBER);
        preparationTaskMemberRepository.save(member);
    }

    @Override
    @Transactional
    public PreparationTaskDto acceptTask(Long taskId, String username) {
        Student student = studentRepository.findByUserUsernameAndIsDeletedFalse(username)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        PreparationTask task = preparationTaskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        boolean isOwner = task.getOwner() != null && task.getOwner().getId().equals(student.getId());
        boolean isMember = preparationTaskMemberRepository.existsByTaskIdAndStudentId(taskId, student.getId());
        if (!isOwner && !isMember) {
            throw new ForbiddenException("Task member permission required");
        }
        if (task.getStatus() != PreparationTaskStatus.PENDING) {
            throw new BadRequestException("Task is not pending");
        }
        task.setStatus(PreparationTaskStatus.ACCEPTED);
        return toTaskDto(preparationTaskRepository.save(task));
    }

    @Override
    @Transactional
    public PreparationTaskDto requestCompleteTask(Long taskId, List<String> proofUrls, String username) {
        Student student = studentRepository.findByUserUsernameAndIsDeletedFalse(username)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        PreparationTask task = preparationTaskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        boolean isLeader = preparationTaskMemberRepository.existsByTaskIdAndStudentIdAndRole(
                taskId,
                student.getId(),
                PreparationTaskMemberRole.LEADER);
        boolean isOwner = task.getOwner() != null && task.getOwner().getId().equals(student.getId());
        if (!isLeader && !isOwner) {
            throw new ForbiddenException("Leader permission required");
        }
        if (task.getStatus() != PreparationTaskStatus.ACCEPTED) {
            throw new BadRequestException("Task must be accepted before requesting completion");
        }
        if (proofUrls != null && !proofUrls.isEmpty()) {
            if (proofUrls.size() > 10) {
                throw new BadRequestException("Maximum 10 proof photos allowed");
            }
            try {
                task.setCompletionProofUrls(objectMapper.writeValueAsString(proofUrls));
            } catch (JsonProcessingException e) {
                throw new BadRequestException("Invalid proof URLs format");
            }
        }
        task.setStatus(PreparationTaskStatus.COMPLETION_REQUESTED);
        return toTaskDto(preparationTaskRepository.save(task));
    }

    @Override
    @Transactional
    public PreparationTaskDto adminCompleteDecision(Long taskId, boolean approved) {
        PreparationTask task = preparationTaskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        if (task.getStatus() != PreparationTaskStatus.COMPLETION_REQUESTED) {
            throw new BadRequestException("Task is not pending completion approval");
        }
        task.setStatus(approved ? PreparationTaskStatus.COMPLETED : PreparationTaskStatus.ACCEPTED);
        return toTaskDto(preparationTaskRepository.save(task));
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkloadWarningDto> getWorkloadWarnings(Long activityId) {
        Activity activity = getActiveActivity(activityId);
        if (!activity.isHasPreparation()) {
            throw new FeatureNotEnabledException("Preparation feature is not enabled for this activity");
        }

        Map<Long, Long> countByStudentId = preparationTaskMemberRepository.countTasksByStudentInActivity(activityId)
                .stream()
                .collect(java.util.stream.Collectors.toMap(
                        PreparationTaskMemberRepository.StudentTaskCountView::getStudentId,
                        PreparationTaskMemberRepository.StudentTaskCountView::getTaskCount));

        return activityOrganizerRepository.findByActivityId(activityId).stream()
                .map(ao -> ao.getStudent())
                .filter(s -> s != null && s.getId() != null)
                .map(s -> {
                    long count = countByStudentId.getOrDefault(s.getId(), 0L);
                    if (count > 3) {
                        return new WorkloadWarningDto(s.getId(), s.getFullName(), count,
                                WorkloadWarningType.OVERLOADED);
                    }
                    if (count == 0) {
                        return new WorkloadWarningDto(s.getId(), s.getFullName(), count,
                                WorkloadWarningType.UNASSIGNED);
                    }
                    return null;
                })
                .filter(v -> v != null)
                .toList();
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
    public BulkAddOrganizersResultDto addOrganizers(Long activityId, List<Long> studentIds) {
        Activity activity = getActiveActivity(activityId);
        if (studentIds == null || studentIds.isEmpty()) {
            return new BulkAddOrganizersResultDto(List.of(), List.of());
        }
        List<Long> unique = studentIds.stream().filter(v -> v != null).distinct().toList();
        if (unique.isEmpty()) {
            return new BulkAddOrganizersResultDto(List.of(), List.of());
        }

        List<Student> students = studentRepository.findAllById(unique).stream()
                .filter(s -> s != null && !s.isDeleted())
                .toList();
        if (students.size() != unique.size()) {
            throw new ResourceNotFoundException("Some students not found");
        }

        List<OrganizerDto> added = new java.util.ArrayList<>();
        List<Long> skipped = new java.util.ArrayList<>();
        for (Student student : students) {
            Long sid = student.getId();
            if (activityOrganizerRepository.existsByActivityIdAndStudentId(activityId, sid)) {
                skipped.add(sid);
                continue;
            }
            ActivityOrganizer organizer = new ActivityOrganizer();
            organizer.setActivity(activity);
            organizer.setStudent(student);
            activityOrganizerRepository.save(organizer);
            added.add(new OrganizerDto(student.getId(), student.getFullName(), false));

            if (student.getUser() != null) {
                String title = "Bạn đã được thêm vào BTC";
                String content = "Hoạt động: " + activity.getName();
                Map<String, Object> metadata = Map.of(
                        "activityId", activity.getId(),
                        "role", "ORGANIZER");
                notificationService.sendNotification(student.getUser().getId(), title, content,
                        NotificationType.GENERAL,
                        null, metadata);
            }
        }

        return new BulkAddOrganizersResultDto(added, skipped);
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
                        ao.getStudent() != null ? ao.getStudent().getFullName() : null,
                        ao.isPrepSupervisor()))
                .toList();
    }

    @Override
    @Transactional
    public void grantPrepSupervisor(Long activityId, Long studentId) {
        ActivityOrganizer organizer = activityOrganizerRepository
                .findByActivityIdAndStudentId(activityId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Student is not an organizer of this activity"));
        organizer.setPrepSupervisor(true);
        activityOrganizerRepository.save(organizer);
    }

    @Override
    @Transactional
    public void revokePrepSupervisor(Long activityId, Long studentId) {
        ActivityOrganizer organizer = activityOrganizerRepository
                .findByActivityIdAndStudentId(activityId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Student is not an organizer of this activity"));
        organizer.setPrepSupervisor(false);
        activityOrganizerRepository.save(organizer);
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
        List<String> proofUrls = parseProofUrls(task.getCompletionProofUrls());
        return new PreparationTaskDto(
                task.getId(),
                activityId,
                ownerId,
                ownerName,
                task.getTitle(),
                task.getDescription(),
                task.getDeadline(),
                task.getAllocatedAmount(),
                task.isFinancial(),
                task.isCheckinScanner(),
                task.getStatus(),
                proofUrls);
    }

    private List<String> parseProofUrls(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    private ActivityBudgetDto toActivityBudgetDto(ActivityBudget budget) {
        Map<Long, BigDecimal> allocatedToTasksByCategory = taskAllocationRepository
                .sumAllocatedToTasksByActivity(budget.getActivity().getId())
                .stream()
                .collect(java.util.stream.Collectors.toMap(
                        TaskAllocationRepository.CategoryAllocationSumView::getCategoryId,
                        TaskAllocationRepository.CategoryAllocationSumView::getAllocatedToTasksAmount));
        Map<Long, BigDecimal> holdingByCategoryId = fundAdvanceRepository
                .sumHoldingByCategoryInActivity(budget.getActivity().getId())
                .stream()
                .collect(java.util.stream.Collectors.toMap(
                        FundAdvanceRepository.FundAdvanceHoldingByCategoryView::getCategoryId,
                        FundAdvanceRepository.FundAdvanceHoldingByCategoryView::getHoldingAmount));
        List<BudgetCategoryDto> categories = budget.getCategories().stream()
                .sorted(Comparator.comparing(BudgetCategory::getId, Comparator.nullsLast(Long::compareTo)))
                .map(c -> {
                    BigDecimal allocated = zeroIfNull(c.getAllocatedAmount());
                    BigDecimal allocatedToTasks = allocatedToTasksByCategory.getOrDefault(c.getId(), BigDecimal.ZERO);
                    BigDecimal availableToAllocate = allocated.subtract(allocatedToTasks);
                    BigDecimal used = zeroIfNull(c.getUsedAmount());
                    BigDecimal remaining = allocated.subtract(used);
                    BigDecimal cashOutside = holdingByCategoryId.getOrDefault(c.getId(), BigDecimal.ZERO);
                    BigDecimal cashAvailable = remaining.subtract(cashOutside);
                    Double percent = allocated.compareTo(BigDecimal.ZERO) > 0
                            ? used.multiply(BigDecimal.valueOf(100))
                                    .divide(allocated, 2, RoundingMode.HALF_UP)
                                    .doubleValue()
                            : 0.0;
                    return new BudgetCategoryDto(c.getId(), c.getName(), allocated, allocatedToTasks,
                            availableToAllocate,
                            cashOutside,
                            cashAvailable,
                            used, remaining, percent);
                })
                .toList();
        return new ActivityBudgetDto(
                budget.getId(),
                budget.getActivity() != null ? budget.getActivity().getId() : null,
                budget.getTotalAmount(),
                categories);
    }

    private BigDecimal zeroIfNull(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
