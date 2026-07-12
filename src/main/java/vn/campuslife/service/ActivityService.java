package vn.campuslife.service;

import vn.campuslife.enumeration.ScoreType;
import vn.campuslife.model.activity.ActivityPresetDefinitionResponse;
import vn.campuslife.model.activity.ActivityPresetPreviewRequest;
import vn.campuslife.model.activity.ActivityResponse;
import vn.campuslife.model.activity.ActivityPresetPreviewResponse;
import vn.campuslife.model.activity.CreateActivityRequest;
import vn.campuslife.model.Response;
import vn.campuslife.security.department.DepartmentScope;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface ActivityService {
    Response createActivity(CreateActivityRequest request);

    Response createActivity(CreateActivityRequest request, DepartmentScope scope);

    Response getAllActivities();

    Response getAllActivities(String username); // username can be null for public access

    Response getAllActivities(String username, DepartmentScope scope); // username can be null for public access

    /**
     * Lấy danh sách hoạt động standalone (không thuộc series) có ít nhất 1 nhiệm vụ
     */
    Response getStandaloneActivitiesWithTasks();

    Response getStandaloneActivitiesWithTasks(DepartmentScope scope);

    Response getActivityById(Long id);

    Response getActivityById(Long id, String username); // username can be null for public access

    Response getActivityById(Long id, String username, DepartmentScope scope); // username can be null for public access

    Response updateActivity(Long id, CreateActivityRequest request);

    Response updateActivity(Long id, CreateActivityRequest request, DepartmentScope scope);

    Response deleteActivity(Long id);

    Response deleteActivity(Long id, DepartmentScope scope);

    List<ActivityPresetDefinitionResponse> getActivityPresetDefinitions();

    ActivityPresetPreviewResponse previewActivityPreset(ActivityPresetPreviewRequest request);

    List<ActivityResponse> getActivitiesByScoreType(ScoreType scoreType);

    List<ActivityResponse> getActivitiesByScoreType(ScoreType scoreType, DepartmentScope scope);

    List<ActivityResponse> getActivitiesByMonth(LocalDate start, LocalDate end);

    List<ActivityResponse> getActivitiesByMonth(LocalDate start, LocalDate end, DepartmentScope scope);

    List<ActivityResponse> getActivitiesForDepartment(Long departmentId);

    List<ActivityResponse> getActivitiesForDepartment(Long departmentId, DepartmentScope scope);

    List<ActivityResponse> listForCurrentUser(String username);

    /**
     * Kiểm tra activity có yêu cầu nộp bài không
     */
    Response checkRequiresSubmission(Long activityId);

    /**
     * Kiểm tra trạng thái đăng ký của student cho activity
     */
    Response checkRegistrationStatus(Long activityId, String username);

    void registerAllStudents(Long activityId);

    // Publish / Unpublish
    Response publishActivity(Long id);
    Response unpublishActivity(Long id);

    Response publishActivity(Long id, DepartmentScope scope);
    Response unpublishActivity(Long id, DepartmentScope scope);

    // Copy activity with optional offset days
    Response copyActivity(Long id, Integer offsetDays);

    Response copyActivity(Long id, Integer offsetDays, DepartmentScope scope);
    //tìm kiếm sự kiện
    List<ActivityResponse> searchUpcomingEvents(String keyword);

    List<ActivityResponse> searchUpcomingEvents(String keyword, DepartmentScope scope);
    //Sự kiện trong tháng
    List<ActivityResponse> getActivitiesByMonth(LocalDateTime start, LocalDateTime end);

    List<ActivityResponse> getActivitiesByMonth(LocalDateTime start, LocalDateTime end, DepartmentScope scope);

    /**
     * Tạo checkInCode cho các activity chưa có code
     * @return Response với số lượng activity đã được cập nhật
     */
    Response backfillCheckInCodes();

    Response backfillCheckInCodes(DepartmentScope scope);

}
