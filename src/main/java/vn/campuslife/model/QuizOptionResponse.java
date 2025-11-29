package vn.campuslife.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.campuslife.entity.MiniGameQuizOption;

/**
 * Response DTO cho Quiz Option (KHÔNG có đáp án đúng)
 * Dùng cho API lấy questions để làm quiz
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuizOptionResponse {
    private Long id;
    private String text;

    public static QuizOptionResponse fromEntity(MiniGameQuizOption option) {
        QuizOptionResponse response = new QuizOptionResponse();
        response.setId(option.getId());
        response.setText(option.getText());
        return response;
    }
}

