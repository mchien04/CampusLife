package vn.campuslife.service;

import org.springframework.web.multipart.MultipartFile;
import vn.campuslife.model.Response;
import vn.campuslife.security.department.DepartmentScope;

import java.util.List;

public interface TaskSubmissionService {

    // Nộp bài cho task
    Response submitTask(Long taskId, Long studentId, String content, List<MultipartFile> files,
            List<MultipartFile> images);

    // Cập nhật bài nộp
    Response updateSubmission(Long submissionId, Long studentId, String content, List<MultipartFile> files,
            List<MultipartFile> images);

    // Lấy danh sách bài nộp của student cho một task
    Response getStudentSubmissions(Long taskId, Long studentId);

    // Lấy tất cả bài nộp của một task (Admin/Manager)
    Response getTaskSubmissions(Long taskId);

    Response getTaskSubmissions(Long taskId, DepartmentScope scope);

    // Chấm điểm bài nộp (đạt/không đạt)
    Response gradeSubmission(Long submissionId, Long graderId, boolean isCompleted, String feedback);

    Response gradeSubmission(Long submissionId, Long graderId, boolean isCompleted, String feedback, DepartmentScope scope);

    // Lấy chi tiết bài nộp
    Response getSubmissionDetails(Long submissionId);

    Response getSubmissionDetails(Long submissionId, DepartmentScope scope);

    // Xóa bài nộp (chỉ student mới nộp)
    Response deleteSubmission(Long submissionId, Long studentId);

    // Lấy danh sách file đính kèm
    Response getSubmissionFiles(Long submissionId);

    Response getSubmissionFiles(Long submissionId, DepartmentScope scope);
}
