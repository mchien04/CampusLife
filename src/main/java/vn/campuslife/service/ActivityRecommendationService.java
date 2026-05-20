package vn.campuslife.service;

import vn.campuslife.model.recommend.RecommendedActivityResponse;

import java.util.List;

public interface ActivityRecommendationService {
    List<RecommendedActivityResponse> recommendForStudent(Long studentId, int limit);
}
