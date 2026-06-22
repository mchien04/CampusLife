package vn.campuslife.controller.activity;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import vn.campuslife.model.recommend.RecommendedActivityResponse;
import vn.campuslife.service.ActivityRecommendationService;

import java.util.List;

@RestController
@RequestMapping("/api/recommendation")
@RequiredArgsConstructor
public class ActivityRecommendationController {
    private final ActivityRecommendationService recommendationService;
    @GetMapping("/students/{studentId}/recommendations")
    public List<RecommendedActivityResponse> getRecommendations(
            @PathVariable Long studentId,
            @RequestParam(defaultValue = "10") int limit
    ) {
        return recommendationService.recommendForStudent(studentId, limit);
    }
}
