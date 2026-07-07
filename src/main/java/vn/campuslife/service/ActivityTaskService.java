package vn.campuslife.service;

import vn.campuslife.model.activity.task.CreateActivityTaskRequest;
import vn.campuslife.model.activity.task.TaskAssignmentRequest;
import vn.campuslife.model.Response;
import vn.campuslife.security.department.DepartmentScope;

public interface ActivityTaskService {

    /**
     * Tạo nhiệm vụ mới cho hoạt động
     */
    Response createTask(CreateActivityTaskRequest request);

    Response createTask(CreateActivityTaskRequest request, DepartmentScope scope);

    /**
     * Lấy danh sách nhiệm vụ theo hoạt động
     */
    Response getTasksByActivity(Long activityId);

    Response getTasksByActivity(Long activityId, DepartmentScope scope);

    /**
     * Lấy chi tiết nhiệm vụ
     */
    Response getTaskById(Long taskId);

    Response getTaskById(Long taskId, DepartmentScope scope);

    /**
     * Cập nhật nhiệm vụ
     */
    Response updateTask(Long taskId, CreateActivityTaskRequest request);

    Response updateTask(Long taskId, CreateActivityTaskRequest request, DepartmentScope scope);

    /**
     * Xóa nhiệm vụ
     */
    Response deleteTask(Long taskId);

    Response deleteTask(Long taskId, DepartmentScope scope);

    /**
     * Phân công nhiệm vụ cho sinh viên
     */
    Response assignTask(TaskAssignmentRequest request);

    Response assignTask(TaskAssignmentRequest request, DepartmentScope scope);

    /**
     * Cập nhật trạng thái nhiệm vụ
     */
    Response updateTaskStatus(Long assignmentId, String status);

    /**
     * Lấy danh sách phân công của sinh viên
     */
    Response getStudentTasks(Long studentId);

    Response getStudentTasks(Long studentId, DepartmentScope scope);

    /**
     * Lấy danh sách phân công theo nhiệm vụ
     */
    Response getTaskAssignments(Long taskId);

    Response getTaskAssignments(Long taskId, DepartmentScope scope);

    /**
     * Hủy phân công nhiệm vụ
     */
    Response removeTaskAssignment(Long assignmentId);

    Response removeTaskAssignment(Long assignmentId, DepartmentScope scope);

    /**
     * Tự động phân công nhiệm vụ cho sinh viên thuộc khoa (nếu bắt buộc)
     */
    Response autoAssignMandatoryTasks(Long activityId);

    Response autoAssignMandatoryTasks(Long activityId, DepartmentScope scope);

    /**
     * Lấy danh sách sinh viên đăng ký cho activity để phân công nhiệm vụ
     */
    Response getRegisteredStudentsForActivity(Long activityId);

    Response getRegisteredStudentsForActivity(Long activityId, DepartmentScope scope);

    /**
     * Phân công nhiệm vụ cho tất cả sinh viên đăng ký activity
     */
    Response assignTaskToRegisteredStudents(Long activityId, Long taskId);

    Response assignTaskToRegisteredStudents(Long activityId, Long taskId, DepartmentScope scope);

    /**
     * Kiểm tra và cập nhật status OVERDUE cho các assignment quá hạn chưa nộp
     */
    Response checkAndUpdateOverdueAssignments();
    /**
     * Lấy danh sách phân công theo Activity ID và Student ID
     */
    Response getAssignmentsByActivityAndStudent(Long activityId, Long studentId);

    Response getAssignmentsByActivityAndStudent(Long activityId, Long studentId, DepartmentScope scope);
}


