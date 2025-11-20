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
import vn.campuslife.model.StudentRankingResponse;
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

    @Override
    @Transactional(readOnly = true)
    public Response getStudentRanking(Long semesterId, ScoreType scoreType, Long departmentId, Long classId, String sortOrder) {
        try {
            // Validate semester
            Optional<Semester> semesterOpt = semesterRepository.findById(semesterId);
            if (semesterOpt.isEmpty()) {
                return Response.error("Semester not found");
            }
            Semester semester = semesterOpt.get();

            // Default sort order là DESC (cao xuống thấp)
            boolean ascending = "ASC".equalsIgnoreCase(sortOrder);

            List<StudentRankingResponse> rankings = new ArrayList<>();

            if (scoreType != null) {
                // Xếp hạng theo một loại điểm cụ thể
                List<StudentScore> scores;
                if (departmentId != null) {
                    scores = studentScoreRepository.findBySemesterIdAndScoreTypeAndDepartmentIdOrderByScoreDesc(
                            semesterId, scoreType, departmentId);
                } else if (classId != null) {
                    scores = studentScoreRepository.findBySemesterIdAndScoreTypeAndClassIdOrderByScoreDesc(
                            semesterId, scoreType, classId);
                } else {
                    scores = studentScoreRepository.findBySemesterIdAndScoreTypeOrderByScoreDesc(
                            semesterId, scoreType);
                }

                // Reverse nếu sort ASC
                if (ascending) {
                    Collections.reverse(scores);
                }

                // Gán rank và convert sang response
                int rank = 1;
                BigDecimal previousScore = null;
                for (int i = 0; i < scores.size(); i++) {
                    StudentScore score = scores.get(i);
                    Student student = score.getStudent();

                    // Xử lý rank: nếu điểm bằng nhau thì cùng rank
                    if (previousScore != null && score.getScore() != null) {
                        if (ascending ? score.getScore().compareTo(previousScore) > 0 
                                : score.getScore().compareTo(previousScore) < 0) {
                            rank = i + 1;
                        }
                    } else if (i > 0) {
                        rank = i + 1;
                    }

                    StudentRankingResponse ranking = new StudentRankingResponse();
                    ranking.setRank(rank);
                    ranking.setStudentId(student.getId());
                    ranking.setStudentCode(student.getStudentCode());
                    ranking.setStudentName(student.getFullName());
                    ranking.setDepartmentId(student.getDepartment() != null ? student.getDepartment().getId() : null);
                    ranking.setDepartmentName(student.getDepartment() != null ? student.getDepartment().getName() : null);
                    ranking.setClassId(student.getStudentClass() != null ? student.getStudentClass().getId() : null);
                    ranking.setClassName(student.getStudentClass() != null ? student.getStudentClass().getClassName() : null);
                    ranking.setSemesterId(semester.getId());
                    ranking.setSemesterName(semester.getName());
                    ranking.setScoreType(scoreType);
                    ranking.setScore(score.getScore() != null ? score.getScore() : BigDecimal.ZERO);
                    ranking.setScoreTypeLabel(getScoreTypeLabel(scoreType));

                    rankings.add(ranking);
                    previousScore = score.getScore();
                }
            } else {
                // Xếp hạng theo tổng điểm tất cả loại
                List<StudentScore> allScores = studentScoreRepository.findBySemesterIdOrderByScoreDesc(semesterId);

                // Group by student và tính tổng điểm
                Map<Long, BigDecimal> totalScoresByStudent = allScores.stream()
                        .filter(ss -> {
                            // Filter theo department và class nếu có
                            if (departmentId != null && (ss.getStudent().getDepartment() == null 
                                    || !ss.getStudent().getDepartment().getId().equals(departmentId))) {
                                return false;
                            }
                            if (classId != null && (ss.getStudent().getStudentClass() == null 
                                    || !ss.getStudent().getStudentClass().getId().equals(classId))) {
                                return false;
                            }
                            return true;
                        })
                        .collect(Collectors.groupingBy(
                                ss -> ss.getStudent().getId(),
                                Collectors.reducing(
                                        BigDecimal.ZERO,
                                        ss -> ss.getScore() != null ? ss.getScore() : BigDecimal.ZERO,
                                        BigDecimal::add)));

                // Sort theo tổng điểm
                List<Map.Entry<Long, BigDecimal>> sortedEntries = totalScoresByStudent.entrySet().stream()
                        .sorted((e1, e2) -> ascending 
                                ? e1.getValue().compareTo(e2.getValue())
                                : e2.getValue().compareTo(e1.getValue()))
                        .collect(Collectors.toList());

                // Gán rank và convert sang response
                int rank = 1;
                BigDecimal previousTotal = null;
                for (int i = 0; i < sortedEntries.size(); i++) {
                    Map.Entry<Long, BigDecimal> entry = sortedEntries.get(i);
                    Long studentId = entry.getKey();
                    BigDecimal totalScore = entry.getValue();

                    // Xử lý rank: nếu điểm bằng nhau thì cùng rank
                    if (previousTotal != null) {
                        if (ascending ? totalScore.compareTo(previousTotal) > 0 
                                : totalScore.compareTo(previousTotal) < 0) {
                            rank = i + 1;
                        }
                    } else if (i > 0) {
                        rank = i + 1;
                    }

                    Optional<Student> studentOpt = studentRepository.findByIdAndIsDeletedFalse(studentId);
                    if (studentOpt.isPresent()) {
                        Student student = studentOpt.get();
                        StudentRankingResponse ranking = new StudentRankingResponse();
                        ranking.setRank(rank);
                        ranking.setStudentId(student.getId());
                        ranking.setStudentCode(student.getStudentCode());
                        ranking.setStudentName(student.getFullName());
                        ranking.setDepartmentId(student.getDepartment() != null ? student.getDepartment().getId() : null);
                        ranking.setDepartmentName(student.getDepartment() != null ? student.getDepartment().getName() : null);
                        ranking.setClassId(student.getStudentClass() != null ? student.getStudentClass().getId() : null);
                        ranking.setClassName(student.getStudentClass() != null ? student.getStudentClass().getClassName() : null);
                        ranking.setSemesterId(semester.getId());
                        ranking.setSemesterName(semester.getName());
                        ranking.setScoreType(null); // Tổng điểm
                        ranking.setScore(totalScore);
                        ranking.setScoreTypeLabel("Tổng điểm");

                        rankings.add(ranking);
                    }
                    previousTotal = totalScore;
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("semesterId", semesterId);
            result.put("semesterName", semester.getName());
            result.put("scoreType", scoreType != null ? scoreType.name() : "TOTAL");
            result.put("departmentId", departmentId);
            result.put("classId", classId);
            result.put("sortOrder", ascending ? "ASC" : "DESC");
            result.put("totalStudents", rankings.size());
            result.put("rankings", rankings);

            return Response.success("Bảng xếp hạng điểm sinh viên", result);
        } catch (Exception e) {
            return Response.error("Failed to get student ranking: " + e.getMessage());
        }
    }

    /**
     * Helper method để lấy label cho ScoreType
     */
    private String getScoreTypeLabel(ScoreType scoreType) {
        if (scoreType == null) {
            return "Tổng điểm";
        }
        switch (scoreType) {
            case REN_LUYEN:
                return "Điểm rèn luyện";
            case CONG_TAC_XA_HOI:
                return "Điểm công tác xã hội";
            case CHUYEN_DE:
                return "Điểm chuyên đề doanh nghiệp";
            case KHAC:
                return "Điểm khác";
            default:
                return scoreType.name();
        }
    }
}
