package vn.campuslife.service;

import vn.campuslife.entity.Activity;
import vn.campuslife.enumeration.ScoreType;
import vn.campuslife.model.CreateActivityRequest;
import vn.campuslife.model.Response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface ActivityService {
    Response createActivity(CreateActivityRequest request);

    Response getAllActivities();

    Response getAllActivities(String username); // username can be null for public access

    Response getActivityById(Long id);

    Response getActivityById(Long id, String username); // username can be null for public access

    Response updateActivity(Long id, CreateActivityRequest request);

    Response deleteActivity(Long id);

    List<Activity> getActivitiesByScoreType(ScoreType scoreType);

    List<Activity> getActivitiesByMonth(LocalDate start, LocalDate end);

    List<Activity> getActivitiesForDepartment(Long departmentId);

    List<Activity> listForCurrentUser(String username);

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

    // Copy activity with optional offset days
    Response copyActivity(Long id, Integer offsetDays);
    //tìm kiếm sự kiện
    List<Activity>searchUpcomingEvents(String keyword);
    //Sự kiện trong tháng
    List<Activity> getActivitiesByMonth(LocalDateTime start, LocalDateTime end);


}