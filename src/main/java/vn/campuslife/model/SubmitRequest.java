package vn.campuslife.model;

import lombok.Data;

import java.util.List;

@Data
public class SubmitRequest {
    private Long attemptId;
    private List<Ans> answers;
    @Data public static class Ans { private Long questionId; private Long optionId; }
}

