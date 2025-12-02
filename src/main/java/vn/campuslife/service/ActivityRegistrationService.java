package vn.campuslife.service;

import vn.campuslife.enumeration.RegistrationStatus;
import vn.campuslife.model.*;

public interface ActivityRegistrationService {

    /**
     * Đăng ký tham gia sự kiện
     */
    Response registerForActivity(ActivityRegistrationRequest request, Long studentId);

    /**
     * Hủy đăng ký sự kiện
     */
    Response cancelRegistration(Long activityId, Long studentId);

    /**
     * Lấy danh sách đăng ký của sinh viên
     */
    Response getStudentRegistrations(Long studentId);

    /**
     * Lấy danh sách đăng ký theo sự kiện
     */
    Response getActivityRegistrations(Long activityId);

    /**
     * Cập nhật trạng thái đăng ký (Admin/Manager)
     */
    Response updateRegistrationStatus(Long registrationId, String status);

    /**
     * Lấy chi tiết đăng ký
     */
    Response getRegistrationById(Long registrationId);

    /**
     * Kiểm tra sinh viên đã đăng ký sự kiện chưa
     */
    Response checkRegistrationStatus(Long activityId, Long studentId);

    /**
     * Check-in tham gia sự kiện qua ticketCode
     */
    Response checkIn(ActivityParticipationRequest request);

    /**
     * Lấy danh sách sinh viên đã tham gia / chưa tham gia
     */
    Response getParticipationReport(Long activityId);

    /**
     * Chấm điểm completion (đạt/không đạt)
     */
    Response gradeCompletion(Long participationId, boolean isCompleted, String notes);

    /**
     * Validate/lookup ticketCode để preview thông tin trước khi check-in
     */
    Response validateTicketCode(String ticketCode);

    /**
     * Backfill: Tạo participation cho tất cả registration đã APPROVED nhưng chưa có participation
     */
    Response backfillMissingParticipations();

    /**
     * Lấy danh sách participations theo activityId
     */
    Response getActivityParticipations(Long activityId);
    /**
     * Lấy danh sách Đăng ký của sinh theo status
     */
    Response getStudentRegistrationsStatus(Long studentId, RegistrationStatus status);
}
