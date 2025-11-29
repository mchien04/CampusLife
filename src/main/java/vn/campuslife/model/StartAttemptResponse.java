package vn.campuslife.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.campuslife.entity.MiniGameAttempt;

import java.time.LocalDateTime;

/**
 * Response DTO cho API POST /api/minigames/{miniGameId}/start
 * Trả về thông tin attempt đã bắt đầu (không có nested entities để tránh
 * circular reference)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StartAttemptResponse {
    private Long id;
    private Long miniGameId;
    private Long studentId;
    private String status;
    private LocalDateTime startedAt;
    private Integer timeLimit; // Từ MiniGame

    public static StartAttemptResponse fromEntity(MiniGameAttempt attempt) {
        StartAttemptResponse response = new StartAttemptResponse();
        response.setId(attempt.getId());
        if (attempt.getMiniGame() != null) {
            response.setMiniGameId(attempt.getMiniGame().getId());
            response.setTimeLimit(attempt.getMiniGame().getTimeLimit());
        }
        if (attempt.getStudent() != null) {
            response.setStudentId(attempt.getStudent().getId());
        }
        response.setStatus(attempt.getStatus().toString());
        response.setStartedAt(attempt.getStartedAt());
        return response;
    }
}
