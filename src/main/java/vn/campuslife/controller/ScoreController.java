package vn.campuslife.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.campuslife.model.Response;
import vn.campuslife.service.ScoreService;

import java.util.List;

@RestController
@RequestMapping("/api/scores")
@RequiredArgsConstructor
public class ScoreController {

    private final ScoreService scoreService;

    @PostMapping("/training/calculate")
    public ResponseEntity<Response> calculateTraining(@RequestParam Long studentId,
            @RequestParam Long semesterId,
            @RequestBody(required = false) List<Long> excludedCriterionIds,
            Authentication authentication) {
        Long userId = 1L; // TODO: map from auth
        Response resp = scoreService.calculateTrainingScore(studentId, semesterId, excludedCriterionIds, userId);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/student/{studentId}/semester/{semesterId}")
    public ResponseEntity<Response> viewScores(@PathVariable Long studentId, @PathVariable Long semesterId) {
        Response resp = scoreService.viewScores(studentId, semesterId);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/student/{studentId}/semester/{semesterId}/total")
    public ResponseEntity<Response> getTotalScore(@PathVariable Long studentId, @PathVariable Long semesterId) {
        Response resp = scoreService.getTotalScore(studentId, semesterId);
        return ResponseEntity.ok(resp);
    }
}
