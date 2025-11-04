package vn.campuslife.model;

import lombok.Data;
import java.util.List;

@Data
public class MiniGameQuizQuestionResponse {
    private Long id;
    private String questionText;
    private List<MiniGameQuizOptionResponse> options;
}
