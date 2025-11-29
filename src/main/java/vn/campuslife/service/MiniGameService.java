package vn.campuslife.service;

import vn.campuslife.model.Response;

import java.util.List;
import java.util.Map;

public interface MiniGameService {
    /**
     * Tạo minigame với quiz
     */
    Response createMiniGame(Long activityId, String title, String description, Integer questionCount,
                           Integer timeLimit, Integer requiredCorrectAnswers, java.math.BigDecimal rewardPoints,
                           List<Map<String, Object>> questions);

    /**
     * Lấy minigame theo activity ID
     */
    Response getMiniGameByActivity(Long activityId);

    /**
     * Student bắt đầu làm quiz
     */
    Response startAttempt(Long miniGameId, Long studentId);

    /**
     * Student nộp bài quiz
     */
    Response submitAttempt(Long attemptId, Long studentId, Map<Long, Long> answers);

    /**
     * Lấy lịch sử attempts của student
     */
    Response getStudentAttempts(Long studentId, Long miniGameId);

    /**
     * Tính điểm và tạo ActivityParticipation nếu đạt
     */
    Response calculateScoreAndCreateParticipation(Long attemptId);

    /**
     * Lấy danh sách câu hỏi và options của minigame (không có đáp án đúng)
     */
    Response getQuestions(Long miniGameId);

    /**
     * Lấy chi tiết attempt (bao gồm kết quả và đáp án đúng nếu đã submit)
     */
    Response getAttemptDetail(Long attemptId, Long studentId);

    /**
     * Cập nhật minigame
     */
    Response updateMiniGame(Long miniGameId, String title, String description, Integer questionCount,
                           Integer timeLimit, Integer requiredCorrectAnswers, java.math.BigDecimal rewardPoints,
                           List<Map<String, Object>> questions);

    /**
     * Xóa minigame (soft delete)
     */
    Response deleteMiniGame(Long miniGameId);

    /**
     * Lấy tất cả minigames (Admin/Manager)
     */
    Response getAllMiniGames();
}

