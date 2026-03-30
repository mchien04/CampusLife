package vn.campuslife.service;

import org.springframework.web.multipart.MultipartFile;
import vn.campuslife.entity.PreparationTask;
import vn.campuslife.enumeration.PreparationTaskStatus;
import vn.campuslife.model.TaskStatsRespone;
import vn.campuslife.model.preparation.*;

import java.util.List;

public interface PreparationService {
    void togglePreparation(Long activityId, boolean enabled);

    // new
    TaskStatsRespone getStudentStats(Long studentId);

    PreparationTaskDto getTaskDetail(Long id);

    List<MyPreparationTaskDto> getPreparationTasks(Long activityId, String username);

    //
    PreparationDashboardDto getPreparationDashboard(Long activityId);

    java.util.List<Long> listMyPreparationActivityIds(String username);

    PreparationTaskDto assignTask(CreatePreparationTaskRequest request);

    PreparationTaskDto updateMyTaskStatus(Long taskId, PreparationTaskStatus status, String username);

    java.util.List<PreparationTaskMemberDto> listTaskMembers(Long taskId);

    void removeTaskMember(Long taskId, Long studentId);

    void promoteTaskLeader(Long taskId, Long studentId);

    void demoteTaskLeader(Long taskId, Long studentId);

    PreparationTaskDto acceptTask(Long taskId, String username);

    PreparationTaskDto requestCompleteTask(Long taskId, String username);

    PreparationTaskDto adminCompleteDecision(Long taskId, boolean approved);

    java.util.List<WorkloadWarningDto> getWorkloadWarnings(Long activityId);

    void addOrganizer(Long activityId, Long studentId);

    void removeOrganizer(Long activityId, Long studentId);

    java.util.List<OrganizerDto> listOrganizers(Long activityId);
}
