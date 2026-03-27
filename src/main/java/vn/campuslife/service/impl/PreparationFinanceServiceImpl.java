package vn.campuslife.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import vn.campuslife.entity.*;
import vn.campuslife.enumeration.ExpenseStatus;
import vn.campuslife.enumeration.FundAdvanceStatus;
import vn.campuslife.enumeration.NotificationType;
import vn.campuslife.enumeration.Role;
import vn.campuslife.exception.BadRequestException;
import vn.campuslife.exception.FeatureNotEnabledException;
import vn.campuslife.exception.ForbiddenException;
import vn.campuslife.exception.InsufficientBudgetException;
import vn.campuslife.exception.ResourceNotFoundException;
import vn.campuslife.model.preparation.*;
import vn.campuslife.repository.*;
import vn.campuslife.service.NotificationService;
import vn.campuslife.service.PreparationFinanceService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PreparationFinanceServiceImpl implements PreparationFinanceService {
    private final ActivityRepository activityRepository;
    private final ActivityBudgetRepository activityBudgetRepository;
    private final BudgetCategoryRepository budgetCategoryRepository;
    private final PreparationTaskRepository preparationTaskRepository;
    private final PreparationTaskMemberRepository preparationTaskMemberRepository;
    private final ExpenseRepository expenseRepository;
    private final FundAdvanceRepository fundAdvanceRepository;
    private final AuditLogRepository auditLogRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public ActivityBudgetDto upsertActivityBudget(Long activityId, UpsertActivityBudgetRequest request) {
        Activity activity = getActiveActivity(activityId);
        if (!activity.isHasPreparation()) {
            throw new FeatureNotEnabledException("Preparation feature is not enabled for this activity");
        }

        BigDecimal totalAmount = zeroIfNull(request.getTotalAmount());
        List<UpsertBudgetCategoryRequest> reqCats = request.getCategories() == null ? List.of()
                : request.getCategories();
        BigDecimal sumAllocated = reqCats.stream()
                .map(UpsertBudgetCategoryRequest::getAllocatedAmount)
                .map(this::zeroIfNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (sumAllocated.compareTo(totalAmount) > 0) {
            throw new BadRequestException("Sum of category allocated amounts cannot exceed total budget");
        }

        ActivityBudget budget = activityBudgetRepository.findByActivityId(activityId).orElseGet(ActivityBudget::new);
        budget.setActivity(activity);
        budget.setTotalAmount(totalAmount);
        Set<String> reqNames = reqCats.stream()
                .map(c -> c.getName().trim())
                .collect(Collectors.toSet());

        budget.getCategories().removeIf(
                c -> !reqNames.contains(c.getName()) && zeroIfNull(c.getUsedAmount()).compareTo(BigDecimal.ZERO) == 0);

        List<String> cannotRemove = budget.getCategories().stream()
                .filter(c -> !reqNames.contains(c.getName())
                        && zeroIfNull(c.getUsedAmount()).compareTo(BigDecimal.ZERO) > 0)
                .map(BudgetCategory::getName)
                .toList();
        if (!cannotRemove.isEmpty()) {
            throw new BadRequestException(
                    "Cannot remove categories with used amount: " + String.join(", ", cannotRemove));
        }

        for (UpsertBudgetCategoryRequest req : reqCats) {
            String name = req.getName().trim();
            BigDecimal allocated = zeroIfNull(req.getAllocatedAmount());

            BudgetCategory existing = budget.getCategories().stream()
                    .filter(c -> c.getName().equalsIgnoreCase(name))
                    .findFirst()
                    .orElse(null);
            if (existing == null) {
                BudgetCategory created = new BudgetCategory();
                created.setActivityBudget(budget);
                created.setName(name);
                created.setAllocatedAmount(allocated);
                budget.getCategories().add(created);
            } else {
                if (allocated.compareTo(zeroIfNull(existing.getUsedAmount())) < 0) {
                    throw new BadRequestException(
                            "Allocated amount cannot be less than used amount for category: " + name);
                }
                existing.setName(name);
                existing.setAllocatedAmount(allocated);
            }
        }

        ActivityBudget saved = activityBudgetRepository.save(budget);
        writeAudit(null, "UPSERT_BUDGET", "ActivityBudget", saved.getId(), "activityId=" + activityId);
        return toActivityBudgetDto(saved);
    }

    @Override
    @Transactional
    public PreparationTaskDto allocateTaskAmount(Long taskId, AllocateTaskAmountRequest request) {
        PreparationTask task = preparationTaskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        BigDecimal newAllocated = zeroIfNull(request.getAllocatedAmount());

        BigDecimal spent = expenseRepository.sumApprovedAmountByTaskId(taskId);
        if (newAllocated.compareTo(spent) < 0) {
            throw new BadRequestException("Allocated amount cannot be less than approved spent");
        }

        if (task.getBudgetLimit() != null && newAllocated.compareTo(task.getBudgetLimit()) > 0) {
            throw new BadRequestException("Allocated amount cannot exceed task budget limit");
        }

        if (task.getActivity() != null) {
            ActivityBudget activityBudget = activityBudgetRepository.findByActivityId(task.getActivity().getId())
                    .orElseThrow(() -> new BadRequestException("No ActivityBudget assigned"));
            BigDecimal currentTotalAllocated = preparationTaskRepository
                    .sumAllocatedAmountByActivityId(task.getActivity().getId());
            BigDecimal newTotalAllocated = currentTotalAllocated
                    .subtract(zeroIfNull(task.getAllocatedAmount()))
                    .add(newAllocated);
            if (newTotalAllocated.compareTo(zeroIfNull(activityBudget.getTotalAmount())) > 0) {
                throw new InsufficientBudgetException("Insufficient activity total budget for task allocation");
            }
        }

        task.setAllocatedAmount(newAllocated);
        PreparationTask saved = preparationTaskRepository.save(task);
        writeAudit(null, "ALLOCATE_TASK_AMOUNT", "PreparationTask", saved.getId(), "allocatedAmount=" + newAllocated);
        return toTaskDto(saved);
    }

    @Override
    @Transactional
    public void addTaskMember(Long taskId, Long studentId) {
        PreparationTask task = preparationTaskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        Student student = studentRepository.findByIdAndIsDeletedFalse(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        if (task.getOwner() != null && task.getOwner().getId().equals(studentId)) {
            return;
        }

        if (preparationTaskMemberRepository.existsByTaskIdAndStudentId(taskId, studentId)) {
            return;
        }

        PreparationTaskMember member = new PreparationTaskMember();
        member.setTask(task);
        member.setStudent(student);
        preparationTaskMemberRepository.save(member);
        writeAudit(null, "ADD_TASK_MEMBER", "PreparationTask", taskId, "studentId=" + studentId);
    }

    @Override
    @Transactional
    public FundAdvanceDto createFundAdvance(Long taskId, CreateFundAdvanceRequest request) {
        PreparationTask task = preparationTaskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        if (!task.isFinancial()) {
            throw new BadRequestException("Task is not financial");
        }

        Student student = studentRepository.findByIdAndIsDeletedFalse(request.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        BigDecimal amount = zeroIfNull(request.getAmount());
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Amount must be > 0");
        }

        FundAdvance advance = new FundAdvance();
        advance.setTask(task);
        advance.setStudent(student);
        advance.setAmount(amount);
        advance.setRemainingAmount(amount);
        advance.setStatus(FundAdvanceStatus.HOLDING);

        FundAdvance saved = fundAdvanceRepository.save(advance);
        writeAudit(null, "CREATE_FUND_ADVANCE", "FundAdvance", saved.getId(),
                "taskId=" + taskId + ",studentId=" + student.getId() + ",amount=" + amount);
        return toFundAdvanceDto(saved);
    }

    @Override
    @Transactional
    public ExpenseDto createExpense(CreateExpenseRequest request, String username) {
        Student creator = studentRepository.findByUserUsernameAndIsDeletedFalse(username)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        PreparationTask task = preparationTaskRepository.findById(request.getTaskId())
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        if (!task.isFinancial()) {
            throw new BadRequestException("Task is not financial");
        }
        if (task.getActivity() == null) {
            throw new BadRequestException("Task has no activity");
        }

        BudgetCategory category = budgetCategoryRepository
                .findByIdAndActivityBudgetActivityId(request.getCategoryId(), task.getActivity().getId())
                .orElseThrow(() -> new BadRequestException("Invalid category for this activity"));

        Expense expense = new Expense();
        expense.setTask(task);
        expense.setCategory(category);
        expense.setAmount(request.getAmount());
        expense.setDescription(request.getDescription());
        expense.setEvidenceUrl(request.getEvidenceUrl());
        expense.setCreatedBy(creator);
        expense.setStatus(ExpenseStatus.PENDING_LEADER);

        Expense saved = expenseRepository.save(expense);
        writeAudit(creator.getUser(), "CREATE_EXPENSE", "Expense", saved.getId(),
                "taskId=" + task.getId() + ",amount=" + saved.getAmount());

        notifyLeaderForExpense(task, saved);
        return toExpenseDto(saved);
    }

    @Override
    @Transactional
    public ExpenseDto leaderDecision(Long expenseId, boolean approved, String username) {
        Student leader = studentRepository.findByUserUsernameAndIsDeletedFalse(username)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found"));
        if (expense.getTask() == null || expense.getTask().getOwner() == null) {
            throw new BadRequestException("Expense has no leader");
        }
        if (!expense.getTask().getOwner().getId().equals(leader.getId())) {
            throw new ForbiddenException("Leader permission required");
        }
        if (expense.getStatus() != ExpenseStatus.PENDING_LEADER) {
            throw new BadRequestException("Expense is not pending leader approval");
        }

        if (approved) {
            expense.setStatus(ExpenseStatus.PENDING_ADMIN);
        } else {
            expense.setStatus(ExpenseStatus.REJECTED);
        }

        Expense saved = expenseRepository.save(expense);
        writeAudit(leader.getUser(), "LEADER_DECISION", "Expense", saved.getId(), "approved=" + approved);

        if (saved.getStatus() == ExpenseStatus.PENDING_ADMIN) {
            notifyAdminsForPendingAdmin(saved);
        } else {
            notifyCreatorForDecision(saved);
        }

        return toExpenseDto(saved);
    }

    @Override
    @Transactional
    public ExpenseDto adminDecision(Long expenseId, boolean approved, String username) {
        User actor = userRepository.findByUsernameAndIsDeletedFalse(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found"));
        if (expense.getStatus() != ExpenseStatus.PENDING_ADMIN) {
            throw new BadRequestException("Expense is not pending admin approval");
        }
        if (expense.getTask() == null || expense.getTask().getActivity() == null) {
            throw new BadRequestException("Expense has no activity");
        }
        if (expense.getCategory() == null) {
            throw new BadRequestException("Expense has no category");
        }

        if (!approved) {
            expense.setStatus(ExpenseStatus.REJECTED);
            Expense saved = expenseRepository.save(expense);
            writeAudit(actor, "ADMIN_DECISION", "Expense", saved.getId(), "approved=false");
            notifyCreatorForDecision(saved);
            return toExpenseDto(saved);
        }

        PreparationTask task = expense.getTask();
        BigDecimal spentTask = expenseRepository.sumApprovedAmountByTaskId(task.getId());
        BigDecimal newSpentTask = spentTask.add(zeroIfNull(expense.getAmount()));

        BigDecimal allocatedAmount = zeroIfNull(task.getAllocatedAmount());
        if (allocatedAmount.compareTo(newSpentTask) < 0) {
            throw new InsufficientBudgetException("Insufficient task allocated amount");
        }
        if (task.getBudgetLimit() != null && task.getBudgetLimit().compareTo(newSpentTask) < 0) {
            throw new InsufficientBudgetException("Task budget limit exceeded");
        }

        BudgetCategory category = expense.getCategory();
        BigDecimal remainingCategory = zeroIfNull(category.getAllocatedAmount())
                .subtract(zeroIfNull(category.getUsedAmount()));
        if (remainingCategory.compareTo(zeroIfNull(expense.getAmount())) < 0) {
            throw new InsufficientBudgetException("Insufficient category budget remaining");
        }

        ensureSufficientFundAdvance(task.getId(), expense.getCreatedBy().getId(), expense.getAmount());

        expense.setStatus(ExpenseStatus.APPROVED);
        Expense saved = expenseRepository.save(expense);

        deductFromFundAdvances(task.getId(), expense.getCreatedBy().getId(), expense.getAmount());
        category.setUsedAmount(zeroIfNull(category.getUsedAmount()).add(zeroIfNull(expense.getAmount())));
        budgetCategoryRepository.save(category);

        writeAudit(actor, "ADMIN_DECISION", "Expense", saved.getId(), "approved=true");

        notifyCreatorForDecision(saved);
        notifyBudgetLowIfNeeded(task, category);

        return toExpenseDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExpenseDto> listExpensesByActivity(Long activityId, ExpenseStatus status) {
        Activity activity = getActiveActivity(activityId);
        if (!activity.isHasPreparation()) {
            throw new FeatureNotEnabledException("Preparation feature is not enabled for this activity");
        }
        List<Expense> expenses = status == null ? expenseRepository.findByTaskActivityIdOrderByCreatedAtDesc(activityId)
                : expenseRepository.findByTaskActivityIdAndStatusOrderByCreatedAtDesc(activityId, status);
        return expenses.stream().map(this::toExpenseDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public FinancialReportDto getFinancialReport(Long activityId) {
        Activity activity = getActiveActivity(activityId);
        if (!activity.isHasPreparation()) {
            throw new FeatureNotEnabledException("Preparation feature is not enabled for this activity");
        }

        ActivityBudget budget = activityBudgetRepository.findByActivityId(activityId)
                .orElseThrow(() -> new BadRequestException("No ActivityBudget assigned"));

        List<BudgetCategoryDto> categories = budget.getCategories().stream()
                .sorted(Comparator.comparing(BudgetCategory::getId, Comparator.nullsLast(Long::compareTo)))
                .map(c -> {
                    BigDecimal allocated = zeroIfNull(c.getAllocatedAmount());
                    BigDecimal used = zeroIfNull(c.getUsedAmount());
                    BigDecimal remaining = allocated.subtract(used);
                    Double percent = allocated.compareTo(BigDecimal.ZERO) > 0
                            ? used.multiply(BigDecimal.valueOf(100))
                                    .divide(allocated, 2, RoundingMode.HALF_UP)
                                    .doubleValue()
                            : 0.0;
                    return new BudgetCategoryDto(c.getId(), c.getName(), allocated, used, remaining, percent);
                })
                .toList();

        List<PreparationTask> tasks = preparationTaskRepository.findByActivityIdOrderByDeadlineAscIdAsc(activityId);
        List<TaskOverBudgetDto> overBudget = tasks.stream()
                .filter(PreparationTask::isFinancial)
                .map(t -> {
                    BigDecimal spent = expenseRepository.sumApprovedAmountByTaskId(t.getId());
                    boolean over = false;
                    if (t.getBudgetLimit() != null && spent.compareTo(t.getBudgetLimit()) > 0) {
                        over = true;
                    }
                    if (zeroIfNull(t.getAllocatedAmount()).compareTo(spent) < 0) {
                        over = true;
                    }
                    return over
                            ? new TaskOverBudgetDto(t.getId(), t.getTitle(), t.getBudgetLimit(),
                                    zeroIfNull(t.getAllocatedAmount()), spent)
                            : null;
                })
                .filter(v -> v != null)
                .toList();

        return new FinancialReportDto(activityId, zeroIfNull(budget.getTotalAmount()), categories, overBudget);
    }

    private Activity getActiveActivity(Long activityId) {
        return activityRepository.findByIdAndIsDeletedFalse(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Activity not found"));
    }

    private ActivityBudgetDto toActivityBudgetDto(ActivityBudget budget) {
        List<BudgetCategoryDto> cats = budget.getCategories().stream()
                .sorted(Comparator.comparing(BudgetCategory::getId, Comparator.nullsLast(Long::compareTo)))
                .map(c -> {
                    BigDecimal allocated = zeroIfNull(c.getAllocatedAmount());
                    BigDecimal used = zeroIfNull(c.getUsedAmount());
                    BigDecimal remaining = allocated.subtract(used);
                    Double percent = allocated.compareTo(BigDecimal.ZERO) > 0
                            ? used.multiply(BigDecimal.valueOf(100))
                                    .divide(allocated, 2, RoundingMode.HALF_UP)
                                    .doubleValue()
                            : 0.0;
                    return new BudgetCategoryDto(c.getId(), c.getName(), allocated, used, remaining, percent);
                })
                .toList();
        return new ActivityBudgetDto(budget.getId(), budget.getActivity() != null ? budget.getActivity().getId() : null,
                budget.getTotalAmount(), cats);
    }

    private PreparationTaskDto toTaskDto(PreparationTask task) {
        Student owner = task.getOwner();
        return new PreparationTaskDto(
                task.getId(),
                task.getActivity() != null ? task.getActivity().getId() : null,
                owner != null ? owner.getId() : null,
                owner != null ? owner.getFullName() : null,
                task.getTitle(),
                task.getDescription(),
                task.getDeadline(),
                task.getBudgetLimit(),
                zeroIfNull(task.getAllocatedAmount()),
                task.isFinancial(),
                task.getStatus());
    }

    private ExpenseDto toExpenseDto(Expense expense) {
        PreparationTask task = expense.getTask();
        BudgetCategory category = expense.getCategory();
        Student creator = expense.getCreatedBy();
        Long activityId = task != null && task.getActivity() != null ? task.getActivity().getId() : null;
        return new ExpenseDto(
                expense.getId(),
                activityId,
                task != null ? task.getId() : null,
                category != null ? category.getId() : null,
                category != null ? category.getName() : null,
                expense.getAmount(),
                expense.getDescription(),
                expense.getEvidenceUrl(),
                expense.getStatus(),
                creator != null ? creator.getId() : null,
                creator != null ? creator.getFullName() : null,
                expense.getCreatedAt());
    }

    private FundAdvanceDto toFundAdvanceDto(FundAdvance advance) {
        Student s = advance.getStudent();
        return new FundAdvanceDto(
                advance.getId(),
                advance.getTask() != null ? advance.getTask().getId() : null,
                s != null ? s.getId() : null,
                s != null ? s.getFullName() : null,
                advance.getAmount(),
                advance.getRemainingAmount(),
                advance.getStatus(),
                advance.getCreatedAt());
    }

    private void ensureSufficientFundAdvance(Long taskId, Long studentId, BigDecimal amount) {
        List<FundAdvance> holding = fundAdvanceRepository.findByTaskIdAndStudentIdAndStatusOrderByCreatedAtAsc(taskId,
                studentId, FundAdvanceStatus.HOLDING);
        BigDecimal totalRemaining = holding.stream()
                .map(FundAdvance::getRemainingAmount)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalRemaining.compareTo(zeroIfNull(amount)) < 0) {
            throw new InsufficientBudgetException("Insufficient fund advance remaining");
        }
    }

    private void deductFromFundAdvances(Long taskId, Long studentId, BigDecimal amount) {
        BigDecimal left = zeroIfNull(amount);
        List<FundAdvance> holding = fundAdvanceRepository.findByTaskIdAndStudentIdAndStatusOrderByCreatedAtAsc(taskId,
                studentId, FundAdvanceStatus.HOLDING);

        for (FundAdvance fa : holding) {
            if (left.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            BigDecimal remaining = zeroIfNull(fa.getRemainingAmount());
            BigDecimal deduct = remaining.min(left);
            BigDecimal newRemaining = remaining.subtract(deduct);
            fa.setRemainingAmount(newRemaining);
            if (newRemaining.compareTo(BigDecimal.ZERO) == 0) {
                fa.setStatus(FundAdvanceStatus.SETTLED);
            }
            fundAdvanceRepository.save(fa);
            left = left.subtract(deduct);
        }

        if (left.compareTo(BigDecimal.ZERO) > 0) {
            throw new InsufficientBudgetException("Insufficient fund advance remaining");
        }
    }

    private void notifyLeaderForExpense(PreparationTask task, Expense expense) {
        Student leader = task.getOwner();
        if (leader == null || leader.getUser() == null) {
            return;
        }
        String title = "Chi phí chờ leader duyệt";
        String content = "Nhiệm vụ: " + task.getTitle() + " | Số tiền: " + expense.getAmount();
        Map<String, Object> metadata = Map.of(
                "activityId", task.getActivity() != null ? task.getActivity().getId() : null,
                "taskId", task.getId(),
                "expenseId", expense.getId(),
                "status", expense.getStatus().name());
        notificationService.sendNotification(leader.getUser().getId(), title, content, NotificationType.GENERAL, null,
                metadata);
    }

    private void notifyAdminsForPendingAdmin(Expense expense) {
        List<User> admins = userRepository.findAllByRoleInAndIsDeletedFalse(List.of(Role.ADMIN, Role.MANAGER));
        List<Long> userIds = admins.stream().map(User::getId).toList();
        if (userIds.isEmpty()) {
            return;
        }
        PreparationTask task = expense.getTask();
        String title = "Chi phí chờ admin duyệt";
        String content = "Nhiệm vụ: " + (task != null ? task.getTitle() : "-") + " | Số tiền: " + expense.getAmount();
        Map<String, Object> metadata = Map.of(
                "activityId", task != null && task.getActivity() != null ? task.getActivity().getId() : null,
                "taskId", task != null ? task.getId() : null,
                "expenseId", expense.getId(),
                "status", expense.getStatus().name());
        notificationService.sendBulkNotification(userIds, title, content, NotificationType.GENERAL, null, metadata);
    }

    private void notifyCreatorForDecision(Expense expense) {
        Student creator = expense.getCreatedBy();
        if (creator == null || creator.getUser() == null) {
            return;
        }
        String title = expense.getStatus() == ExpenseStatus.APPROVED ? "Chi phí đã được duyệt" : "Chi phí bị từ chối";
        String content = "Số tiền: " + expense.getAmount() + " | Trạng thái: " + expense.getStatus().name();
        PreparationTask task = expense.getTask();
        Map<String, Object> metadata = Map.of(
                "activityId", task != null && task.getActivity() != null ? task.getActivity().getId() : null,
                "taskId", task != null ? task.getId() : null,
                "expenseId", expense.getId(),
                "status", expense.getStatus().name());
        notificationService.sendNotification(creator.getUser().getId(), title, content, NotificationType.GENERAL, null,
                metadata);
    }

    private void notifyBudgetLowIfNeeded(PreparationTask task, BudgetCategory category) {
        BigDecimal allocated = zeroIfNull(category.getAllocatedAmount());
        if (allocated.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        BigDecimal remaining = allocated.subtract(zeroIfNull(category.getUsedAmount()));
        BigDecimal threshold = allocated.multiply(new BigDecimal("0.10"));
        if (remaining.compareTo(threshold) > 0) {
            return;
        }

        String title = "Ngân sách sắp cạn";
        String content = "Hạng mục: " + category.getName() + " | Còn lại: " + remaining;
        Map<String, Object> metadata = Map.of(
                "activityId", task.getActivity() != null ? task.getActivity().getId() : null,
                "taskId", task.getId(),
                "categoryId", category.getId(),
                "remaining", remaining);

        Student leader = task.getOwner();
        if (leader != null && leader.getUser() != null) {
            notificationService.sendNotification(leader.getUser().getId(), title, content, NotificationType.GENERAL,
                    null, metadata);
        }

        List<User> admins = userRepository.findAllByRoleInAndIsDeletedFalse(List.of(Role.ADMIN, Role.MANAGER));
        List<Long> userIds = admins.stream().map(User::getId).toList();
        if (!userIds.isEmpty()) {
            notificationService.sendBulkNotification(userIds, title, content, NotificationType.GENERAL, null, metadata);
        }
    }

    private void writeAudit(User actor, String action, String entityType, Long entityId, String detail) {
        User resolved = actor;
        if (resolved == null) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null) {
                resolved = userRepository.findByUsernameAndIsDeletedFalse(authentication.getName()).orElse(null);
            }
        }
        if (resolved == null) {
            return;
        }
        AuditLog log = new AuditLog();
        log.setActor(resolved);
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setDetail(detail);
        auditLogRepository.save(log);
    }

    private BigDecimal zeroIfNull(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
