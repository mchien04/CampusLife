package vn.campuslife.service;

import vn.campuslife.model.Response;

public interface ActivitySeriesService {
        /**
         * Tạo chuỗi sự kiện mới
         */
        Response createSeries(String name, String description, String milestonePointsJson,
                        vn.campuslife.enumeration.ScoreType scoreType, Long mainActivityId,
                        java.time.LocalDateTime registrationStartDate,
                        java.time.LocalDateTime registrationDeadline,
                        Boolean requiresApproval, Integer ticketQuantity);

        /**
         * Tạo activity trong series với các thuộc tính tối giản
         */
        Response createActivityInSeries(Long seriesId, String name, String description,
                        java.time.LocalDateTime startDate, java.time.LocalDateTime endDate,
                        String location, Integer order, String shareLink, String bannerUrl,
                        String benefits, String requirements, String contactInfo, java.util.List<Long> organizerIds,
                        vn.campuslife.enumeration.ActivityType type);

        /**
         * Thêm activity vào chuỗi
         */
        Response addActivityToSeries(Long activityId, Long seriesId, Integer order);

        /**
         * Student đăng ký series (tự động đăng ký tất cả activities trong series)
         */
        Response registerForSeries(Long seriesId, Long studentId);

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

        /**
         * Lấy tất cả chuỗi sự kiện
         */
        Response getAllSeries();

        /**
         * Lấy chuỗi sự kiện theo ID
         */
        Response getSeriesById(Long seriesId);

        /**
         * Lấy danh sách activities trong series
         */
        Response getActivitiesInSeries(Long seriesId);

        /**
         * Lấy thông tin progress của student trong series
         */
        Response getStudentProgress(Long seriesId, Long studentId);

        /**
         * Kiểm tra student đã đăng ký chuỗi sự kiện này chưa
         */
        Response checkSeriesRegistration(Long seriesId, Long studentId);

        /**
         * Cập nhật thông tin chuỗi sự kiện
         */
        Response updateSeries(Long seriesId, String name, String description, String milestonePointsJson,
                        vn.campuslife.enumeration.ScoreType scoreType, Long mainActivityId,
                        java.time.LocalDateTime registrationStartDate,
                        java.time.LocalDateTime registrationDeadline,
                        Boolean requiresApproval, Integer ticketQuantity);

        /**
         * Xóa chuỗi sự kiện (soft delete)
         */
        Response deleteSeries(Long seriesId);
}
