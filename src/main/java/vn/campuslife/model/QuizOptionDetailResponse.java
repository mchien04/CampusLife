package vn.campuslife.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.campuslife.entity.MiniGameQuizOption;

/**
 * Response DTO cho Quiz Option với đáp án đúng
 * Dùng cho API xem chi tiết attempt
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuizOptionDetailResponse {
    private Long id;
    private String text;
    private Boolean isCorrect;
    private Boolean isSelected;

    public static QuizOptionDetailResponse fromEntity(MiniGameQuizOption option, Boolean isSelected) {
        QuizOptionDetailResponse response = new QuizOptionDetailResponse();
        response.setId(option.getId());
        response.setText(option.getText());
        response.setIsCorrect(option.isCorrect());
        response.setIsSelected(isSelected != null && isSelected);
        return response;
    }
}

