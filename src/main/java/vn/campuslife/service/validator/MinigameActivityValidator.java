package vn.campuslife.service.validator;

import org.springframework.stereotype.Component;
import vn.campuslife.model.activity.minigame.MinigameActivityCreateRequest;
import vn.campuslife.model.activity.quiz.CreateMiniGameRequest;

@Component
public class MinigameActivityValidator implements ActivityValidator<MinigameActivityCreateRequest> {

    @Override
    public void validate(MinigameActivityCreateRequest request) {
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Minigame activity name is required");
        }
        if (request.getStartDate() == null || request.getEndDate() == null) {
            throw new IllegalArgumentException("Start date and end date are required");
        }
        if (request.getStartDate().isAfter(request.getEndDate()) || request.getStartDate().isEqual(request.getEndDate())) {
            throw new IllegalArgumentException("Start date must be before end date");
        }
        
        if (request.getRegistrationStartDate() != null && request.getRegistrationDeadline() != null) {
            if (request.getRegistrationStartDate().isAfter(request.getRegistrationDeadline())) {
                throw new IllegalArgumentException("Registration start date must be before deadline");
            }
        }
        
        if (request.getQuiz() == null) {
            throw new IllegalArgumentException("Quiz config is required for Minigame");
        }
        
        var quiz = request.getQuiz();
        if (quiz.getTitle() == null || quiz.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Quiz title is required");
        }
        if (quiz.getQuestionCount() == null || quiz.getQuestionCount() < 1) {
            throw new IllegalArgumentException("Question count must be at least 1");
        }
        if (quiz.getQuestions() == null || quiz.getQuestions().isEmpty()) {
            throw new IllegalArgumentException("Quiz questions are required");
        }
        
        for (int i = 0; i < quiz.getQuestions().size(); i++) {
            CreateMiniGameRequest.QuestionRequest q = quiz.getQuestions().get(i);
            if (q.getQuestionText() == null || q.getQuestionText().trim().isEmpty()) {
                throw new IllegalArgumentException("Question " + (i+1) + " text is required");
            }
            if (q.getOptions() == null || q.getOptions().size() < 2) {
                throw new IllegalArgumentException("Question " + (i+1) + " must have at least 2 options");
            }
            boolean hasCorrect = q.getOptions().stream()
                    .anyMatch(opt -> opt.getIsCorrect() != null && opt.getIsCorrect());
            if (!hasCorrect) {
                throw new IllegalArgumentException("Question " + (i+1) + " must have at least 1 correct option");
            }
        }
    }
}
