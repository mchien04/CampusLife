package vn.campuslife.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.campuslife.enumeration.ScoreType;
import vn.campuslife.model.Response;
import vn.campuslife.service.ScoreService;
import vn.campuslife.service.StudentService;

@RestController
@RequestMapping("/api/scores")
@RequiredArgsConstructor
public class ScoreController {

    private final ScoreService scoreService;
    private final StudentService studentService;

    // Deprecated: training score by criteria removed

    @GetMapping("/student/{studentId}/semester/{semesterId}")
    public ResponseEntity<Response> viewScores(@PathVariable Long studentId, @PathVariable Long semesterId) {
        Response resp = scoreService.viewScores(studentId, semesterId);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/student/{studentId}/semester/{semesterId}/total")
    public ResponseEntity<Response> getTotalScore(@PathVariable Long studentId, @PathVariable Long semesterId) {
        Response resp = scoreService.getTotalScore(studentId, semesterId);
        return ResponseEntity.ok(resp);
    }

    /**
     * Lấy bảng xếp hạng điểm sinh viên
     * 
     * @param semesterId ID học kỳ (required)
     * @param scoreType Loại điểm (optional - null = tổng điểm tất cả loại): REN_LUYEN, CONG_TAC_XA_HOI, CHUYEN_DE, KHAC
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
            @RequestParam(required = false, defaultValue = "DESC") String sortOrder) {
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

            Response resp = scoreService.getStudentRanking(semesterId, scoreTypeEnum, departmentId, classId, sortOrder);
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
            @RequestParam(required = false) Long semesterId) {
        try {
            Response resp = scoreService.recalculateStudentScore(studentId, semesterId);
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
            @RequestParam(required = false) Long semesterId) {
        try {
            Response resp = scoreService.recalculateAllStudentScores(semesterId);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Failed to recalculate all scores: " + e.getMessage(), null));
        }
    }

    /**
     * Xem lịch sử điểm của student
     * Bao gồm: ScoreHistory (tổng hợp) và ActivityParticipation (chi tiết)
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
            Authentication authentication) {
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

            Response resp = scoreService.getScoreHistory(studentId, semesterId, scoreTypeEnum, page, size, requestingStudentId);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Failed to get score history: " + e.getMessage(), null));
        }
    }
}
