package vn.campuslife.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.campuslife.entity.MiniGameQuizQuestion;
import vn.campuslife.util.UrlUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Response DTO cho Quiz Question với đáp án đúng và kết quả
 * Dùng cho API xem chi tiết attempt
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuizQuestionDetailResponse {
    private Long id;
    private String questionText;
    private String imageUrl;
    private Integer displayOrder;
    private List<QuizOptionDetailResponse> options;
    private Long correctOptionId;
    private Long selectedOptionId;
    private Boolean isCorrect;

    public static QuizQuestionDetailResponse fromEntity(
            MiniGameQuizQuestion question,
            Map<Long, Long> studentAnswers) {
        return fromEntity(question, studentAnswers, null);
    }

    public static QuizQuestionDetailResponse fromEntity(
            MiniGameQuizQuestion question,
            Map<Long, Long> studentAnswers,
            String publicUrl) {
        QuizQuestionDetailResponse response = new QuizQuestionDetailResponse();
        response.setId(question.getId());
        response.setQuestionText(question.getQuestionText());
        // Convert relative path to full URL if publicUrl is provided
        response.setImageUrl(UrlUtils.toFullUrl(question.getImageUrl(), publicUrl));
        response.setDisplayOrder(question.getDisplayOrder());

        // Tìm đáp án đúng
        Long correctOptionId = null;
        if (question.getOptions() != null) {
            correctOptionId = question.getOptions().stream()
                    .filter(opt -> opt.isCorrect())
                    .map(opt -> opt.getId())
                    .findFirst()
                    .orElse(null);

            // Map options với isSelected
            Long selectedOptionId = studentAnswers != null ? studentAnswers.get(question.getId()) : null;
            response.setOptions(question.getOptions().stream()
                    .map(opt -> QuizOptionDetailResponse.fromEntity(
                            opt,
                            opt.getId().equals(selectedOptionId)))
                    .collect(Collectors.toList()));
        }

        response.setCorrectOptionId(correctOptionId);
        response.setSelectedOptionId(studentAnswers != null ? studentAnswers.get(question.getId()) : null);
        response.setIsCorrect(correctOptionId != null &&
                correctOptionId.equals(studentAnswers != null ? studentAnswers.get(question.getId()) : null));

        return response;
    }
}

