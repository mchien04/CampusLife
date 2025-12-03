package vn.campuslife.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.campuslife.entity.MiniGameQuizOption;

/**
 * Response DTO cho Quiz Option với đáp án đúng (cho admin/manager edit)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuizOptionEditResponse {
    private Long id;
    private String text;
    private Boolean isCorrect;

    public static QuizOptionEditResponse fromEntity(MiniGameQuizOption option) {
        QuizOptionEditResponse response = new QuizOptionEditResponse();
        response.setId(option.getId());
        response.setText(option.getText());
        response.setIsCorrect(option.isCorrect());
        return response;
    }
}

