package vn.campuslife.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.campuslife.entity.MiniGameQuizQuestion;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Response DTO cho Quiz Question với đáp án đúng (cho admin/manager edit)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuizQuestionEditResponse {
    private Long id;
    private String questionText;
    private String imageUrl;
    private Integer displayOrder;
    private List<QuizOptionEditResponse> options;

    public static QuizQuestionEditResponse fromEntity(MiniGameQuizQuestion question) {
        QuizQuestionEditResponse response = new QuizQuestionEditResponse();
        response.setId(question.getId());
        response.setQuestionText(question.getQuestionText());
        response.setImageUrl(question.getImageUrl());
        response.setDisplayOrder(question.getDisplayOrder());
        if (question.getOptions() != null) {
            response.setOptions(question.getOptions().stream()
                    .map(QuizOptionEditResponse::fromEntity)
                    .collect(Collectors.toList()));
        }
        return response;
    }
}

