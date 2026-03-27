package vn.campuslife.service;

import vn.campuslife.enumeration.PreparationTaskStatus;
import vn.campuslife.model.TaskStatsRespone;
import vn.campuslife.model.preparation.*;

public interface PreparationService {
    void togglePreparation(Long activityId, boolean enabled);
    TaskStatsRespone getStudentStats(Long studentId);
    PreparationDashboardDto getPreparationDashboard(Long activityId);

    java.util.List<Long> listMyPreparationActivityIds(String username);

    PreparationTaskDto assignTask(CreatePreparationTaskRequest request);

    PreparationTaskDto updateMyTaskStatus(Long taskId, PreparationTaskStatus status, String username);

    void addOrganizer(Long activityId, Long studentId);

    void removeOrganizer(Long activityId, Long studentId);

    java.util.List<OrganizerDto> listOrganizers(Long activityId);
}
