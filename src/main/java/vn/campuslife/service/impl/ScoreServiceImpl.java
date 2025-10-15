package vn.campuslife.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.campuslife.entity.*;
import vn.campuslife.enumeration.ScoreSourceType;
import vn.campuslife.enumeration.ScoreType;
import vn.campuslife.model.Response;
import vn.campuslife.model.ScoreViewResponse;
import vn.campuslife.repository.*;
import vn.campuslife.service.ScoreService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScoreServiceImpl implements ScoreService {

    private final StudentRepository studentRepository;
    private final SemesterRepository semesterRepository;
    private final CriterionRepository criterionRepository;
    private final StudentScoreRepository studentScoreRepository;
    private final UserRepository userRepository;
    private final ScoreHistoryRepository scoreHistoryRepository;

    @Override
    @Transactional
    public Response calculateTrainingScore(Long studentId, Long semesterId, List<Long> excludedCriterionIds,
            Long enteredByUserId) {
        try {
            Optional<Student> studentOpt = studentRepository.findByIdAndIsDeletedFalse(studentId);
            if (studentOpt.isEmpty())
                return new Response(false, "Student not found", null);
            Optional<Semester> semesterOpt = semesterRepository.findById(semesterId);
            if (semesterOpt.isEmpty())
                return new Response(false, "Semester not found", null);
            Optional<User> enteredByOpt = userRepository.findById(enteredByUserId);
            if (enteredByOpt.isEmpty())
                return new Response(false, "User not found", null);

            Set<Long> excluded = excludedCriterionIds == null ? Set.of() : new HashSet<>(excludedCriterionIds);

            // Lấy tất cả criterion thuộc nhóm rèn luyện
            List<Criterion> criteria = criterionRepository.findAllTrainingCriteria();

            BigDecimal total = BigDecimal.ZERO;
            List<StudentScore> scoresToSave = new ArrayList<>();
            List<ScoreHistory> histories = new ArrayList<>();

            for (Criterion c : criteria) {
                boolean ok = !excluded.contains(c.getId());
                BigDecimal given = ok && c.getMaxScore() != null ? c.getMaxScore() : BigDecimal.ZERO;

                StudentScore ss = new StudentScore();
                ss.setStudent(studentOpt.get());
                ss.setSemester(semesterOpt.get());
                ss.setCriterion(c);
                ss.setScore(given);
                ss.setEnteredBy(enteredByOpt.get());
                ss.setEntryDate(LocalDateTime.now());
                ss.setUpdatedDate(LocalDateTime.now());
                ss.setScoreType(ScoreType.REN_LUYEN);
                ss.setScoreSourceType(ScoreSourceType.MANUAL);
                ss.setSourceNote("Training default criteria calculation");

                total = total.add(given);
                scoresToSave.add(ss);

                ScoreHistory h = new ScoreHistory();
                h.setScore(ss);
                h.setOldScore(BigDecimal.ZERO);
                h.setNewScore(given);
                h.setChangedBy(enteredByOpt.get());
                h.setChangeDate(LocalDateTime.now());
                h.setScoreSourceType(ScoreSourceType.MANUAL);
                h.setReason(ok ? "Full score for criterion" : "Excluded criterion");
                histories.add(h);
            }

            studentScoreRepository.saveAll(scoresToSave);
            scoreHistoryRepository.saveAll(histories);

            Map<String, Object> payload = new HashMap<>();
            payload.put("total", total);
            payload.put("items", scoresToSave.stream().map(s -> Map.of(
                    "criterionId", s.getCriterion().getId(),
                    "criterionName", s.getCriterion().getName(),
                    "score", s.getScore())).collect(Collectors.toList()));

            return new Response(true, "Training score calculated", payload);
        } catch (Exception e) {
            return new Response(false, "Failed to calculate training score: " + e.getMessage(), null);
        }
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
                    it.setSourceType(ss.getScoreSourceType());
                    it.setActivityId(ss.getActivityId());
                    it.setTaskId(ss.getTaskId());
                    it.setSubmissionId(ss.getSubmissionId());
                    it.setSourceNote(ss.getSourceNote());
                    it.setCriterionId(ss.getCriterion() != null ? ss.getCriterion().getId() : null);
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
