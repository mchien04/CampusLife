package vn.campuslife.service;

import vn.campuslife.model.Response;
import vn.campuslife.security.department.DepartmentScope;

public interface StatisticsService {
    
    /**
     * Dashboard tổng quan
     */
    Response getDashboardOverview(Long studentId);

    Response getDashboardOverview(Long studentId, DepartmentScope scope);
    
    /**
     * Thống kê Activities
     */
    Response getActivityStatistics(String activityType, String scoreType, Long departmentId, 
                                   java.time.LocalDateTime startDate, java.time.LocalDateTime endDate);

    Response getActivityStatistics(String activityType, String scoreType, Long departmentId,
                                   java.time.LocalDateTime startDate, java.time.LocalDateTime endDate,
                                   DepartmentScope scope);
    
    /**
     * Thống kê Students
     */
    Response getStudentStatistics(Long departmentId, Long classId, Long semesterId);

    Response getStudentStatistics(Long departmentId, Long classId, Long semesterId, DepartmentScope scope);
    
    /**
     * Thống kê Scores
     */
    Response getScoreStatistics(String scoreType, Long semesterId, Long departmentId, 
                                Long classId, Long studentId);

    Response getScoreStatistics(String scoreType, Long semesterId, Long departmentId,
                                Long classId, Long studentId, DepartmentScope scope);
    
    /**
     * Thống kê Series
     */
    Response getSeriesStatistics(Long seriesId, Long semesterId);
    
    /**
     * Thống kê MiniGames
     */
    Response getMiniGameStatistics(Long miniGameId, java.time.LocalDateTime startDate, 
                                    java.time.LocalDateTime endDate);

    /**
     * Score source-type breakdown
     */
    Response getScoreBreakdown(Long semesterId, Long studentId, Long departmentId);

    Response getScoreBreakdown(Long semesterId, Long studentId, Long departmentId, DepartmentScope scope);
}

