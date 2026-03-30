package vn.campuslife.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import vn.campuslife.entity.*;
import vn.campuslife.enumeration.AllocationAdjustmentStatus;
import vn.campuslife.enumeration.ExpenseStatus;
import vn.campuslife.enumeration.FundAdvanceStatus;
import vn.campuslife.enumeration.NotificationType;
import vn.campuslife.enumeration.PreparationTaskMemberRole;
import vn.campuslife.enumeration.Role;
import vn.campuslife.exception.BadRequestException;
import vn.campuslife.exception.FeatureNotEnabledException;
import vn.campuslife.exception.ForbiddenException;
import vn.campuslife.exception.InsufficientBudgetException;
import vn.campuslife.exception.OverBudgetException;
import vn.campuslife.exception.ResourceNotFoundException;
import vn.campuslife.model.preparation.*;
import vn.campuslife.repository.*;
import vn.campuslife.service.NotificationService;
import vn.campuslife.service.PreparationFinanceService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PreparationFinanceServiceImpl implements PreparationFinanceService {
    private final ActivityRepository activityRepository;
    private final ActivityOrganizerRepository activityOrganizerRepository;
    private final ActivityBudgetRepository activityBudgetRepository;
    private final BudgetCategoryRepository budgetCategoryRepository;
    private final PreparationTaskRepository preparationTaskRepository;
    private final PreparationTaskMemberRepository preparationTaskMemberRepository;
    private final ExpenseRepository expenseRepository;
    private final FundAdvanceRepository fundAdvanceRepository;
    private final AuditLogRepository auditLogRepository;
    private final TaskAllocationRepository taskAllocationRepository;
    private final AllocationAdjustmentRequestRepository allocationAdjustmentRequestRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    private static final String DEFAULT_WALLET_NAME = "Tổng";
    private static final String RESIDUAL_WALLET_NAME = "Khác";
    private static final Set<String> RESIDUAL_WALLET_NAME_NORMALIZED = Set.of("khác", "khac");
    private static final String AUDIT_TASK_THRESHOLD_80 = "TASK_ALLOCATED_USED_80";
    private static final String AUDIT_TASK_THRESHOLD_90 = "TASK_ALLOCATED_USED_90";
    private static final String AUDIT_TASK_THRESHOLD_100 = "TASK_ALLOCATED_USED_100";
    private static final String AUDIT_CATEGORY_LOW_10 = "CATEGORY_WALLET_LOW_10P";

    @Override
    @Transactional
    public ActivityBudgetDto upsertActivityBudget(Long activityId, UpsertActivityBudgetRequest request) {
        Activity activity = getActiveActivity(activityId);
        if (!activity.isHasPreparation()) {
            throw new FeatureNotEnabledException("Preparation feature is not enabled for this activity");
        }

        BigDecimal totalAmount = parseNonNegativeAmount(request.getTotalAmount(), "totalAmount");
        List<UpsertBudgetCategoryRequest> reqCats = request.getCategories() == null ? List.of()
                : request.getCategories();

        ActivityBudget budget = activityBudgetRepository.findByActivityId(activityId).orElseGet(ActivityBudget::new);
        budget.setActivity(activity);
        if (!reqCats.isEmpty()
                && reqCats.stream().anyMatch(c -> c == null || c.getName() == null || c.getName().trim().isEmpty())) {
            throw new BadRequestException("Category name is required");
        }
        budget.setTotalAmount(totalAmount);
        List<UpsertBudgetCategoryRequest> nonEmptyReq = reqCats.stream()
                .filter(c -> c != null && c.getName() != null && !c.getName().trim().isEmpty())
                .toList();
        Map<String, UpsertBudgetCategoryRequest> reqByName = nonEmptyReq.stream()
                .collect(Collectors.toMap(
                        c -> normalizeCategoryName(c.getName()),
                        c -> c,
                        (a, b) -> {
                            throw new BadRequestException("Duplicate category name: " + a.getName().trim());
                        }));

        BigDecimal existingResidualUsed = budget.getCategories().stream()
                .filter(c -> isResidualName(normalizeCategoryName(c.getName())))
                .map(BudgetCategory::getUsedAmount)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, BigDecimal> desiredAllocatedByName = buildDesiredCategoryAllocation(
                totalAmount,
                reqByName,
                existingResidualUsed);

        Set<String> desiredNames = desiredAllocatedByName.keySet();

        budget.getCategories().removeIf(
                c -> !desiredNames.contains(normalizeCategoryName(c.getName()))
                        && zeroIfNull(c.getUsedAmount()).compareTo(BigDecimal.ZERO) == 0);

        List<String> cannotRemove = budget.getCategories().stream()
                .filter(c -> !desiredNames.contains(normalizeCategoryName(c.getName()))
                        && zeroIfNull(c.getUsedAmount()).compareTo(BigDecimal.ZERO) > 0)
                .map(BudgetCategory::getName)
                .toList();
        if (!cannotRemove.isEmpty()) {
            throw new BadRequestException(
                    "Cannot remove categories with used amount: " + String.join(", ", cannotRemove));
        }

        for (Map.Entry<String, BigDecimal> entry : desiredAllocatedByName.entrySet()) {
            String normalized = entry.getKey();
            BigDecimal allocated = entry.getValue();
            String displayName = resolveDesiredDisplayName(reqByName.get(normalized), normalized);

            BudgetCategory existing = budget.getCategories().stream()
                    .filter(c -> normalizeCategoryName(c.getName()).equals(normalized))
                    .findFirst()
                    .orElse(null);
            if (existing == null) {
                BudgetCategory created = new BudgetCategory();
                created.setActivityBudget(budget);
                created.setName(displayName);
                created.setAllocatedAmount(allocated);
                budget.getCategories().add(created);
            } else {
                if (allocated.compareTo(zeroIfNull(existing.getUsedAmount())) < 0) {
                    throw new BadRequestException(
                            "Allocated amount cannot be less than used amount for category: " + existing.getName());
                }
                existing.setName(displayName);
                existing.setAllocatedAmount(allocated);
            }
        }

        ActivityBudget saved = activityBudgetRepository.save(budget);
        writeAudit(null, "UPSERT_BUDGET", "ActivityBudget", saved.getId(), "activityId=" + activityId);
        return toActivityBudgetDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ActivityBudgetDto getActivityBudget(Long activityId) {
        Activity activity = getActiveActivity(activityId);
        if (!activity.isHasPreparation()) {
            throw new FeatureNotEnabledException("Preparation feature is not enabled for this activity");
        }
        ActivityBudget budget = activityBudgetRepository.findByActivityId(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("ActivityBudget not found"));
        return toActivityBudgetDto(budget);
    }

    @Override
    @Transactional
    public PreparationTaskDto allocateTaskAmount(Long taskId, AllocateTaskAmountRequest request) {
        PreparationTask task = preparationTaskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        requirePreparationEnabledForTask(task);
        if (!task.isFinancial()) {
            throw new BadRequestException("Task is not financial");
        }
        BigDecimal newAllocated = parseNonNegativeAmount(request.getAllocatedAmount(), "allocatedAmount");

        if (task.getActivity() == null || task.getActivity().getId() == null) {
            throw new BadRequestException("Task has no activity");
        }
        ActivityBudget activityBudget = activityBudgetRepository.findByActivityId(task.getActivity().getId())
                .orElseThrow(() -> new BadRequestException("No ActivityBudget assigned"));
        BudgetCategory category = budgetCategoryRepository
                .findByIdAndActivityBudgetActivityId(request.getCategoryId(), activityBudget.getActivity().getId())
                .orElseThrow(() -> new BadRequestException("Invalid category for this activity"));

        TaskAllocation allocation = taskAllocationRepository.findByTaskIdAndCategoryId(taskId, category.getId())
                .orElseGet(TaskAllocation::new);
        BigDecimal oldAmount = allocation.getId() == null ? BigDecimal.ZERO : zeroIfNull(allocation.getAmount());

        BigDecimal totalAllocatedOfCategory = taskAllocationRepository.sumAmountByCategoryId(category.getId());
        BigDecimal newTotalAllocatedOfCategory = totalAllocatedOfCategory.subtract(oldAmount).add(newAllocated);
        if (newTotalAllocatedOfCategory.compareTo(zeroIfNull(category.getAllocatedAmount())) > 0) {
            throw new InsufficientBudgetException("Insufficient category wallet remaining for allocation");
        }

        allocation.setTask(task);
        allocation.setCategory(category);
        allocation.setAmount(newAllocated);
        taskAllocationRepository.save(allocation);

        BigDecimal newTotalAllocatedForTask = taskAllocationRepository.sumAmountByTaskId(taskId);
        BigDecimal committed = expenseRepository.sumCommittedAmountByTaskId(taskId);
        if (newTotalAllocatedForTask.compareTo(committed) < 0) {
            throw new BadRequestException("Allocated amount cannot be less than committed spent");
        }

        task.setAllocatedAmount(newTotalAllocatedForTask);
        PreparationTask saved = preparationTaskRepository.save(task);
        writeAudit(null, "ALLOCATE_TASK_AMOUNT", "PreparationTask", saved.getId(),
                "categoryId=" + category.getId() + ",allocatedAmount=" + newAllocated);
        return toTaskDto(saved);
    }

    @Override
    @Transactional
    public void addTaskMember(Long taskId, Long studentId) {
        PreparationTask task = preparationTaskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        requirePreparationEnabledForTask(task);
        Long activityId = task.getActivity().getId();
        Student student = studentRepository.findByIdAndIsDeletedFalse(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        if (!activityOrganizerRepository.existsByActivityIdAndStudentId(activityId, studentId)) {
            throw new BadRequestException("Student is not an organizer of this activity");
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!isAdminOrManager(authentication)) {
            Long actorStudentId = getStudentIdByUsername(authentication != null ? authentication.getName() : null);
            if (actorStudentId == null
                    || !activityOrganizerRepository.existsByActivityIdAndStudentId(activityId, actorStudentId)) {
                throw new ForbiddenException("Organizer permission required");
            }
        }

        if (task.getOwner() != null && task.getOwner().getId().equals(studentId)) {
            return;
        }

        if (preparationTaskMemberRepository.existsByTaskIdAndStudentId(taskId, studentId)) {
            return;
        }

        PreparationTaskMember member = new PreparationTaskMember();
        member.setTask(task);
        member.setStudent(student);
        member.setRole(PreparationTaskMemberRole.MEMBER);
        preparationTaskMemberRepository.save(member);
        writeAudit(null, "ADD_TASK_MEMBER", "PreparationTask", taskId, "studentId=" + studentId);
    }

    @Override
    @Transactional
    public FundAdvanceDto requestFundAdvance(Long taskId, CreateFundAdvanceRequest request, String username) {
        PreparationTask task = preparationTaskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        requirePreparationEnabledForTask(task);
        if (!task.isFinancial()) {
            throw new BadRequestException("Task is not financial");
        }

        Student requester = studentRepository.findByUserUsernameAndIsDeletedFalse(username)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        Student student = studentRepository.findByIdAndIsDeletedFalse(request.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        Long activityId = task.getActivity().getId();
        if (!activityOrganizerRepository.existsByActivityIdAndStudentId(activityId, student.getId())) {
            throw new BadRequestException("Student is not an organizer of this activity");
        }
        boolean isLeader = task.getOwner() != null && task.getOwner().getId().equals(student.getId());
        boolean isMember = preparationTaskMemberRepository.existsByTaskIdAndStudentId(taskId, student.getId());
        if (!isLeader && !isMember) {
            throw new BadRequestException("Student is not a member of this task");
        }

        BigDecimal amount = parsePositiveAmount(request.getAmount(), "amount");

        BudgetCategory category = budgetCategoryRepository
                .findByIdAndActivityBudgetActivityId(request.getCategoryId(), activityId)
                .orElseThrow(() -> new BadRequestException("Invalid category for this activity"));

        boolean hasUnsettled = fundAdvanceRepository
                .existsByTaskActivityIdAndStudentIdAndStatusInAndRemainingAmountGreaterThan(
                        activityId,
                        student.getId(),
                        java.util.Set.of(FundAdvanceStatus.HOLDING),
                        BigDecimal.ZERO);
        if (hasUnsettled) {
            throw new BadRequestException("Student has unsettled fund advance in this activity");
        }

        TaskAllocation allocation = taskAllocationRepository.findByTaskIdAndCategoryId(taskId, category.getId())
                .orElse(null);
        BigDecimal allocationAmount = allocation != null ? zeroIfNull(allocation.getAmount()) : BigDecimal.ZERO;
        BigDecimal approvedInTaskCategory = expenseRepository.sumApprovedAmountByTaskIdAndCategoryId(taskId,
                category.getId());
        BigDecimal holdingInTaskCategory = fundAdvanceRepository
                .findByTaskIdOrderByCreatedAtDesc(taskId)
                .stream()
                .filter(fa -> fa.getStatus() == FundAdvanceStatus.HOLDING)
                .filter(fa -> fa.getCategory() != null && category.getId().equals(fa.getCategory().getId()))
                .map(FundAdvance::getRemainingAmount)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (approvedInTaskCategory.add(holdingInTaskCategory).add(amount).compareTo(allocationAmount) > 0) {
            throw new InsufficientBudgetException("Insufficient task allocation in selected category for fund advance");
        }

        BigDecimal holdingInCategory = fundAdvanceRepository.sumHoldingByCategoryId(category.getId());
        BigDecimal cashAvailable = zeroIfNull(category.getAllocatedAmount())
                .subtract(zeroIfNull(category.getUsedAmount()))
                .subtract(zeroIfNull(holdingInCategory));
        if (cashAvailable.compareTo(amount) < 0) {
            throw new InsufficientBudgetException("Insufficient wallet cash remaining for fund advance");
        }

        FundAdvance advance = new FundAdvance();
        advance.setTask(task);
        advance.setCategory(category);
        advance.setStudent(student);
        advance.setRequestedBy(requester);
        advance.setAmount(amount);
        advance.setRemainingAmount(BigDecimal.ZERO);
        advance.setStatus(FundAdvanceStatus.REQUESTED);

        FundAdvance saved = fundAdvanceRepository.save(advance);
        writeAudit(null, "REQUEST_FUND_ADVANCE", "FundAdvance", saved.getId(),
                "taskId=" + taskId + ",studentId=" + student.getId() + ",amount=" + amount);
        return toFundAdvanceDto(saved);
    }

    @Override
    @Transactional
    public FundAdvanceDto adminDecisionFundAdvance(Long fundAdvanceId, boolean approved, String username) {
        User actor = userRepository.findByUsernameAndIsDeletedFalse(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        FundAdvance advance = fundAdvanceRepository.findById(fundAdvanceId)
                .orElseThrow(() -> new ResourceNotFoundException("FundAdvance not found"));
        if (advance.getStatus() != FundAdvanceStatus.REQUESTED) {
            throw new BadRequestException("FundAdvance is not requested");
        }
        PreparationTask task = advance.getTask();
        requirePreparationEnabledForTask(task);
        if (task == null || task.getActivity() == null || task.getActivity().getId() == null) {
            throw new BadRequestException("FundAdvance has no activity");
        }
        Long activityId = task.getActivity().getId();
        Student student = advance.getStudent();
        if (student == null || student.getId() == null) {
            throw new BadRequestException("FundAdvance has no student");
        }

        boolean hasUnsettled = fundAdvanceRepository
                .existsByTaskActivityIdAndStudentIdAndStatusInAndRemainingAmountGreaterThan(
                        activityId,
                        student.getId(),
                        java.util.Set.of(FundAdvanceStatus.HOLDING),
                        BigDecimal.ZERO);
        if (approved && hasUnsettled) {
            throw new BadRequestException("Student has unsettled fund advance in this activity");
        }

        if (approved) {
            BudgetCategory category = advance.getCategory();
            if (category == null || category.getId() == null) {
                throw new BadRequestException("FundAdvance has no category");
            }
            BigDecimal holdingInCategory = fundAdvanceRepository.sumHoldingByCategoryId(category.getId());
            BigDecimal cashAvailable = zeroIfNull(category.getAllocatedAmount())
                    .subtract(zeroIfNull(category.getUsedAmount()))
                    .subtract(zeroIfNull(holdingInCategory));
            if (cashAvailable.compareTo(zeroIfNull(advance.getAmount())) < 0) {
                throw new InsufficientBudgetException("Insufficient wallet cash remaining for fund advance");
            }
            advance.setStatus(FundAdvanceStatus.HOLDING);
            advance.setRemainingAmount(zeroIfNull(advance.getAmount()));
        } else {
            advance.setStatus(FundAdvanceStatus.REJECTED);
            advance.setRemainingAmount(BigDecimal.ZERO);
        }
        advance.setDecidedAt(java.time.LocalDateTime.now());
        advance.setDecidedBy(actor);
        FundAdvance saved = fundAdvanceRepository.save(advance);
        writeAudit(actor, "ADMIN_DECISION_FUND_ADVANCE", "FundAdvance", saved.getId(), "approved=" + approved);
        return toFundAdvanceDto(saved);
    }

    @Override
    @Transactional
    public FundAdvanceDto adminReturnFundAdvance(Long fundAdvanceId, String username) {
        User actor = userRepository.findByUsernameAndIsDeletedFalse(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        FundAdvance advance = fundAdvanceRepository.findById(fundAdvanceId)
                .orElseThrow(() -> new ResourceNotFoundException("FundAdvance not found"));
        if (advance.getStatus() != FundAdvanceStatus.HOLDING) {
            throw new BadRequestException("FundAdvance is not holding");
        }
        requirePreparationEnabledForTask(advance.getTask());
        advance.setRemainingAmount(BigDecimal.ZERO);
        advance.setStatus(FundAdvanceStatus.SETTLED);
        advance.setDecidedAt(java.time.LocalDateTime.now());
        advance.setDecidedBy(actor);
        FundAdvance saved = fundAdvanceRepository.save(advance);
        writeAudit(actor, "RETURN_FUND_ADVANCE", "FundAdvance", saved.getId(), "returned=true");
        return toFundAdvanceDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FundAdvanceSourceSuggestionDto> suggestFundAdvanceSources(Long taskId, String amount) {
        PreparationTask task = preparationTaskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        requirePreparationEnabledForTask(task);
        if (!task.isFinancial()) {
            throw new BadRequestException("Task is not financial");
        }
        if (task.getActivity() == null || task.getActivity().getId() == null) {
            throw new BadRequestException("Task has no activity");
        }
        BigDecimal target = amount == null || amount.isBlank() ? null : parsePositiveAmount(amount, "amount");

        Long activityId = task.getActivity().getId();
        ActivityBudget budget = activityBudgetRepository.findByActivityId(activityId)
                .orElseThrow(() -> new BadRequestException("No ActivityBudget assigned"));

        List<TaskAllocation> allocations = taskAllocationRepository.findByTaskIdOrderByCreatedAtAsc(taskId);
        if (allocations.isEmpty()) {
            return List.of();
        }

        return allocations.stream()
                .map(a -> {
                    BudgetCategory c = a.getCategory();
                    if (c == null || c.getId() == null) {
                        return null;
                    }
                    BudgetCategory category = budgetCategoryRepository
                            .findByIdAndActivityBudgetActivityId(c.getId(), activityId)
                            .orElse(null);
                    if (category == null) {
                        return null;
                    }

                    BigDecimal allocationAmount = zeroIfNull(a.getAmount());
                    BigDecimal approved = expenseRepository.sumApprovedAmountByTaskIdAndCategoryId(taskId, category.getId());
                    BigDecimal holdingInTaskCategory = fundAdvanceRepository.sumHoldingByTaskIdAndCategoryId(taskId, category.getId());
                    BigDecimal allocationRemaining = allocationAmount.subtract(zeroIfNull(approved)).subtract(zeroIfNull(holdingInTaskCategory));
                    if (allocationRemaining.compareTo(BigDecimal.ZERO) < 0) {
                        allocationRemaining = BigDecimal.ZERO;
                    }

                    BigDecimal holdingInCategory = fundAdvanceRepository.sumHoldingByCategoryId(category.getId());
                    BigDecimal remaining = zeroIfNull(category.getAllocatedAmount()).subtract(zeroIfNull(category.getUsedAmount()));
                    BigDecimal cashAvailable = remaining.subtract(zeroIfNull(holdingInCategory));
                    if (cashAvailable.compareTo(BigDecimal.ZERO) < 0) {
                        cashAvailable = BigDecimal.ZERO;
                    }

                    BigDecimal max = allocationRemaining.min(cashAvailable);
                    return new FundAdvanceSourceSuggestionDto(
                            category.getId(),
                            category.getName(),
                            allocationRemaining,
                            cashAvailable,
                            max);
                })
                .filter(v -> v != null)
                .filter(v -> v.getMaxAdvanceAmount().compareTo(BigDecimal.ZERO) > 0)
                .filter(v -> target == null || v.getMaxAdvanceAmount().compareTo(target) >= 0)
                .sorted(Comparator.comparing(FundAdvanceSourceSuggestionDto::getMaxAdvanceAmount).reversed())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FundAdvanceDto> listFundAdvancesByTask(Long taskId) {
        PreparationTask task = preparationTaskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        requirePreparationEnabledForTask(task);
        return fundAdvanceRepository.findByTaskIdOrderByCreatedAtDesc(taskId).stream()
                .map(this::toFundAdvanceDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FundAdvanceDebtDto> listFundAdvanceDebts(Long activityId, Long studentId) {
        Activity activity = getActiveActivity(activityId);
        if (!activity.isHasPreparation()) {
            throw new FeatureNotEnabledException("Preparation feature is not enabled for this activity");
        }
        List<Student> students;
        if (studentId != null) {
            Student s = studentRepository.findByIdAndIsDeletedFalse(studentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
            students = List.of(s);
        } else {
            students = activityOrganizerRepository.findByActivityId(activityId).stream()
                    .map(ActivityOrganizer::getStudent)
                    .filter(s -> s != null && s.getId() != null)
                    .toList();
        }
        Map<Long, BigDecimal> holdingByStudentId = fundAdvanceRepository.sumHoldingByActivity(activityId).stream()
                .collect(Collectors.toMap(
                        FundAdvanceRepository.FundAdvanceHoldingView::getStudentId,
                        FundAdvanceRepository.FundAdvanceHoldingView::getHoldingAmount));

        return students.stream()
                .map(s -> new FundAdvanceDebtDto(
                        s.getId(),
                        s.getFullName(),
                        holdingByStudentId.getOrDefault(s.getId(), BigDecimal.ZERO)))
                .filter(d -> d.getHoldingAmount().compareTo(BigDecimal.ZERO) > 0)
                .toList();
    }

    @Override
    @Transactional
    public ExpenseDto createExpense(CreateExpenseRequest request, String username) {
        Student creator = studentRepository.findByUserUsernameAndIsDeletedFalse(username)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        PreparationTask task = preparationTaskRepository.findById(request.getTaskId())
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        requirePreparationEnabledForTask(task);
        if (!task.isFinancial()) {
            throw new BadRequestException("Task is not financial");
        }
        if (task.getActivity() == null) {
            throw new BadRequestException("Task has no activity");
        }
        if (!activityOrganizerRepository.existsByActivityIdAndStudentId(task.getActivity().getId(), creator.getId())) {
            throw new ForbiddenException("Organizer permission required");
        }

        BigDecimal amount = parsePositiveAmount(request.getAmount(), "amount");
        BigDecimal committed = expenseRepository.sumCommittedAmountByTaskId(task.getId());
        BigDecimal newCommitted = committed.add(amount);
        BigDecimal allocated = zeroIfNull(task.getAllocatedAmount());
        if (newCommitted.compareTo(allocated) > 0) {
            BigDecimal requiredAdditional = newCommitted.subtract(allocated);
            throw new OverBudgetException("Expense exceeds task allocated amount",
                    new OverBudgetInfoDto(
                            task.getId(),
                            requiredAdditional,
                            allocated,
                            committed,
                            suggestAllocationSources(task.getActivity().getId())));
        }

        BudgetCategory category = budgetCategoryRepository
                .findByIdAndActivityBudgetActivityId(request.getCategoryId(), task.getActivity().getId())
                .orElseThrow(() -> new BadRequestException("Invalid category for this activity"));

        Expense expense = new Expense();
        expense.setTask(task);
        expense.setCategory(category);
        expense.setAmount(amount);
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
        if (expense.getTask() != null) {
            requirePreparationEnabledForTask(expense.getTask());
        }
        if (expense.getTask() != null && expense.getTask().getActivity() != null
                && !activityOrganizerRepository.existsByActivityIdAndStudentId(expense.getTask().getActivity().getId(),
                        leader.getId())) {
            throw new ForbiddenException("Organizer permission required");
        }
        if (expense.getTask() == null || expense.getTask().getOwner() == null) {
            throw new BadRequestException("Expense has no leader");
        }
        boolean isLeader = preparationTaskMemberRepository.existsByTaskIdAndStudentIdAndRole(
                expense.getTask().getId(),
                leader.getId(),
                PreparationTaskMemberRole.LEADER);
        if (!isLeader && !expense.getTask().getOwner().getId().equals(leader.getId())) {
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
        if (expense.getTask() != null) {
            requirePreparationEnabledForTask(expense.getTask());
        }
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

        BudgetCategory category = expense.getCategory();
        BigDecimal remainingCategory = zeroIfNull(category.getAllocatedAmount())
                .subtract(zeroIfNull(category.getUsedAmount()));
        if (remainingCategory.compareTo(zeroIfNull(expense.getAmount())) < 0) {
            throw new InsufficientBudgetException("Insufficient category budget remaining");
        }

        ensureSufficientFundAdvance(task.getId(), expense.getCreatedBy().getId(), category.getId(),
                expense.getAmount());

        expense.setStatus(ExpenseStatus.APPROVED);
        Expense saved = expenseRepository.save(expense);

        deductFromFundAdvances(task.getId(), expense.getCreatedBy().getId(), category.getId(), expense.getAmount());
        category.setUsedAmount(zeroIfNull(category.getUsedAmount()).add(zeroIfNull(expense.getAmount())));
        budgetCategoryRepository.save(category);

        writeAudit(actor, "ADMIN_DECISION", "Expense", saved.getId(), "approved=true");

        notifyCreatorForDecision(saved);
        notifyBudgetLowIfNeeded(actor, task, category);
        notifyTaskThresholdIfNeeded(actor, task, newSpentTask);

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
    @Transactional
    public AllocationAdjustmentRequestDto createAllocationAdjustmentRequest(Long taskId,
            CreateAllocationAdjustmentRequest request, String username) {
        Student creator = studentRepository.findByUserUsernameAndIsDeletedFalse(username)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        PreparationTask task = preparationTaskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        requirePreparationEnabledForTask(task);
        if (!task.isFinancial()) {
            throw new BadRequestException("Task is not financial");
        }
        if (task.getActivity() == null || task.getActivity().getId() == null) {
            throw new BadRequestException("Task has no activity");
        }
        if (!activityOrganizerRepository.existsByActivityIdAndStudentId(task.getActivity().getId(), creator.getId())) {
            throw new ForbiddenException("Organizer permission required");
        }
        boolean isOwner = task.getOwner() != null && task.getOwner().getId().equals(creator.getId());
        boolean isMember = preparationTaskMemberRepository.existsByTaskIdAndStudentId(taskId, creator.getId());
        if (!isOwner && !isMember) {
            throw new ForbiddenException("Task member permission required");
        }

        BigDecimal amount = parsePositiveAmount(request.getAmount(), "amount");
        BudgetCategory preferred = null;
        if (request.getPreferredCategoryId() != null) {
            preferred = budgetCategoryRepository.findByIdAndActivityBudgetActivityId(
                    request.getPreferredCategoryId(),
                    task.getActivity().getId())
                    .orElseThrow(() -> new BadRequestException("Invalid category for this activity"));
        }

        AllocationAdjustmentRequest entity = new AllocationAdjustmentRequest();
        entity.setTask(task);
        entity.setRequestedBy(creator);
        entity.setAmount(amount);
        entity.setPreferredCategory(preferred);
        entity.setStatus(AllocationAdjustmentStatus.PENDING);
        AllocationAdjustmentRequest saved = allocationAdjustmentRequestRepository.save(entity);
        writeAudit(null, "CREATE_ALLOCATION_ADJUSTMENT", "AllocationAdjustmentRequest", saved.getId(),
                "taskId=" + taskId + ",amount=" + amount);
        return toAllocationAdjustmentDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AllocationAdjustmentRequestDto> listAllocationAdjustmentRequests(Long activityId,
            AllocationAdjustmentStatus status) {
        Activity activity = getActiveActivity(activityId);
        if (!activity.isHasPreparation()) {
            throw new FeatureNotEnabledException("Preparation feature is not enabled for this activity");
        }
        List<AllocationAdjustmentRequest> list = status == null
                ? allocationAdjustmentRequestRepository.findByTaskActivityIdOrderByCreatedAtDesc(activityId)
                : allocationAdjustmentRequestRepository.findByTaskActivityIdAndStatusOrderByCreatedAtDesc(activityId,
                        status);
        return list.stream().map(this::toAllocationAdjustmentDto).toList();
    }

    @Override
    @Transactional
    public AllocationAdjustmentRequestDto adminDecisionAllocationAdjustment(Long requestId, boolean approved,
            Long categoryId, String username) {
        User actor = userRepository.findByUsernameAndIsDeletedFalse(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        AllocationAdjustmentRequest req = allocationAdjustmentRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Allocation adjustment request not found"));
        if (req.getStatus() != AllocationAdjustmentStatus.PENDING) {
            throw new BadRequestException("Request is not pending");
        }
        if (req.getTask() == null) {
            throw new BadRequestException("Request has no task");
        }
        PreparationTask task = req.getTask();
        requirePreparationEnabledForTask(task);

        if (!approved) {
            req.setStatus(AllocationAdjustmentStatus.REJECTED);
            req.setDecidedAt(java.time.LocalDateTime.now());
            req.setDecidedBy(actor);
            AllocationAdjustmentRequest saved = allocationAdjustmentRequestRepository.save(req);
            writeAudit(actor, "ADMIN_DECISION_ALLOCATION_ADJUSTMENT", "AllocationAdjustmentRequest", saved.getId(),
                    "approved=false");
            return toAllocationAdjustmentDto(saved);
        }

        if (categoryId == null) {
            throw new BadRequestException("Category ID is required");
        }
        if (task.getActivity() == null || task.getActivity().getId() == null) {
            throw new BadRequestException("Task has no activity");
        }
        BudgetCategory category = budgetCategoryRepository
                .findByIdAndActivityBudgetActivityId(categoryId, task.getActivity().getId())
                .orElseThrow(() -> new BadRequestException("Invalid category for this activity"));

        applyAllocationDelta(task, category, zeroIfNull(req.getAmount()));

        req.setStatus(AllocationAdjustmentStatus.APPROVED);
        req.setDecidedAt(java.time.LocalDateTime.now());
        req.setDecidedBy(actor);
        AllocationAdjustmentRequest saved = allocationAdjustmentRequestRepository.save(req);
        writeAudit(actor, "ADMIN_DECISION_ALLOCATION_ADJUSTMENT", "AllocationAdjustmentRequest", saved.getId(),
                "approved=true,categoryId=" + category.getId() + ",amount=" + req.getAmount());
        return toAllocationAdjustmentDto(saved);
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

        Map<Long, BigDecimal> holdingByCategoryId = fundAdvanceRepository.sumHoldingByCategoryInActivity(activityId)
                .stream()
                .collect(Collectors.toMap(
                        FundAdvanceRepository.FundAdvanceHoldingByCategoryView::getCategoryId,
                        FundAdvanceRepository.FundAdvanceHoldingByCategoryView::getHoldingAmount));

        List<BudgetCategoryDto> categories = budget.getCategories().stream()
                .sorted(Comparator.comparing(BudgetCategory::getId, Comparator.nullsLast(Long::compareTo)))
                .map(c -> {
                    BigDecimal allocated = zeroIfNull(c.getAllocatedAmount());
                    BigDecimal allocatedToTasks = taskAllocationRepository.sumAmountByCategoryId(c.getId());
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

        List<PreparationTask> tasks = preparationTaskRepository.findByActivityIdOrderByDeadlineAscIdAsc(activityId);
        List<TaskOverBudgetDto> overBudget = tasks.stream()
                .filter(PreparationTask::isFinancial)
                .map(t -> {
                    BigDecimal spent = expenseRepository.sumApprovedAmountByTaskId(t.getId());
                    boolean over = false;
                    if (zeroIfNull(t.getAllocatedAmount()).compareTo(spent) < 0) {
                        over = true;
                    }
                    return over
                            ? new TaskOverBudgetDto(t.getId(), t.getTitle(), zeroIfNull(t.getAllocatedAmount()), spent)
                            : null;
                })
                .filter(v -> v != null)
                .toList();

        return new FinancialReportDto(activityId, zeroIfNull(budget.getTotalAmount()), categories, overBudget);
    }

    @Override
    @Transactional(readOnly = true)
    public FinanceOverviewReportDto getFinanceOverviewReport(Long activityId) {
        Activity activity = getActiveActivity(activityId);
        if (!activity.isHasPreparation()) {
            throw new FeatureNotEnabledException("Preparation feature is not enabled for this activity");
        }
        ActivityBudget budget = activityBudgetRepository.findByActivityId(activityId)
                .orElseThrow(() -> new BadRequestException("No ActivityBudget assigned"));

        ActivityBudgetDto budgetDto = toActivityBudgetDto(budget);
        BigDecimal totalBudget = zeroIfNull(budget.getTotalAmount());
        BigDecimal totalAllocatedToTasks = budgetDto.getCategories().stream()
                .map(BudgetCategoryDto::getAllocatedToTasksAmount)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalApprovedSpent = expenseRepository.sumApprovedAmountByActivityId(activityId);
        BigDecimal variance = totalAllocatedToTasks.subtract(zeroIfNull(totalApprovedSpent));

        List<PreparationTask> tasks = preparationTaskRepository.findByActivityIdOrderByDeadlineAscIdAsc(activityId);
        Map<Long, BigDecimal> approvedByTask = expenseRepository.sumApprovedSpentByTaskInActivity(activityId).stream()
                .collect(Collectors.toMap(ExpenseRepository.TaskApprovedSpentView::getTaskId,
                        ExpenseRepository.TaskApprovedSpentView::getApprovedSpent));
        Map<Long, BigDecimal> committedByTask = expenseRepository.sumCommittedSpentByTaskInActivity(activityId).stream()
                .collect(Collectors.toMap(ExpenseRepository.TaskCommittedSpentView::getTaskId,
                        ExpenseRepository.TaskCommittedSpentView::getCommittedAmount));

        List<TaskSpendStatusDto> taskDtos = tasks.stream()
                .filter(PreparationTask::isFinancial)
                .map(t -> {
                    BigDecimal allocated = zeroIfNull(t.getAllocatedAmount());
                    BigDecimal approved = approvedByTask.getOrDefault(t.getId(), BigDecimal.ZERO);
                    BigDecimal committed = committedByTask.getOrDefault(t.getId(), BigDecimal.ZERO);
                    Double percent = allocated.compareTo(BigDecimal.ZERO) > 0
                            ? approved.multiply(BigDecimal.valueOf(100))
                                    .divide(allocated, 2, RoundingMode.HALF_UP)
                                    .doubleValue()
                            : 0.0;
                    return new TaskSpendStatusDto(
                            t.getId(),
                            t.getTitle(),
                            allocated,
                            committed,
                            approved,
                            percent);
                })
                .toList();

        return new FinanceOverviewReportDto(
                activityId,
                totalBudget,
                totalAllocatedToTasks,
                zeroIfNull(totalApprovedSpent),
                variance,
                budgetDto.getCategories(),
                taskDtos);
    }

    @Override
    @Transactional(readOnly = true)
    public CashFlowReportDto getCashFlowReport(Long activityId) {
        Activity activity = getActiveActivity(activityId);
        if (!activity.isHasPreparation()) {
            throw new FeatureNotEnabledException("Preparation feature is not enabled for this activity");
        }
        ActivityBudget budget = activityBudgetRepository.findByActivityId(activityId)
                .orElseThrow(() -> new BadRequestException("No ActivityBudget assigned"));

        BigDecimal totalBudget = zeroIfNull(budget.getTotalAmount());
        BigDecimal approvedSpent = zeroIfNull(expenseRepository.sumApprovedAmountByActivityId(activityId));

        BigDecimal cashOutside = fundAdvanceRepository.sumHoldingByActivity(activityId).stream()
                .map(FundAdvanceRepository.FundAdvanceHoldingView::getHoldingAmount)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal cashInside = totalBudget.subtract(approvedSpent).subtract(cashOutside);
        if (cashInside.compareTo(BigDecimal.ZERO) < 0) {
            cashInside = BigDecimal.ZERO;
        }

        List<FundAdvanceDebtDto> debts = listFundAdvanceDebts(activityId, null);
        List<InvoiceStatusSummaryDto> invoiceSummary = expenseRepository.summarizeInvoiceStatusesByActivity(activityId)
                .stream()
                .map(v -> new InvoiceStatusSummaryDto(v.getStatus(), v.getCount(), v.getTotalAmount()))
                .toList();

        return new CashFlowReportDto(
                activityId,
                totalBudget,
                approvedSpent,
                cashOutside,
                cashInside,
                debts,
                invoiceSummary);
    }

    private Activity getActiveActivity(Long activityId) {
        return activityRepository.findByIdAndIsDeletedFalse(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Activity not found"));
    }

    private void requirePreparationEnabledForTask(PreparationTask task) {
        if (task == null || task.getActivity() == null || task.getActivity().getId() == null) {
            throw new BadRequestException("Task has no activity");
        }
        Activity activity = getActiveActivity(task.getActivity().getId());
        if (!activity.isHasPreparation()) {
            throw new FeatureNotEnabledException("Preparation feature is not enabled for this activity");
        }
    }

    private boolean isAdminOrManager(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()) || "ROLE_MANAGER".equals(a.getAuthority()));
    }

    private Long getStudentIdByUsername(String username) {
        if (username == null || username.isBlank()) {
            return null;
        }
        return studentRepository.findByUserUsernameAndIsDeletedFalse(username).map(Student::getId).orElse(null);
    }

    private String normalizeCategoryName(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().toLowerCase(Locale.ROOT);
    }

    private boolean isResidualName(String normalizedName) {
        return RESIDUAL_WALLET_NAME_NORMALIZED.contains(normalizedName);
    }

    private Map<String, BigDecimal> buildDesiredCategoryAllocation(
            BigDecimal totalAmount,
            Map<String, UpsertBudgetCategoryRequest> reqByName,
            BigDecimal existingResidualUsed) {
        if (reqByName.isEmpty()) {
            return Map.of(normalizeCategoryName(DEFAULT_WALLET_NAME), totalAmount);
        }

        BigDecimal sumNonResidual = reqByName.entrySet().stream()
                .filter(e -> !isResidualName(e.getKey()))
                .map(Map.Entry::getValue)
                .map(UpsertBudgetCategoryRequest::getAllocatedAmount)
                .map(v -> parseNonNegativeAmount(v, "categories.allocatedAmount"))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (sumNonResidual.compareTo(totalAmount) > 0) {
            throw new BadRequestException("Sum of category allocated amounts cannot exceed total budget");
        }

        BigDecimal computedResidual = totalAmount.subtract(sumNonResidual);
        if (computedResidual.compareTo(existingResidualUsed) < 0) {
            throw new BadRequestException("Residual category budget cannot be less than used amount");
        }

        Map<String, BigDecimal> desired = reqByName.entrySet().stream()
                .filter(e -> !isResidualName(e.getKey()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> parseNonNegativeAmount(e.getValue().getAllocatedAmount(), "categories.allocatedAmount")));

        if (computedResidual.compareTo(BigDecimal.ZERO) > 0 || existingResidualUsed.compareTo(BigDecimal.ZERO) > 0) {
            desired.put(normalizeCategoryName(RESIDUAL_WALLET_NAME), computedResidual);
        }

        return desired;
    }

    private String resolveDesiredDisplayName(UpsertBudgetCategoryRequest req, String normalized) {
        if (normalizeCategoryName(DEFAULT_WALLET_NAME).equals(normalized)) {
            return DEFAULT_WALLET_NAME;
        }
        if (isResidualName(normalized)) {
            return RESIDUAL_WALLET_NAME;
        }
        if (req == null || req.getName() == null || req.getName().trim().isEmpty()) {
            return normalized;
        }
        return req.getName().trim();
    }

    private ActivityBudgetDto toActivityBudgetDto(ActivityBudget budget) {
        Map<Long, BigDecimal> allocatedToTasksByCategory = taskAllocationRepository
                .sumAllocatedToTasksByActivity(budget.getActivity().getId())
                .stream()
                .collect(Collectors.toMap(
                        TaskAllocationRepository.CategoryAllocationSumView::getCategoryId,
                        TaskAllocationRepository.CategoryAllocationSumView::getAllocatedToTasksAmount));
        Map<Long, BigDecimal> holdingByCategoryId = fundAdvanceRepository
                .sumHoldingByCategoryInActivity(budget.getActivity().getId())
                .stream()
                .collect(Collectors.toMap(
                        FundAdvanceRepository.FundAdvanceHoldingByCategoryView::getCategoryId,
                        FundAdvanceRepository.FundAdvanceHoldingByCategoryView::getHoldingAmount));
        List<BudgetCategoryDto> cats = budget.getCategories().stream()
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
        Student requester = advance.getRequestedBy();
        BudgetCategory c = advance.getCategory();
        return new FundAdvanceDto(
                advance.getId(),
                advance.getTask() != null ? advance.getTask().getId() : null,
                c != null ? c.getId() : null,
                c != null ? c.getName() : null,
                s != null ? s.getId() : null,
                s != null ? s.getFullName() : null,
                requester != null ? requester.getId() : null,
                requester != null ? requester.getFullName() : null,
                advance.getAmount(),
                advance.getRemainingAmount(),
                advance.getStatus(),
                advance.getCreatedAt(),
                advance.getDecidedAt());
    }

    private void ensureSufficientFundAdvance(Long taskId, Long studentId, Long categoryId, BigDecimal amount) {
        List<FundAdvance> holding = fundAdvanceRepository.findByTaskIdAndStudentIdAndStatusOrderByCreatedAtAsc(taskId,
                studentId, FundAdvanceStatus.HOLDING);
        List<FundAdvance> filtered = holding.stream()
                .filter(fa -> fa.getCategory() == null
                        || (fa.getCategory().getId() != null && fa.getCategory().getId().equals(categoryId)))
                .toList();
        BigDecimal totalRemaining = filtered.stream()
                .map(FundAdvance::getRemainingAmount)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalRemaining.compareTo(zeroIfNull(amount)) < 0) {
            throw new InsufficientBudgetException("Insufficient fund advance remaining");
        }
    }

    private void deductFromFundAdvances(Long taskId, Long studentId, Long categoryId, BigDecimal amount) {
        BigDecimal left = zeroIfNull(amount);
        List<FundAdvance> holding = fundAdvanceRepository.findByTaskIdAndStudentIdAndStatusOrderByCreatedAtAsc(taskId,
                studentId, FundAdvanceStatus.HOLDING);
        List<FundAdvance> preferred = holding.stream()
                .filter(fa -> fa.getCategory() != null && fa.getCategory().getId() != null
                        && fa.getCategory().getId().equals(categoryId))
                .toList();
        List<FundAdvance> fallback = holding.stream()
                .filter(fa -> fa.getCategory() == null)
                .toList();

        for (FundAdvance fa : java.util.stream.Stream.concat(preferred.stream(), fallback.stream()).toList()) {
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
        String title = "Chi phí chờ leader duyệt";
        String content = "Nhiệm vụ: " + task.getTitle() + " | Số tiền: " + expense.getAmount();
        Map<String, Object> metadata = Map.of(
                "activityId", task.getActivity() != null ? task.getActivity().getId() : null,
                "taskId", task.getId(),
                "expenseId", expense.getId(),
                "status", expense.getStatus().name());
        List<Long> leaderUserIds = preparationTaskMemberRepository.findByTaskIdOrderByRoleAscCreatedAtAsc(task.getId())
                .stream()
                .filter(m -> m.getRole() == PreparationTaskMemberRole.LEADER)
                .map(PreparationTaskMember::getStudent)
                .map(Student::getUser)
                .filter(u -> u != null && u.getId() != null)
                .map(User::getId)
                .toList();
        if (!leaderUserIds.isEmpty()) {
            notificationService.sendBulkNotification(leaderUserIds, title, content, NotificationType.GENERAL, null,
                    metadata);
            return;
        }

        Student leader = task.getOwner();
        if (leader == null || leader.getUser() == null || leader.getUser().getId() == null) {
            return;
        }
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

    private void notifyBudgetLowIfNeeded(User actor, PreparationTask task, BudgetCategory category) {
        BigDecimal allocated = zeroIfNull(category.getAllocatedAmount());
        if (allocated.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        BigDecimal holding = fundAdvanceRepository.sumHoldingByCategoryId(category.getId());
        BigDecimal remaining = allocated.subtract(zeroIfNull(category.getUsedAmount()));
        BigDecimal cashAvailable = remaining.subtract(zeroIfNull(holding));
        BigDecimal threshold = allocated.multiply(new BigDecimal("0.10"));
        if (cashAvailable.compareTo(threshold) > 0) {
            return;
        }
        if (auditLogRepository.existsByActionAndEntityTypeAndEntityId(AUDIT_CATEGORY_LOW_10, "BudgetCategory",
                category.getId())) {
            return;
        }

        String title = "Ngân sách sắp cạn";
        String content = "Hạng mục: " + category.getName() + " | Còn lại: " + cashAvailable;
        Map<String, Object> metadata = Map.of(
                "activityId", task.getActivity() != null ? task.getActivity().getId() : null,
                "taskId", task.getId(),
                "categoryId", category.getId(),
                "remaining", cashAvailable);

        List<Long> leaderUserIds = preparationTaskMemberRepository.findByTaskIdOrderByRoleAscCreatedAtAsc(task.getId())
                .stream()
                .filter(m -> m.getRole() == PreparationTaskMemberRole.LEADER)
                .map(PreparationTaskMember::getStudent)
                .map(Student::getUser)
                .filter(u -> u != null && u.getId() != null)
                .map(User::getId)
                .toList();
        if (!leaderUserIds.isEmpty()) {
            notificationService.sendBulkNotification(leaderUserIds, title, content, NotificationType.GENERAL, null,
                    metadata);
        }

        List<User> admins = userRepository.findAllByRoleInAndIsDeletedFalse(List.of(Role.ADMIN, Role.MANAGER));
        List<Long> userIds = admins.stream().map(User::getId).toList();
        if (!userIds.isEmpty()) {
            notificationService.sendBulkNotification(userIds, title, content, NotificationType.GENERAL, null, metadata);
        }
        writeAudit(actor, AUDIT_CATEGORY_LOW_10, "BudgetCategory", category.getId(),
                "allocated=" + allocated + ",used=" + zeroIfNull(category.getUsedAmount()) + ",holding=" + holding
                        + ",remaining=" + cashAvailable);
    }

    private void notifyTaskThresholdIfNeeded(User actor, PreparationTask task, BigDecimal newApprovedSpent) {
        if (task == null || task.getId() == null) {
            return;
        }
        BigDecimal allocated = zeroIfNull(task.getAllocatedAmount());
        if (allocated.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        BigDecimal spent = zeroIfNull(newApprovedSpent);
        BigDecimal p80 = allocated.multiply(new BigDecimal("0.80"));
        BigDecimal p90 = allocated.multiply(new BigDecimal("0.90"));

        String action = null;
        int percent = 0;
        if (spent.compareTo(allocated) >= 0) {
            action = AUDIT_TASK_THRESHOLD_100;
            percent = 100;
        } else if (spent.compareTo(p90) >= 0) {
            action = AUDIT_TASK_THRESHOLD_90;
            percent = 90;
        } else if (spent.compareTo(p80) >= 0) {
            action = AUDIT_TASK_THRESHOLD_80;
            percent = 80;
        }
        if (action == null) {
            return;
        }
        if (auditLogRepository.existsByActionAndEntityTypeAndEntityId(action, "PreparationTask", task.getId())) {
            return;
        }

        String title = "Cảnh báo ngưỡng chi tiêu";
        String content = "Task: " + task.getTitle() + " | Đã dùng ≥ " + percent + "%";
        Map<String, Object> metadata = Map.of(
                "activityId", task.getActivity() != null ? task.getActivity().getId() : null,
                "taskId", task.getId(),
                "allocatedAmount", allocated,
                "approvedSpent", spent,
                "threshold", percent);

        List<Long> leaderUserIds = preparationTaskMemberRepository.findByTaskIdOrderByRoleAscCreatedAtAsc(task.getId())
                .stream()
                .filter(m -> m.getRole() == PreparationTaskMemberRole.LEADER)
                .map(PreparationTaskMember::getStudent)
                .map(Student::getUser)
                .filter(u -> u != null && u.getId() != null)
                .map(User::getId)
                .toList();
        if (!leaderUserIds.isEmpty()) {
            notificationService.sendBulkNotification(leaderUserIds, title, content, NotificationType.GENERAL, null,
                    metadata);
        }

        List<User> admins = userRepository.findAllByRoleInAndIsDeletedFalse(List.of(Role.ADMIN, Role.MANAGER));
        List<Long> adminIds = admins.stream().map(User::getId).toList();
        if (!adminIds.isEmpty()) {
            notificationService.sendBulkNotification(adminIds, title, content, NotificationType.GENERAL, null,
                    metadata);
        }
        writeAudit(actor, action, "PreparationTask", task.getId(),
                "allocated=" + allocated + ",approvedSpent=" + spent + ",threshold=" + percent);
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

    private BigDecimal parseNonNegativeAmount(String raw, String fieldName) {
        if (raw == null || raw.isBlank()) {
            throw new BadRequestException(fieldName + " is required");
        }
        try {
            BigDecimal v = new BigDecimal(raw.trim());
            if (v.compareTo(BigDecimal.ZERO) < 0) {
                throw new BadRequestException(fieldName + " must be >= 0");
            }
            return v;
        } catch (NumberFormatException e) {
            throw new BadRequestException(fieldName + " is invalid");
        }
    }

    private BigDecimal parsePositiveAmount(String raw, String fieldName) {
        BigDecimal v = parseNonNegativeAmount(raw, fieldName);
        if (v.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException(fieldName + " must be > 0");
        }
        return v;
    }

    private BigDecimal zeroIfNull(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private void applyAllocationDelta(PreparationTask task, BudgetCategory category, BigDecimal delta) {
        if (delta == null || delta.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Amount must be > 0");
        }
        TaskAllocation allocation = taskAllocationRepository.findByTaskIdAndCategoryId(task.getId(), category.getId())
                .orElseGet(TaskAllocation::new);
        BigDecimal old = allocation.getId() == null ? BigDecimal.ZERO : zeroIfNull(allocation.getAmount());
        BigDecimal newAmount = old.add(delta);

        BigDecimal totalAllocatedOfCategory = taskAllocationRepository.sumAmountByCategoryId(category.getId());
        BigDecimal newTotalAllocatedOfCategory = totalAllocatedOfCategory.subtract(old).add(newAmount);
        if (newTotalAllocatedOfCategory.compareTo(zeroIfNull(category.getAllocatedAmount())) > 0) {
            throw new InsufficientBudgetException("Insufficient category wallet remaining for allocation");
        }

        allocation.setTask(task);
        allocation.setCategory(category);
        allocation.setAmount(newAmount);
        taskAllocationRepository.save(allocation);

        BigDecimal newTotalAllocatedForTask = taskAllocationRepository.sumAmountByTaskId(task.getId());
        BigDecimal committed = expenseRepository.sumCommittedAmountByTaskId(task.getId());
        if (newTotalAllocatedForTask.compareTo(committed) < 0) {
            throw new BadRequestException("Allocated amount cannot be less than committed spent");
        }
        task.setAllocatedAmount(newTotalAllocatedForTask);
        preparationTaskRepository.save(task);
    }

    private AllocationAdjustmentRequestDto toAllocationAdjustmentDto(AllocationAdjustmentRequest req) {
        PreparationTask task = req.getTask();
        Student requestedBy = req.getRequestedBy();
        BudgetCategory preferred = req.getPreferredCategory();
        Long activityId = task != null && task.getActivity() != null ? task.getActivity().getId() : null;
        return new AllocationAdjustmentRequestDto(
                req.getId(),
                activityId,
                task != null ? task.getId() : null,
                req.getAmount(),
                req.getStatus(),
                requestedBy != null ? requestedBy.getId() : null,
                requestedBy != null ? requestedBy.getFullName() : null,
                preferred != null ? preferred.getId() : null,
                preferred != null ? preferred.getName() : null,
                req.getCreatedAt(),
                req.getDecidedAt(),
                req.getDecidedBy() != null ? req.getDecidedBy().getId() : null);
    }

    private List<AllocationSourceSuggestionDto> suggestAllocationSources(Long activityId) {
        if (activityId == null) {
            return List.of();
        }
        ActivityBudget budget = activityBudgetRepository.findByActivityId(activityId).orElse(null);
        if (budget == null) {
            return List.of();
        }
        Map<Long, BigDecimal> allocatedToTasksByCategory = taskAllocationRepository
                .sumAllocatedToTasksByActivity(activityId)
                .stream()
                .collect(Collectors.toMap(
                        TaskAllocationRepository.CategoryAllocationSumView::getCategoryId,
                        TaskAllocationRepository.CategoryAllocationSumView::getAllocatedToTasksAmount));
        return budget.getCategories().stream()
                .map(c -> {
                    BigDecimal allocated = zeroIfNull(c.getAllocatedAmount());
                    BigDecimal allocatedToTasks = allocatedToTasksByCategory.getOrDefault(c.getId(), BigDecimal.ZERO);
                    BigDecimal available = allocated.subtract(allocatedToTasks);
                    return available.compareTo(BigDecimal.ZERO) > 0
                            ? new AllocationSourceSuggestionDto(c.getId(), c.getName(), available)
                            : null;
                })
                .filter(v -> v != null)
                .sorted(Comparator.comparing(AllocationSourceSuggestionDto::getAvailableToAllocateAmount).reversed())
                .toList();
    }
}
