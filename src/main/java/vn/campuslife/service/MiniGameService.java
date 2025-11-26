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
}

