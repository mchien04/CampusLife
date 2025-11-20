package vn.campuslife.service;

import vn.campuslife.model.CreateActivityTaskRequest;
import vn.campuslife.model.TaskAssignmentRequest;
import vn.campuslife.model.Response;

public interface ActivityTaskService {

    /**
     * Tạo nhiệm vụ mới cho hoạt động
     */
    Response createTask(CreateActivityTaskRequest request);

    /**
     * Lấy danh sách nhiệm vụ theo hoạt động
     */
    Response getTasksByActivity(Long activityId);

    /**
     * Lấy chi tiết nhiệm vụ
     */
    Response getTaskById(Long taskId);

    /**
     * Cập nhật nhiệm vụ
     */
    Response updateTask(Long taskId, CreateActivityTaskRequest request);

    /**
     * Xóa nhiệm vụ
     */
    Response deleteTask(Long taskId);

    /**
     * Phân công nhiệm vụ cho sinh viên
     */
    Response assignTask(TaskAssignmentRequest request);

    /**
     * Cập nhật trạng thái nhiệm vụ
     */
    Response updateTaskStatus(Long assignmentId, String status);

    /**
     * Lấy danh sách phân công của sinh viên
     */
    Response getStudentTasks(Long studentId);

    /**
     * Lấy danh sách phân công theo nhiệm vụ
     */
    Response getTaskAssignments(Long taskId);

    /**
     * Hủy phân công nhiệm vụ
     */
    Response removeTaskAssignment(Long assignmentId);

    /**
     * Tự động phân công nhiệm vụ cho sinh viên thuộc khoa (nếu bắt buộc)
     */
    Response autoAssignMandatoryTasks(Long activityId);

    /**
     * Lấy danh sách sinh viên đăng ký cho activity để phân công nhiệm vụ
     */
    Response getRegisteredStudentsForActivity(Long activityId);

    /**
     * Phân công nhiệm vụ cho tất cả sinh viên đăng ký activity
     */
    Response assignTaskToRegisteredStudents(Long activityId, Long taskId);

    /**
     * Kiểm tra và cập nhật status OVERDUE cho các assignment quá hạn chưa nộp
     */
    Response checkAndUpdateOverdueAssignments();
}
