package vn.campuslife.service;

import org.springframework.web.multipart.MultipartFile;
import vn.campuslife.entity.PreparationTask;
import vn.campuslife.enumeration.PreparationTaskStatus;
import vn.campuslife.model.TaskStatsRespone;
import vn.campuslife.model.preparation.*;

import java.util.List;

public interface PreparationService {
    void togglePreparation(Long activityId, boolean enabled);
    //new
    TaskStatsRespone getStudentStats(Long studentId);
    PreparationTaskDto getTaskDetail(Long id);
    List<PreparationTaskDto> getPreparationTasks(Long activityId, Long userId);
    //
    PreparationDashboardDto getPreparationDashboard(Long activityId);

    java.util.List<Long> listMyPreparationActivityIds(String username);

    PreparationTaskDto assignTask(CreatePreparationTaskRequest request);

    PreparationTaskDto updateMyTaskStatus(Long taskId, PreparationTaskStatus status, String username);

    BudgetDto createOrUpdateBudget(UpsertBudgetRequest request);

    ExpenseDto createExpense(CreateExpenseRequest request, String username);

    UploadResultDto uploadExpenseEvidence(MultipartFile file);

    java.util.List<ExpenseDto> listExpenses(Long activityId, String status);

    ExpenseDto approveExpense(Long expenseId, boolean approved);

    void addOrganizer(Long activityId, Long studentId);

    void removeOrganizer(Long activityId, Long studentId);

    java.util.List<OrganizerDto> listOrganizers(Long activityId);
}
