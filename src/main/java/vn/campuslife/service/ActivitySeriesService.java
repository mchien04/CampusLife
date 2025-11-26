package vn.campuslife.service;

import vn.campuslife.model.Response;

public interface ActivitySeriesService {
    /**
     * Tạo chuỗi sự kiện mới
     */
    Response createSeries(String name, String description, String milestonePointsJson, 
                         vn.campuslife.enumeration.ScoreType scoreType, Long mainActivityId);

    /**
     * Thêm activity vào chuỗi
     */
    Response addActivityToSeries(Long activityId, Long seriesId, Integer order);

    /**
     * Cập nhật tiến độ sinh viên khi check-in activity thuộc chuỗi
     */
    Response updateStudentProgress(Long studentId, Long activityId);

    /**
     * Tính điểm milestone dựa trên số sự kiện đã tham gia
     */
    Response calculateMilestonePoints(Long studentId, Long seriesId);

    /**
     * Kiểm tra và áp dụng penalty nếu không đạt yêu cầu tối thiểu
     */
    Response checkMinimumRequirement(Long studentId, Long seriesId);
}

