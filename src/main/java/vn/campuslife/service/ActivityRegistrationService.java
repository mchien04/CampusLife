package vn.campuslife.service;

import vn.campuslife.model.ActivityRegistrationRequest;
import vn.campuslife.model.ActivityRegistrationResponse;
import vn.campuslife.model.ActivityParticipationRequest;
import vn.campuslife.model.ActivityParticipationResponse;
import vn.campuslife.model.Response;

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
     * Ghi nhận tham gia sự kiện
     */
    Response recordParticipation(ActivityParticipationRequest request, Long studentId);

    /**
     * Lấy danh sách tham gia của sinh viên
     */
    Response getStudentParticipations(Long studentId);

    /**
     * Lấy danh sách tham gia theo sự kiện
     */
    Response getActivityParticipations(Long activityId);

    /**
     * Kiểm tra sinh viên đã đăng ký sự kiện chưa
     */
    Response checkRegistrationStatus(Long activityId, Long studentId);
}
