package vn.campuslife.service;

import org.springframework.web.multipart.MultipartFile;
import vn.campuslife.model.Response;

import java.util.List;

public interface TaskSubmissionService {

    // Nộp bài cho task
    Response submitTask(Long taskId, Long studentId, String content, List<MultipartFile> files);

    // Cập nhật bài nộp
    Response updateSubmission(Long submissionId, Long studentId, String content, List<MultipartFile> files);

    // Lấy danh sách bài nộp của student cho một task
    Response getStudentSubmissions(Long taskId, Long studentId);

    // Lấy tất cả bài nộp của một task (Admin/Manager)
    Response getTaskSubmissions(Long taskId);

    // Chấm điểm bài nộp
    Response gradeSubmission(Long submissionId, Long graderId, Double score, String feedback);

    // Lấy chi tiết bài nộp
    Response getSubmissionDetails(Long submissionId);

    // Xóa bài nộp (chỉ student mới nộp)
    Response deleteSubmission(Long submissionId, Long studentId);

    // Lấy danh sách file đính kèm
    Response getSubmissionFiles(Long submissionId);
}
