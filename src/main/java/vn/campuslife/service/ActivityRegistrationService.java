package vn.campuslife.service;

import vn.campuslife.entity.ActivityRegistration;
import vn.campuslife.enumeration.EventTimeStatus;
import vn.campuslife.enumeration.RegistrationStatus;
import vn.campuslife.model.Response;
import vn.campuslife.model.activity.ActivityParticipationRequest;
import vn.campuslife.model.activity.ActivityRegistrationRequest;
import vn.campuslife.security.department.DepartmentScope;

import java.time.LocalDate;
import java.util.List;

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
     * Lấy danh sách đăng ký của sinh viên theo trạng thái thời gian sự kiện (UPCOMING / ONGOING / PAST)
     */
    Response getStudentRegistrations(Long studentId, EventTimeStatus eventTimeStatus);

    /**
     * Lấy danh sách đăng ký theo sự kiện
     */
    Response getActivityRegistrations(Long activityId);

    Response getActivityRegistrations(Long activityId, DepartmentScope scope);

    /**
     * Lấy danh sách đăng ký theo chuỗi sự kiện (series)
     */
    Response getSeriesRegistrations(Long seriesId);

    Response getSeriesRegistrations(Long seriesId, DepartmentScope scope);

    /**
     * Cập nhật trạng thái đăng ký (Admin/Manager)
     */
    Response updateRegistrationStatus(Long registrationId, String status);

    Response updateRegistrationStatus(Long registrationId, String status, DepartmentScope scope);

    /**
     * Lấy chi tiết đăng ký
     */
    Response getRegistrationById(Long registrationId);

    Response getRegistrationById(Long registrationId, DepartmentScope scope);

    /**
     * Kiểm tra sinh viên đã đăng ký sự kiện chưa
     */
    Response checkRegistrationStatus(Long activityId, Long studentId);

    /**
     * Lịch cá nhân: markedDates (chấm theo ngày) + events trong khoảng from/to.
     * Optional {@code date} thu hẹp danh sách events theo ngày được chọn.
     */
    Response getStudentJoinedEventDates(Long studentId, LocalDate from, LocalDate to, LocalDate date);

    /**
     * Đăng ký vào danh sách chờ
     */
    Response registerForWaitlist(Long activityId, Long studentId);

    Response cancelSeriesRegistration(Long seriesId, Long studentId);

    void promoteWaitlist(Long activityId);

    /**
     * Check-in tham gia sự kiện qua ticketCode
     */
    Response checkIn(ActivityParticipationRequest request, String username);

    /**
     * Check-in bằng QR code (tự động set thành ATTENDED)
     * 
     * @param checkInCode Mã QR code từ activity
     * @param studentId   ID của sinh viên (từ authentication)
     * @return Response với thông tin participation
     */
    Response checkInByQrCode(String checkInCode, Long studentId);

    /**
     * Lấy danh sách sinh viên đã tham gia / chưa tham gia
     */
    Response getParticipationReport(Long activityId);

    Response getParticipationReport(Long activityId, DepartmentScope scope);

    /**
     * Xuất Excel danh sách sinh viên tham gia / chưa tham gia theo activity
     */
    ExportFile exportParticipationReport(Long activityId);

    ExportFile exportParticipationReport(Long activityId, DepartmentScope scope);

    record ExportFile(String filename, String contentType, byte[] bytes) {
    }

    /**
     * Chấm điểm completion (đạt/không đạt)
     */
    Response gradeCompletion(Long participationId, boolean isCompleted, String notes);

    Response gradeCompletion(Long participationId, boolean isCompleted, String notes, DepartmentScope scope);

    /**
     * Validate/lookup ticketCode để preview thông tin trước khi check-in
     */
    Response validateTicketCode(String ticketCode, String username);

    /**
     * Backfill: Tạo participation cho tất cả registration đã APPROVED nhưng chưa có
     * participation
     */
    Response backfillMissingParticipations();

    Response backfillMissingParticipations(DepartmentScope scope);

    /**
     * Lấy danh sách participations theo activityId
     */
    Response getActivityParticipations(Long activityId);

    Response getActivityParticipations(Long activityId, DepartmentScope scope);

    /**
     * Lấy danh sách Đăng ký của sinh theo status
     */
    Response getStudentRegistrationsStatus(Long studentId, RegistrationStatus status);

    /**
     * Tìm kiếm
     */
    Response search(String keyword, RegistrationStatus status);

    Response search(String keyword, RegistrationStatus status, DepartmentScope scope);
}
