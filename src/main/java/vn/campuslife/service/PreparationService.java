package vn.campuslife.service;

import vn.campuslife.enumeration.PreparationTaskStatus;
import vn.campuslife.model.TaskStatsRespone;
import vn.campuslife.model.preparation.*;

import java.util.List;

public interface PreparationService {
    void togglePreparation(Long activityId, boolean enabled);

    TaskStatsRespone getStudentStats(Long studentId);

    PreparationTaskDto getTaskDetail(Long id);

    List<MyPreparationTaskDto> getPreparationTasks(Long activityId, String username);

    PreparationDashboardDto getPreparationDashboard(Long activityId);

    List<PreparationSummaryResponse> getPreparationsSummary(List<Long> activityIds);

    List<Long> listMyPreparationActivityIds(String username);

    PreparationTaskDto assignTask(CreatePreparationTaskRequest request);

    PreparationTaskDto updateMyTaskStatus(Long taskId, PreparationTaskStatus status, String username);

    List<PreparationTaskMemberDto> listTaskMembers(Long taskId);

    void removeTaskMember(Long taskId, Long studentId);

    void promoteTaskLeader(Long taskId, Long studentId);

    void demoteTaskLeader(Long taskId, Long studentId);

    PreparationTaskDto acceptTask(Long taskId, String username);

    PreparationTaskDto requestCompleteTask(Long taskId, List<String> proofUrls, String username);

    PreparationTaskDto adminCompleteDecision(Long taskId, boolean approved);

    List<WorkloadWarningDto> getWorkloadWarnings(Long activityId);

    void addOrganizer(Long activityId, Long studentId);

    BulkAddOrganizersResultDto addOrganizers(Long activityId, List<Long> studentIds);

    void removeOrganizer(Long activityId, Long studentId);

    List<OrganizerDto> listOrganizers(Long activityId);

    void grantPrepSupervisor(Long activityId, Long studentId);

    void revokePrepSupervisor(Long activityId, Long studentId);
}
