package vn.campuslife.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.campuslife.enumeration.ScoreType;
import vn.campuslife.model.Response;
import vn.campuslife.service.ScoreService;

@RestController
@RequestMapping("/api/scores")
@RequiredArgsConstructor
public class ScoreController {

    private final ScoreService scoreService;

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
}
