package vn.campuslife.service;

import vn.campuslife.model.Response;

public interface StatisticsService {
    
    /**
     * Dashboard tổng quan
     */
    Response getDashboardOverview(Long studentId);
    
    /**
     * Thống kê Activities
     */
    Response getActivityStatistics(String activityType, String scoreType, Long departmentId, 
                                   java.time.LocalDateTime startDate, java.time.LocalDateTime endDate);
    
    /**
     * Thống kê Students
     */
    Response getStudentStatistics(Long departmentId, Long classId, Long semesterId);
    
    /**
     * Thống kê Scores
     */
    Response getScoreStatistics(String scoreType, Long semesterId, Long departmentId, 
                                Long classId, Long studentId);
    
    /**
     * Thống kê Series
     */
    Response getSeriesStatistics(Long seriesId, Long semesterId);
    
    /**
     * Thống kê MiniGames
     */
    Response getMiniGameStatistics(Long miniGameId, java.time.LocalDateTime startDate, 
                                    java.time.LocalDateTime endDate);
}

