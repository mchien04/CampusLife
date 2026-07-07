package vn.campuslife.controller.score;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.campuslife.enumeration.ScoreType;
import vn.campuslife.model.Response;
import vn.campuslife.security.department.DepartmentRequestScope;
import vn.campuslife.security.department.DepartmentScope;
import vn.campuslife.service.RecalculationJobService;
import vn.campuslife.service.ScoreService;
import vn.campuslife.service.StudentService;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/scores")
@RequiredArgsConstructor
public class ScoreController {

    private final ScoreService scoreService;
    private final StudentService studentService;
    private final RecalculationJobService recalculationJobService;

    // Deprecated: training score by criteria removed

    @GetMapping("/student/{studentId}/semester/{semesterId}")
    public ResponseEntity<Response> viewScores(@PathVariable Long studentId, @PathVariable Long semesterId,
            HttpServletRequest request) {
        DepartmentScope scope = currentScope(request);
        Response resp = hasManagerScope(scope)
                ? scoreService.viewScores(studentId, semesterId, scope)
                : scoreService.viewScores(studentId, semesterId);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/student/{studentId}/semester/{semesterId}/total")
    public ResponseEntity<Response> getTotalScore(@PathVariable Long studentId, @PathVariable Long semesterId,
            HttpServletRequest request) {
        DepartmentScope scope = currentScope(request);
        Response resp = hasManagerScope(scope)
                ? scoreService.getTotalScore(studentId, semesterId, scope)
                : scoreService.getTotalScore(studentId, semesterId);
        return ResponseEntity.ok(resp);
    }

    /**
     * Lấy bảng xếp hạng điểm sinh viên
     * 
     * @param semesterId ID học kỳ (required)
     * @param scoreType Loại điểm (optional - null = tổng điểm tất cả loại): REN_LUYEN, CONG_TAC_XA_HOI, CHUYEN_DE
     * @param departmentId ID khoa (optional - null = tất cả khoa)
     * @param classId ID lớp (optional - null = tất cả lớp)
     * @param sortOrder Thứ tự sắp xếp: "ASC" (thấp đến cao) hoặc "DESC" (cao xuống thấp, mặc định)
     * @return Danh sách xếp hạng với rank, student info và score
     */
    @GetMapping("/ranking")
    public ResponseEntity<Response> getStudentRanking(
            @RequestParam Long semesterId,
            @RequestParam(required = false) String scoreType,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false, defaultValue = "DESC") String sortOrder,
            HttpServletRequest request) {
        try {
            ScoreType scoreTypeEnum = null;
            if (scoreType != null && !scoreType.isBlank()) {
                try {
                    scoreTypeEnum = ScoreType.valueOf(scoreType.toUpperCase());
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.badRequest()
                            .body(new Response(false, "Invalid scoreType: " + scoreType, null));
                }
            }

            DepartmentScope scope = currentScope(request);
            Response resp = hasManagerScope(scope)
                    ? scoreService.getStudentRanking(semesterId, scoreTypeEnum, departmentId, classId, sortOrder, scope)
                    : scoreService.getStudentRanking(semesterId, scoreTypeEnum, departmentId, classId, sortOrder);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Failed to get ranking: " + e.getMessage(), null));
        }
    }

    /**
     * Rà soát và tính lại điểm cho một student
     * Bao gồm: điểm từ ActivityParticipation (minigame, activity thường) và milestone points từ series
     * 
     * @param studentId ID sinh viên
     * @param semesterId ID học kỳ (optional - null = học kỳ hiện tại)
     * @return Kết quả rà soát và cập nhật
     */
    @PostMapping("/recalculate/student/{studentId}")
    public ResponseEntity<Response> recalculateStudentScore(
            @PathVariable Long studentId,
            @RequestParam(required = false) Long semesterId,
            HttpServletRequest request) {
        try {
            DepartmentScope scope = currentScope(request);
            Response resp = hasManagerScope(scope)
                    ? scoreService.recalculateStudentScore(studentId, semesterId, scope)
                    : scoreService.recalculateStudentScore(studentId, semesterId);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Failed to recalculate score: " + e.getMessage(), null));
        }
    }

    /**
     * Rà soát và tính lại điểm cho tất cả students
     * 
     * @param semesterId ID học kỳ (optional - null = học kỳ hiện tại)
     * @return Kết quả rà soát và cập nhật
     */
    @PostMapping("/recalculate/all")
    public ResponseEntity<Response> recalculateAllStudentScores(
            @RequestParam(required = false) Long semesterId,
            HttpServletRequest request) {
        try {
            DepartmentScope scope = currentScope(request);
            Response resp = hasManagerScope(scope)
                    ? scoreService.recalculateAllStudentScores(semesterId, scope)
                    : scoreService.recalculateAllStudentScores(semesterId);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Failed to recalculate all scores: " + e.getMessage(), null));
        }
    }

