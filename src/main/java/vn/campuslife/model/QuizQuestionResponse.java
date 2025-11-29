package vn.campuslife.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.campuslife.entity.MiniGameQuizQuestion;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Response DTO cho Quiz Question (KHÔNG có đáp án đúng)
 * Dùng cho API lấy questions để làm quiz
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuizQuestionResponse {
    private Long id;
    private String questionText;
    private Integer displayOrder;
    private List<QuizOptionResponse> options;

    public static QuizQuestionResponse fromEntity(MiniGameQuizQuestion question) {
        QuizQuestionResponse response = new QuizQuestionResponse();
        response.setId(question.getId());
        response.setQuestionText(question.getQuestionText());
        response.setDisplayOrder(question.getDisplayOrder());
        if (question.getOptions() != null) {
            response.setOptions(question.getOptions().stream()
                    .map(QuizOptionResponse::fromEntity)
                    .collect(Collectors.toList()));
        }
        return response;
    }
}

