package vn.campuslife.service;

import vn.campuslife.model.Response;

import java.util.List;

public interface ScoreService {
    Response calculateTrainingScore(Long studentId, Long semesterId, List<Long> excludedCriterionIds,
            Long enteredByUserId);

    Response viewScores(Long studentId, Long semesterId);
}
