package vn.campuslife.service;

import vn.campuslife.model.Response;
import vn.campuslife.model.activity.quiz.CreateMiniGameRequest;
import vn.campuslife.model.activity.quiz.UpdateMiniGameRequest;
import vn.campuslife.security.department.DepartmentScope;

import java.util.List;
import java.util.Map;

public interface MiniGameService {
    Response createMiniGame(CreateMiniGameRequest request);

    Response createMiniGame(CreateMiniGameRequest request, DepartmentScope scope);

    /**
     * Lấy minigame theo activity ID
     */
    Response getMiniGameByActivity(Long activityId);

    Response getMiniGameByActivity(Long activityId, DepartmentScope scope);

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
    Response updateMiniGame(Long miniGameId, UpdateMiniGameRequest request);

    Response updateMiniGame(Long miniGameId, UpdateMiniGameRequest request, DepartmentScope scope);

    Response deleteMiniGame(Long miniGameId);

    Response deleteMiniGame(Long miniGameId, DepartmentScope scope);

    Response getAllMiniGames();

    Response getAllMiniGames(DepartmentScope scope);

    Response checkActivityHasQuiz(Long activityId);

    Response checkActivityHasQuiz(Long activityId, DepartmentScope scope);

    /**
     * Lấy danh sách câu hỏi và options với đáp án đúng (cho admin/manager để chỉnh sửa)
     * Nếu chưa có quiz, trả về questions rỗng
     */
    Response getQuestionsForEdit(Long miniGameId);

    Response getQuestionsForEdit(Long miniGameId, DepartmentScope scope);
}


