package vn.campuslife.service;

import vn.campuslife.enumeration.PreparationTaskStatus;
import vn.campuslife.model.TaskStatsRespone;
import vn.campuslife.model.preparation.*;
import vn.campuslife.security.department.DepartmentScope;

import java.util.List;

public interface PreparationService {
    void togglePreparation(Long activityId, boolean enabled);

    void togglePreparation(Long activityId, boolean enabled, DepartmentScope scope);

    TaskStatsRespone getStudentStats(Long studentId);

    PreparationTaskDto getTaskDetail(Long id);

    List<MyPreparationTaskDto> getPreparationTasks(Long activityId, String username);

    PreparationDashboardDto getPreparationDashboard(Long activityId);

    PreparationDashboardDto getPreparationDashboard(Long activityId, DepartmentScope scope);

    List<PreparationSummaryResponse> getPreparationsSummary(List<Long> activityIds);

    List<PreparationSummaryResponse> getPreparationsSummary(List<Long> activityIds, DepartmentScope scope);

    List<Long> listMyPreparationActivityIds(String username);

    PreparationTaskDto assignTask(CreatePreparationTaskRequest request);

    PreparationTaskDto assignTask(CreatePreparationTaskRequest request, DepartmentScope scope);

    PreparationTaskDto updateMyTaskStatus(Long taskId, PreparationTaskStatus status, String username);

    List<PreparationTaskMemberDto> listTaskMembers(Long taskId);

    void removeTaskMember(Long taskId, Long studentId);

    void promoteTaskLeader(Long taskId, Long studentId);

    void demoteTaskLeader(Long taskId, Long studentId);

    PreparationTaskDto acceptTask(Long taskId, String username);

    PreparationTaskDto requestCompleteTask(Long taskId, List<String> proofUrls, String username);

    PreparationTaskDto adminCompleteDecision(Long taskId, boolean approved);

    List<WorkloadWarningDto> getWorkloadWarnings(Long activityId);

    List<WorkloadWarningDto> getWorkloadWarnings(Long activityId, DepartmentScope scope);

    void addOrganizer(Long activityId, Long studentId);

    void addOrganizer(Long activityId, Long studentId, DepartmentScope scope);

    BulkAddOrganizersResultDto addOrganizers(Long activityId, List<Long> studentIds);

    BulkAddOrganizersResultDto addOrganizers(Long activityId, List<Long> studentIds, DepartmentScope scope);

    void removeOrganizer(Long activityId, Long studentId);

    void removeOrganizer(Long activityId, Long studentId, DepartmentScope scope);

    List<OrganizerDto> listOrganizers(Long activityId);

    void grantPrepSupervisor(Long activityId, Long studentId);

    void grantPrepSupervisor(Long activityId, Long studentId, DepartmentScope scope);

    void revokePrepSupervisor(Long activityId, Long studentId);

    void revokePrepSupervisor(Long activityId, Long studentId, DepartmentScope scope);
}
