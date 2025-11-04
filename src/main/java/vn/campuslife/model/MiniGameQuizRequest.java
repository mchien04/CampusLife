package vn.campuslife.model;

import lombok.Data;

import java.util.List;

@Data
public class MiniGameQuizRequest {
    private List<MiniGameQuizQuestionRequest> questions;
}

