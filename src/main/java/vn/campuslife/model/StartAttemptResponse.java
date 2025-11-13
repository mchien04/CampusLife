package vn.campuslife.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class StartAttemptResponse {
    private Long attemptId;
    private String title;
    private Integer questionCount;
    private Integer timeLimit;
    private List<MiniGameQuizQuestionResponse> questions;
}

