package vn.campuslife.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.campuslife.entity.*;
import vn.campuslife.enumeration.NotificationType;
import vn.campuslife.enumeration.PreparationTaskStatus;
import vn.campuslife.enumeration.Role;
import vn.campuslife.exception.*;
import vn.campuslife.model.preparation.*;
import vn.campuslife.repository.*;
import vn.campuslife.service.FileUploadService;
import vn.campuslife.service.NotificationService;
import vn.campuslife.service.PreparationService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PreparationServiceImpl implements PreparationService {

    private final ActivityRepository activityRepository;
    private final StudentRepository studentRepository;
    private final ActivityOrganizerRepository activityOrganizerRepository;
    private final PreparationTaskRepository preparationTaskRepository;
    private final BudgetRepository budgetRepository;
    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;
    private final FileUploadService fileUploadService;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public void togglePreparation(Long activityId, boolean enabled) {
        Activity activity = getActiveActivity(activityId);
        activity.setHasPreparation(enabled);
        activityRepository.save(activity);
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

        return budgetRepository.findByActivityId(activityId)
                .map(budget -> {
                    BudgetDto budgetDto = toBudgetDto(budget);
                    return new PreparationDashboardDto(activityId, true, tasks, budgetDto, null);
                })
                .orElseGet(() -> new PreparationDashboardDto(activityId, true, tasks, null, "No Budget Assigned"));
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

        Student assignee = studentRepository.findByIdAndIsDeletedFalse(request.getAssigneeId())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        boolean organizer = activityOrganizerRepository.existsByActivityIdAndStudentId(activity.getId(),
                assignee.getId());
        if (!organizer) {
            throw new BadRequestException("Assignee must be an organizer of this activity");
        }

        PreparationTask task = new PreparationTask();
        task.setActivity(activity);
        task.setAssignee(assignee);
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setDeadline(request.getDeadline());
        task.setStatus(PreparationTaskStatus.PENDING);
        PreparationTask saved = preparationTaskRepository.save(task);

        return toTaskDto(saved);
    }

    @Override
    @Transactional
    public PreparationTaskDto updateMyTaskStatus(Long taskId, PreparationTaskStatus status, String username) {
        Student student = studentRepository.findByUserUsernameAndIsDeletedFalse(username)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        PreparationTask task = preparationTaskRepository.findByIdAndAssigneeId(taskId, student.getId())
                .orElseThrow(() -> new ForbiddenException("You are not allowed to update this task"));

        task.setStatus(status);
        PreparationTask saved = preparationTaskRepository.save(task);
        return toTaskDto(saved);
    }

    @Override
    @Transactional
    public BudgetDto createOrUpdateBudget(UpsertBudgetRequest request) {
        if (request.getActivityId() == null) {
            throw new BadRequestException("Activity ID is required");
        }
        Activity activity = getActiveActivity(request.getActivityId());
        if (!activity.isHasPreparation()) {
            throw new FeatureNotEnabledException("Preparation feature is not enabled for this activity");
        }

        Budget budget = budgetRepository.findByActivityId(activity.getId()).orElseGet(Budget::new);
        budget.setActivity(activity);
        budget.setTotalAmount(request.getTotalAmount());
        budget.setDescription(request.getDescription());

        Budget saved = budgetRepository.save(budget);
        return toBudgetDto(saved);
    }

    @Override
    @Transactional
    public ExpenseDto createExpense(CreateExpenseRequest request, String username) {
        if (request.getActivityId() == null) {
            throw new BadRequestException("Activity ID is required");
        }
        Activity activity = getActiveActivity(request.getActivityId());
        if (!activity.isHasPreparation()) {
            throw new FeatureNotEnabledException("Preparation feature is not enabled for this activity");
        }

        Student reporter = studentRepository.findByUserUsernameAndIsDeletedFalse(username)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        boolean organizer = activityOrganizerRepository.existsByActivityIdAndStudentId(activity.getId(),
                reporter.getId());
        if (!organizer) {
            throw new ForbiddenException("Organizer permission required");
        }

        Budget budget = budgetRepository.findByActivityId(activity.getId())
                .orElseThrow(() -> new BadRequestException("No Budget Assigned"));

        Expense expense = new Expense();
        expense.setBudget(budget);
        expense.setAmount(request.getAmount());
        expense.setDescription(request.getDescription());
        expense.setEvidenceUrl(request.getEvidenceUrl());
        expense.setReportedBy(reporter);
        expense.setApproved(null);

        Expense saved = expenseRepository.save(expense);

        notifyAdminsForExpense(activity, budget, saved, reporter);

        return toExpenseDto(saved, activity.getId());
    }

    @Override
    public UploadResultDto uploadExpenseEvidence(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is required");
        }
        String url = fileUploadService.uploadFile(file);
        return new UploadResultDto(url);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExpenseDto> listExpenses(Long activityId, String status) {
        Activity activity = getActiveActivity(activityId);
        if (!activity.isHasPreparation()) {
            throw new FeatureNotEnabledException("Preparation feature is not enabled for this activity");
        }
        budgetRepository.findByActivityId(activityId)
                .orElseThrow(() -> new BadRequestException("No Budget Assigned"));

        String normalized = status == null ? "ALL" : status.trim().toUpperCase();

        List<Expense> expenses = switch (normalized) {
            case "PENDING" -> expenseRepository.findByBudgetActivityIdAndApprovedIsNullOrderByCreatedAtDesc(activityId);
            case "APPROVED" ->
                expenseRepository.findByBudgetActivityIdAndApprovedOrderByCreatedAtDesc(activityId, true);
            case "REJECTED" ->
                expenseRepository.findByBudgetActivityIdAndApprovedOrderByCreatedAtDesc(activityId, false);
            case "ALL" -> expenseRepository.findByBudgetActivityIdOrderByCreatedAtDesc(activityId);
            default -> throw new BadRequestException("Invalid status. Use PENDING/APPROVED/REJECTED/ALL");
        };

        return expenses.stream()
                .map(e -> toExpenseDto(e, activityId))
                .toList();
    }

    @Override
    @Transactional
    public ExpenseDto approveExpense(Long expenseId, boolean approved) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found"));

        Budget budget = expense.getBudget();
        if (budget == null || budget.getActivity() == null) {
            throw new BadRequestException("Expense has no associated activity");
        }

        Activity activity = getActiveActivity(budget.getActivity().getId());
        if (!activity.isHasPreparation()) {
            throw new FeatureNotEnabledException("Preparation feature is not enabled for this activity");
        }

        if (Boolean.TRUE.equals(expense.getApproved()) && approved) {
            return toExpenseDto(expense, activity.getId());
        }

        if (approved) {
            BigDecimal spent = expenseRepository.sumApprovedAmountByBudgetId(budget.getId());
            BigDecimal remaining = budget.getTotalAmount().subtract(spent);
            if (remaining.compareTo(expense.getAmount()) < 0) {
                throw new InsufficientBudgetException("Insufficient budget remaining");
            }
            expense.setApproved(true);
        } else {
            expense.setApproved(false);
        }

        Expense saved = expenseRepository.save(expense);
        notifyReporterForExpenseDecision(activity, saved);
        return toExpenseDto(saved, activity.getId());
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

    private BudgetDto toBudgetDto(Budget budget) {
        BigDecimal spent = expenseRepository.sumApprovedAmountByBudgetId(budget.getId());
        BigDecimal remaining = budget.getTotalAmount().subtract(spent);
        Long activityId = budget.getActivity() != null ? budget.getActivity().getId() : null;
        return new BudgetDto(
                budget.getId(),
                activityId,
                budget.getTotalAmount(),
                spent,
                remaining,
                budget.getDescription());
    }

    private PreparationTaskDto toTaskDto(PreparationTask task) {
        Student assignee = task.getAssignee();
        String assigneeName = assignee != null ? assignee.getFullName() : null;
        Long activityId = task.getActivity() != null ? task.getActivity().getId() : null;
        Long assigneeId = assignee != null ? assignee.getId() : null;
        return new PreparationTaskDto(
                task.getId(),
                activityId,
                assigneeId,
                assigneeName,
                task.getTitle(),
                task.getDescription(),
                task.getDeadline(),
                task.getStatus());
    }

    private ExpenseDto toExpenseDto(Expense expense, Long activityId) {
        Student reporter = expense.getReportedBy();
        return new ExpenseDto(
                expense.getId(),
                activityId,
                expense.getBudget() != null ? expense.getBudget().getId() : null,
                expense.getAmount(),
                expense.getDescription(),
                expense.getEvidenceUrl(),
                reporter != null ? reporter.getId() : null,
                reporter != null ? reporter.getFullName() : null,
                expense.getApproved(),
                expense.getCreatedAt());
    }

    private void notifyAdminsForExpense(Activity activity, Budget budget, Expense expense, Student reporter) {
        List<User> admins = userRepository.findAllByRoleInAndIsDeletedFalse(List.of(Role.ADMIN, Role.MANAGER));
        List<Long> userIds = admins.stream().map(User::getId).toList();
        if (userIds.isEmpty()) {
            return;
        }

        String title = "Báo cáo chi phí mới";
        String content = "Hoạt động: " + activity.getName()
                + " | Số tiền: " + expense.getAmount()
                + " | Trạng thái: PENDING"
                + " | Người báo cáo: " + (reporter.getFullName() != null ? reporter.getFullName() : reporter.getId());

        Map<String, Object> metadata = Map.of(
                "activityId", activity.getId(),
                "budgetId", budget.getId(),
                "expenseId", expense.getId());

        notificationService.sendBulkNotification(userIds, title, content, NotificationType.GENERAL, null, metadata);
    }

    private void notifyReporterForExpenseDecision(Activity activity, Expense expense) {
        Student reporter = expense.getReportedBy();
        if (reporter == null || reporter.getUser() == null) {
            return;
        }
        String decision = Boolean.TRUE.equals(expense.getApproved()) ? "APPROVED" : "REJECTED";
        String title = "Chi phí đã được duyệt";
        if ("REJECTED".equals(decision)) {
            title = "Chi phí bị từ chối";
        }
        String content = "Hoạt động: " + activity.getName()
                + " | Số tiền: " + expense.getAmount()
                + " | Trạng thái: " + decision;

        Map<String, Object> metadata = Map.of(
                "activityId", activity.getId(),
                "expenseId", expense.getId());

        notificationService.sendNotification(reporter.getUser().getId(), title, content, NotificationType.GENERAL, null,
                metadata);
    }
}
