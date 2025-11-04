package vn.campuslife.model;
import lombok.Data;
import java.util.List;

@Data
public class MiniGameQuizQuestionRequest {
    private Long id;
    private String questionText;
    private List<MiniGameQuizOptionRequest> options;
}
