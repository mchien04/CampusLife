package vn.campuslife.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.campuslife.entity.MiniGame;
import vn.campuslife.entity.MiniGameQuiz;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Response DTO cho API GET /api/minigames/{miniGameId}/questions
 * Trả về danh sách câu hỏi và options (KHÔNG có đáp án đúng)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuizQuestionsResponse {
    private Long miniGameId;
    private String title;
    private String description;
    private Integer questionCount;
    private Integer timeLimit;
    private List<QuizQuestionResponse> questions;

    public static QuizQuestionsResponse fromEntities(MiniGame miniGame, MiniGameQuiz quiz) {
        QuizQuestionsResponse response = new QuizQuestionsResponse();
        response.setMiniGameId(miniGame.getId());
        response.setTitle(miniGame.getTitle());
        response.setDescription(miniGame.getDescription());
        response.setQuestionCount(miniGame.getQuestionCount());
        response.setTimeLimit(miniGame.getTimeLimit());

        if (quiz != null && quiz.getQuestions() != null) {
            response.setQuestions(quiz.getQuestions().stream()
                    .sorted((q1, q2) -> {
                        Integer order1 = q1.getDisplayOrder() != null ? q1.getDisplayOrder() : 0;
                        Integer order2 = q2.getDisplayOrder() != null ? q2.getDisplayOrder() : 0;
                        return order1.compareTo(order2);
                    })
                    .map(QuizQuestionResponse::fromEntity)
                    .collect(Collectors.toList()));
        }

        return response;
    }
}

