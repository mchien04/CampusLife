package vn.campuslife.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.campuslife.model.Response;
import vn.campuslife.repository.StudentRepository;
import vn.campuslife.repository.UserRepository;
import vn.campuslife.service.TaskSubmissionService;

import java.util.List;

@RestController
@RequestMapping("/api/submissions")
@RequiredArgsConstructor
public class TaskSubmissionController {

    private final TaskSubmissionService taskSubmissionService;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;

    /**
     * Nộp bài cho task
     */
    @PostMapping("/task/{taskId}")
    public ResponseEntity<Response> submitTask(@PathVariable Long taskId,
            @RequestParam(required = false) String content,
            @RequestParam(required = false) List<MultipartFile> files,
            Authentication authentication) {
        try {
            Long studentId = getStudentIdFromAuth(authentication);
            if (studentId == null) {
                return ResponseEntity.badRequest()
                        .body(new Response(false, "Student not found", null));
            }

            Response response = taskSubmissionService.submitTask(taskId, content, files);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Failed to submit task: " + e.getMessage(), null));
        }
    }

    /**
     * Cập nhật bài nộp
     */
    @PutMapping("/{submissionId}")
    public ResponseEntity<Response> updateSubmission(@PathVariable Long submissionId,
            @RequestParam(required = false) String content,
            @RequestParam(required = false) List<MultipartFile> files,
            Authentication authentication) {
        try {
            Long studentId = getStudentIdFromAuth(authentication);
            if (studentId == null) {
                return ResponseEntity.badRequest()
                        .body(new Response(false, "Student not found", null));
            }

            Response response = taskSubmissionService.updateSubmission(submissionId, studentId, content, files);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Failed to update submission: " + e.getMessage(), null));
        }
    }

    /**
     * Lấy danh sách bài nộp của student cho một task
     */
    @GetMapping("/task/{taskId}/my")
    public ResponseEntity<Response> getMySubmissions(@PathVariable Long taskId, Authentication authentication) {
        try {
            Long studentId = getStudentIdFromAuth(authentication);
            if (studentId == null) {
                return ResponseEntity.badRequest()
                        .body(new Response(false, "Student not found", null));
            }

            Response response = taskSubmissionService.getStudentSubmissions(taskId, studentId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Failed to get submissions: " + e.getMessage(), null));
        }
    }

    /**
     * Lấy tất cả bài nộp của một task (Admin/Manager)
     */
    @GetMapping("/task/{taskId}")
    public ResponseEntity<Response> getTaskSubmissions(@PathVariable Long taskId) {
        try {
            Response response = taskSubmissionService.getTaskSubmissions(taskId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Failed to get task submissions: " + e.getMessage(), null));
        }
    }

    /**
     * Chấm điểm bài nộp
     */
    @PutMapping("/{submissionId}/grade")
    public ResponseEntity<Response> gradeSubmission(@PathVariable Long submissionId,
            @RequestParam Double score,
            @RequestParam(required = false) String feedback,
            Authentication authentication) {
        try {
            Long graderId = getUserIdFromAuth(authentication);
            if (graderId == null) {
                return ResponseEntity.badRequest()
                        .body(new Response(false, "User not found", null));
            }

            Response response = taskSubmissionService.gradeSubmission(submissionId, graderId, score, feedback);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Failed to grade submission: " + e.getMessage(), null));
        }
    }

    /**
     * Lấy chi tiết bài nộp
     */
    @GetMapping("/{submissionId}")
    public ResponseEntity<Response> getSubmissionDetails(@PathVariable Long submissionId) {
        try {
            Response response = taskSubmissionService.getSubmissionDetails(submissionId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Failed to get submission details: " + e.getMessage(), null));
        }
    }

    /**
     * Xóa bài nộp
     */
    @DeleteMapping("/{submissionId}")
    public ResponseEntity<Response> deleteSubmission(@PathVariable Long submissionId, Authentication authentication) {
        try {
            Long studentId = getStudentIdFromAuth(authentication);
            if (studentId == null) {
                return ResponseEntity.badRequest()
                        .body(new Response(false, "Student not found", null));
            }

            Response response = taskSubmissionService.deleteSubmission(submissionId, studentId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Failed to delete submission: " + e.getMessage(), null));
        }
    }

    /**
     * Lấy danh sách file đính kèm
     */
    @GetMapping("/{submissionId}/files")
    public ResponseEntity<Response> getSubmissionFiles(@PathVariable Long submissionId) {
        try {
            Response response = taskSubmissionService.getSubmissionFiles(submissionId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Failed to get submission files: " + e.getMessage(), null));
        }
    }

    /**
     * Helper method to get student ID from authentication
     */
    private Long getStudentIdFromAuth(Authentication authentication) {
        try {
            String username = authentication.getName();
            return studentRepository.findByUserUsernameAndIsDeletedFalse(username)
                    .map(s -> s.getId())
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }


    /**
     * Helper method to get user ID from authentication
     */

    private Long getUserIdFromAuth(Authentication authentication) {
        try {
            String username = authentication.getName();
            return userRepository.findByUsername(username)
                    .map(u -> u.getId())
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }
}
