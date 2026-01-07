package vn.campuslife.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.campuslife.entity.MiniGame;
import vn.campuslife.entity.MiniGameQuiz;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Response DTO cho API GET /api/minigames/{miniGameId}/questions/edit
 * Trả về danh sách câu hỏi và options (CÓ đáp án đúng) - Dùng cho admin/manager để chỉnh sửa
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuizQuestionsEditResponse {
    private Long miniGameId;
    private String title;
    private String description;
    private Integer questionCount;
    private Integer timeLimit;
    private Integer requiredCorrectAnswers;
    private java.math.BigDecimal rewardPoints;
    private List<QuizQuestionEditResponse> questions;

    public static QuizQuestionsEditResponse fromEntities(MiniGame miniGame, MiniGameQuiz quiz) {
        return fromEntities(miniGame, quiz, null);
    }

    public static QuizQuestionsEditResponse fromEntities(MiniGame miniGame, MiniGameQuiz quiz, String publicUrl) {
        QuizQuestionsEditResponse response = new QuizQuestionsEditResponse();
        response.setMiniGameId(miniGame.getId());
        response.setTitle(miniGame.getTitle());
        response.setDescription(miniGame.getDescription());
        response.setQuestionCount(miniGame.getQuestionCount());
        response.setTimeLimit(miniGame.getTimeLimit());
        response.setRequiredCorrectAnswers(miniGame.getRequiredCorrectAnswers());
        response.setRewardPoints(miniGame.getRewardPoints());

        if (quiz != null && quiz.getQuestions() != null && !quiz.getQuestions().isEmpty()) {
            response.setQuestions(quiz.getQuestions().stream()
                    .sorted((q1, q2) -> {
                        Integer order1 = q1.getDisplayOrder() != null ? q1.getDisplayOrder() : 0;
                        Integer order2 = q2.getDisplayOrder() != null ? q2.getDisplayOrder() : 0;
                        return order1.compareTo(order2);
                    })
                    .map(q -> QuizQuestionEditResponse.fromEntity(q, publicUrl))
                    .collect(Collectors.toList()));
        } else {
            // Nếu chưa có quiz, trả về questions rỗng
            response.setQuestions(new ArrayList<>());
        }

        return response;
    }
}

