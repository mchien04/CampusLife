package vn.campuslife.service;

import vn.campuslife.entity.Activity;
import vn.campuslife.entity.Student;
import vn.campuslife.enumeration.ActivityType;
import vn.campuslife.enumeration.ScoreType;
import vn.campuslife.model.CreateActivityRequest;
import vn.campuslife.model.Response;

import java.time.LocalDate;
import java.util.List;

public interface ActivityService {
    Response createActivity(CreateActivityRequest request);
    Response getAllActivities();
    Response getActivityById(Long id);
    Response updateActivity(Long id, CreateActivityRequest request);
    Response deleteActivity(Long id);

    List<Activity> getActivitiesByScoreType(ScoreType scoreType);
    List<Activity> getActivitiesByMonth(LocalDate start, LocalDate end);
    List<Activity> getActivitiesForDepartment(Long departmentId);
    List<Activity> listForCurrentUser(String username);
    void registerAllStudents(Long activityId);
}