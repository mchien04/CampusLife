package vn.campuslife.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.campuslife.entity.Student;
import vn.campuslife.entity.Semester;
import vn.campuslife.entity.StudentScore;
import vn.campuslife.enumeration.ScoreType;
import vn.campuslife.model.Response;
import vn.campuslife.model.ScoreViewResponse;
import vn.campuslife.repository.*;
import vn.campuslife.service.ScoreService;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScoreServiceImpl implements ScoreService {

    private final StudentRepository studentRepository;
    private final SemesterRepository semesterRepository;
    private final StudentScoreRepository studentScoreRepository;

    @Override
    @Transactional
    public Response calculateTrainingScore(Long studentId, Long semesterId, List<Long> excludedCriterionIds,
            Long enteredByUserId) {
        return new Response(false, "Deprecated: training score by criteria has been removed.", null);
    }

    @Override
    public Response viewScores(Long studentId, Long semesterId) {
        try {
            List<StudentScore> rows = studentScoreRepository.findByStudentAndSemester(studentId, semesterId);

            Map<ScoreType, List<StudentScore>> byType = rows.stream()
                    .collect(Collectors.groupingBy(StudentScore::getScoreType));

            ScoreViewResponse resp = new ScoreViewResponse();
            resp.setStudentId(studentId);
            resp.setSemesterId(semesterId);

            List<ScoreViewResponse.ScoreTypeSummary> summaries = new ArrayList<>();
            for (Map.Entry<ScoreType, List<StudentScore>> e : byType.entrySet()) {
                ScoreViewResponse.ScoreTypeSummary s = new ScoreViewResponse.ScoreTypeSummary();
                s.setScoreType(e.getKey());
                s.setTotal(e.getValue().stream().map(StudentScore::getScore).filter(Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add));

                List<ScoreViewResponse.ScoreItem> items = e.getValue().stream().map(ss -> {
                    ScoreViewResponse.ScoreItem it = new ScoreViewResponse.ScoreItem();
                    it.setScore(ss.getScore());

                    // Parse activityIds JSON
                    List<Long> activityIds = new ArrayList<>();
                    String activityIdsJson = ss.getActivityIds() != null ? ss.getActivityIds() : "[]";
                    try {
                        String content = activityIdsJson.replaceAll("[\\[\\]]", "").trim();
                        if (!content.isEmpty()) {
                            String[] ids = content.split(",");
                            for (String id : ids) {
                                activityIds.add(Long.parseLong(id.trim()));
                            }
                        }
                    } catch (Exception ex) {
                        // Ignore parse error
                    }
                    it.setActivityIds(activityIds);
                    it.setNotes(ss.getNotes());
                    return it;
                }).collect(Collectors.toList());
                s.setItems(items);
                summaries.add(s);
            }

            resp.setSummaries(summaries);
            return new Response(true, "Scores retrieved", resp);
        } catch (Exception e) {
            return new Response(false, "Failed to view scores: " + e.getMessage(), null);
        }
    }

    @Override
    public Response getTotalScore(Long studentId, Long semesterId) {
        try {
            List<StudentScore> rows = studentScoreRepository.findByStudentAndSemester(studentId, semesterId);

            Map<ScoreType, BigDecimal> totalsByType = rows.stream()
                    .filter(ss -> ss.getScore() != null)
                    .collect(Collectors.groupingBy(
                            StudentScore::getScoreType,
                            Collectors.reducing(BigDecimal.ZERO, StudentScore::getScore, BigDecimal::add)));

            BigDecimal grandTotal = totalsByType.values().stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            Map<String, Object> result = new HashMap<>();
            result.put("studentId", studentId);
            result.put("semesterId", semesterId);
            result.put("grandTotal", grandTotal);
            result.put("totalsByType", totalsByType);
            result.put("scoreCount", rows.size());

            return new Response(true, "Total score calculated", result);
        } catch (Exception e) {
            return new Response(false, "Failed to calculate total score: " + e.getMessage(), null);
        }
    }
}