    /**
     * Xem lịch sử điểm của student
     * Bao gồm: ScoreEntry (tổng hợp) và ActivityParticipation (chi tiết)
     * 
     * @param studentId  ID sinh viên
     * @param semesterId ID học kỳ (required)
     * @param scoreType  Loại điểm (optional - null = tất cả loại)
     * @param page       Số trang (default 0)
     * @param size       Số bản ghi mỗi trang (default 20)
     * @return Lịch sử điểm với thông tin nguồn (activity/series)
     */
    @GetMapping("/history/student/{studentId}")
    public ResponseEntity<Response> getScoreHistory(
            @PathVariable Long studentId,
            @RequestParam Long semesterId,
            @RequestParam(required = false) String scoreType,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer size,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String keyword,
            Authentication authentication,
            HttpServletRequest request) {
        try {
            // Get requesting student ID if user is a student
            Long requestingStudentId = null;
            if (authentication != null) {
                boolean isStudent = authentication.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT"));
                if (isStudent) {
                    String username = authentication.getName();
                    requestingStudentId = studentService.getStudentIdByUsername(username);
                }
            }

            ScoreType scoreTypeEnum = null;
            if (scoreType != null && !scoreType.isBlank()) {
                try {
                    scoreTypeEnum = ScoreType.valueOf(scoreType.toUpperCase());
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.badRequest()
                            .body(new Response(false, "Invalid scoreType: " + scoreType, null));
                }
            }

            LocalDateTime startDateTime = null;
            LocalDateTime endDateTime = null;
            if (startDate != null && !startDate.isBlank()) {
                startDateTime = LocalDateTime.parse(startDate);
            }
            if (endDate != null && !endDate.isBlank()) {
                endDateTime = LocalDateTime.parse(endDate);
            }

            DepartmentScope scope = currentScope(request);
            Response resp = hasManagerScope(scope)
                    ? scoreService.getScoreHistory(studentId, semesterId, scoreTypeEnum, page, size, requestingStudentId,
                            startDateTime, endDateTime, keyword, scope)
                    : scoreService.getScoreHistory(studentId, semesterId, scoreTypeEnum, page, size, requestingStudentId,
                            startDateTime, endDateTime, keyword);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Failed to get score history: " + e.getMessage(), null));
        }
    }

    /**
     * Start async recalculation for all students in a semester
     * Returns a job ID for tracking progress
     *
     * @param semesterId ID học kỳ (optional - null = học kỳ hiện tại)
     * @return Job info with jobId for status polling
     */
    @PostMapping("/recalculate/async")
    public ResponseEntity<Response> startAsyncRecalculation(
            @RequestParam(required = false) Long semesterId,
            Authentication authentication,
            HttpServletRequest request) {
        try {
            if (hasManagerScope(currentScope(request))) {
                return ResponseEntity.badRequest()
                        .body(new Response(false,
                                "Async recalculation for MANAGER requires persisted DepartmentScopeSnapshot and is not enabled yet",
                                null));
            }
            Long createdBy = null;
            if (authentication != null) {
                // Try to resolve user ID if available
                createdBy = null; // createdBy is optional tracking field
            }
            Response resp = recalculationJobService.startAsyncRecalculation(semesterId, createdBy);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Failed to start async recalculation: " + e.getMessage(), null));
        }
    }

    /**
     * Get status of an async recalculation job
     *
     * @param jobId ID of the recalculation job
     * @return Job status with progress info
     */
    @GetMapping("/recalculate/status/{jobId}")
    public ResponseEntity<Response> getRecalculationJobStatus(@PathVariable Long jobId) {
        try {
            Response resp = recalculationJobService.getJobStatus(jobId);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Failed to get job status: " + e.getMessage(), null));
        }
    }

    /**
     * Retry a failed or timed-out recalculation job
     *
     * @param jobId ID of the failed recalculation job
     * @return New job info
     */
    @PostMapping("/recalculate/retry/{jobId}")
    public ResponseEntity<Response> retryRecalculationJob(@PathVariable Long jobId) {
        try {
            Response resp = recalculationJobService.retryFailedJob(jobId);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Failed to retry job: " + e.getMessage(), null));
        }
    }

    private DepartmentScope currentScope(HttpServletRequest request) {
        return DepartmentRequestScope.get(request).orElse(null);
    }

    private boolean hasManagerScope(DepartmentScope scope) {
        return scope != null && scope.manager() && !scope.departmentIds().isEmpty();
    }
}
