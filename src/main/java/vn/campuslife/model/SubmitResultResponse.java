package vn.campuslife.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SubmitResultResponse {
    private Long attemptId;
    private Integer correctCount;
    private Boolean passed;
}