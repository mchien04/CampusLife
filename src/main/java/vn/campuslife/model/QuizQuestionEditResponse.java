package vn.campuslife.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.campuslife.entity.MiniGameQuizQuestion;
import vn.campuslife.util.UrlUtils;

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
        return fromEntity(question, null);
    }

    public static QuizQuestionEditResponse fromEntity(MiniGameQuizQuestion question, String publicUrl) {
        QuizQuestionEditResponse response = new QuizQuestionEditResponse();
        response.setId(question.getId());
        response.setQuestionText(question.getQuestionText());
        // Convert relative path to full URL if publicUrl is provided
        response.setImageUrl(UrlUtils.toFullUrl(question.getImageUrl(), publicUrl));
        response.setDisplayOrder(question.getDisplayOrder());
        if (question.getOptions() != null) {
            response.setOptions(question.getOptions().stream()
                    .map(QuizOptionEditResponse::fromEntity)
                    .collect(Collectors.toList()));
        }
        return response;
    }
}

