package vn.campuslife.model;

import lombok.Data;

@Data
public class MiniGameQuizOptionRequest {
    private Long id;
    private String text;
    private boolean isCorrect;
}
