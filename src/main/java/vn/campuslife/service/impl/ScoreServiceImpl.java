package vn.campuslife.service.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.campuslife.entity.Activity;
import vn.campuslife.entity.ActivityParticipation;
import vn.campuslife.entity.ActivitySeries;
import vn.campuslife.entity.ScoreEntry;
import vn.campuslife.entity.Semester;
import vn.campuslife.entity.Student;
import vn.campuslife.entity.StudentScore;
import vn.campuslife.enumeration.ParticipationType;
import vn.campuslife.enumeration.Role;
import vn.campuslife.enumeration.ScoreEntryStatus;
import vn.campuslife.enumeration.ScoreEntrySourceType;
import vn.campuslife.enumeration.ScoreType;
import vn.campuslife.model.Response;
import vn.campuslife.model.StudentRankingResponse;
import vn.campuslife.model.activity.ActivityParticipationDetailResponse;
import vn.campuslife.model.score.ScoreViewResponse;
import vn.campuslife.model.score.ScoreHistoryDetailResponse;
import vn.campuslife.model.score.ScoreHistoryViewResponse;
import vn.campuslife.repository.ActivityParticipationRepository;
import vn.campuslife.repository.ActivityRepository;
import vn.campuslife.repository.ActivitySeriesRepository;
import vn.campuslife.repository.ScoreEntryRepository;
import vn.campuslife.repository.SemesterRepository;
import vn.campuslife.repository.StudentRepository;
import vn.campuslife.repository.StudentScoreRepository;
import vn.campuslife.repository.StudentSeriesProgressRepository;
import vn.campuslife.repository.UserRepository;
import vn.campuslife.security.department.DepartmentAuthorizationService;
import vn.campuslife.security.department.DepartmentScope;
import vn.campuslife.security.department.DepartmentScopeSpec;
import vn.campuslife.service.ScoreService;
import vn.campuslife.service.SemesterHelperService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScoreServiceImpl implements ScoreService {

    private static final Logger logger = LoggerFactory.getLogger(ScoreServiceImpl.class);

    private final StudentRepository studentRepository;
    private final SemesterRepository semesterRepository;
    private final StudentScoreRepository studentScoreRepository;
    private final ActivityParticipationRepository participationRepository;
    private final StudentSeriesProgressRepository progressRepository;
    private final ScoreEntryRepository scoreEntryRepository;
    private final UserRepository userRepository;
    private final ActivityRepository activityRepository;
    private final ActivitySeriesRepository seriesRepository;
    private final SemesterHelperService semesterHelperService;
    private final vn.campuslife.service.ScoreEntryService scoreEntryService;
    private final DepartmentAuthorizationService departmentAuthorizationService;

    @Override
    @Transactional
    public Response calculateTrainingScore(Long studentId, Long semesterId, List<Long> excludedCriterionIds,
            Long enteredByUserId) {
        return new Response(false, "Deprecated: training score by criteria has been removed.", null);
    }

    @Override
    public Response viewScores(Long studentId, Long semesterId) {
        return viewScores(studentId, semesterId, null);
    }

    @Override
    public Response viewScores(Long studentId, Long semesterId, DepartmentScope scope) {
        try {
            guardStudentScoreAccess(studentId, scope);
            List<StudentScore> rows = studentScoreRepository.findByStudentAndSemester(studentId, semesterId);

            Map<ScoreType, List<StudentScore>> byType = rows.stream()
                    .collect(Collectors.groupingBy(StudentScore::getScoreType));

            ScoreViewResponse resp = new ScoreViewResponse();
            resp.setStudentId(studentId);
            resp.setSemesterId(semesterId);

            List<ScoreViewResponse.ScoreTypeSummary> summaries = new ArrayList<>();
            for (Map.Entry<ScoreType, List<StudentScore>> e : byType.entrySet()) {
                ScoreViewResponse.ScoreTypeSummary s = new ScoreViewResponse.ScoreTypeSummary();
                ScoreType scoreType = e.getKey();
                s.setScoreType(scoreType);
                s.setTotal(e.getValue().stream().map(StudentScore::getScore).filter(Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add));

                // Tính tổng tích lũy suốt các học kỳ cho loại điểm cumulative
                if (scoreType.isCumulative()) {
                    BigDecimal cumulativeTotal = studentScoreRepository.sumScoreByStudentIdAndScoreType(studentId, scoreType);
                    s.setCumulativeTotal(cumulativeTotal);
                }

                List<ScoreViewResponse.ScoreItem> items = e.getValue().stream().map(ss -> {
                    ScoreViewResponse.ScoreItem it = new ScoreViewResponse.ScoreItem();
                    it.setScore(ss.getScore());
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
        return getTotalScore(studentId, semesterId, null);
    }

    @Override
    public Response getTotalScore(Long studentId, Long semesterId, DepartmentScope scope) {
        try {
            guardStudentScoreAccess(studentId, scope);
            List<StudentScore> rows = studentScoreRepository.findByStudentAndSemester(studentId, semesterId);

            Map<ScoreType, BigDecimal> totalsByType = rows.stream()
                    .filter(ss -> ss.getScore() != null)
                    .collect(Collectors.groupingBy(
                            StudentScore::getScoreType,
                            Collectors.reducing(BigDecimal.ZERO, StudentScore::getScore, BigDecimal::add)));

            BigDecimal grandTotal = totalsByType.values().stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Tính tổng tích lũy cross-semester cho các loại điểm cumulative
            Map<ScoreType, BigDecimal> cumulativeTotals = new HashMap<>();
            for (ScoreType scoreType : ScoreType.values()) {
                if (scoreType.isCumulative()) {
                    BigDecimal cumulativeTotal = studentScoreRepository.sumScoreByStudentIdAndScoreType(studentId, scoreType);
                    cumulativeTotals.put(scoreType, cumulativeTotal);
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("studentId", studentId);
            result.put("semesterId", semesterId);
            result.put("grandTotal", grandTotal);
            result.put("totalsByType", totalsByType);
            result.put("cumulativeTotals", cumulativeTotals);
            result.put("scoreCount", rows.size());

            return new Response(true, "Total score calculated", result);
        } catch (Exception e) {
            return new Response(false, "Failed to calculate total score: " + e.getMessage(), null);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Response getStudentRanking(Long semesterId, ScoreType scoreType, Long departmentId, Long classId, String sortOrder) {
        return getStudentRanking(semesterId, scoreType, departmentId, classId, sortOrder, null);
    }

    @Override
    @Transactional(readOnly = true)
    public Response getStudentRanking(Long semesterId, ScoreType scoreType, Long departmentId, Long classId, String sortOrder,
                                      DepartmentScope scope) {
        try {
            Set<Long> managerDeptFilter = null;
            if (scope != null && scope.manager()) {
                managerDeptFilter = departmentAuthorizationService.managerDepartmentFilter(scope, departmentId);
                if (classId != null) {
                    departmentAuthorizationService.requireStudentClassAccess(classId, scope);
                }
                if (managerDeptFilter.size() == 1) {
                    departmentId = managerDeptFilter.iterator().next();
                } else {
                    departmentId = null;
                }
            }
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
                if (managerDeptFilter != null && managerDeptFilter.size() > 1) {
                    Set<Long> allowedDeptIds = managerDeptFilter;
                    scores = scores.stream()
                            .filter(score -> score.getStudent().getDepartment() != null
                                    && allowedDeptIds.contains(score.getStudent().getDepartment().getId()))
                            .collect(Collectors.toList());
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
                if (managerDeptFilter != null) {
                    Set<Long> allowedDeptIds = managerDeptFilter;
                    allScores = allScores.stream()
                            .filter(score -> score.getStudent().getDepartment() != null
                                    && allowedDeptIds.contains(score.getStudent().getDepartment().getId()))
                            .collect(Collectors.toList());
                }

                // Group by student và tính tổng điểm
                Long effectiveDepartmentId = departmentId;
                Long effectiveClassId = classId;
                Map<Long, BigDecimal> totalScoresByStudent = allScores.stream()
                        .filter(ss -> {
                            // Filter theo department và class nếu có
                            if (effectiveDepartmentId != null && (ss.getStudent().getDepartment() == null
                                    || !ss.getStudent().getDepartment().getId().equals(effectiveDepartmentId))) {
                                return false;
                            }
                            if (effectiveClassId != null && (ss.getStudent().getStudentClass() == null
                                    || !ss.getStudent().getStudentClass().getId().equals(effectiveClassId))) {
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
            default:
                return scoreType.name();
        }
    }

    private void guardStudentScoreAccess(Long studentId, DepartmentScope scope) {
        if (scope != null) {
            departmentAuthorizationService.requireStudentAccess(studentId, scope);
        }
    }

    @Override
    @Transactional
    public Response recalculateStudentScore(Long studentId, Long semesterId) {
        return recalculateStudentScore(studentId, semesterId, null);
    }

    @Override
    @Transactional
    public Response recalculateStudentScore(Long studentId, Long semesterId, DepartmentScope scope) {
        try {
            guardStudentScoreAccess(studentId, scope);
            Optional<Student> studentOpt = studentRepository.findByIdAndIsDeletedFalse(studentId);
            if (studentOpt.isEmpty()) {
                return Response.error("Student not found");
            }

            Semester semester;
            if (semesterId != null) {
                Optional<Semester> semesterOpt = semesterRepository.findById(semesterId);
                if (semesterOpt.isEmpty()) {
                    return Response.error("Semester not found");
                }
                semester = semesterOpt.get();
            } else {
                semester = semesterRepository.findAll().stream()
                        .filter(Semester::isOpen)
                        .findFirst()
                        .orElse(semesterRepository.findAll().stream().findFirst().orElse(null));
                if (semester == null) {
                    return Response.error("No semester found");
                }
            }

            for (ScoreType type : ScoreType.values()) {
                scoreEntryService.refreshStudentScore(studentId, semester.getId(), type);
            }

            return Response.success("Score recalculated successfully");
        } catch (Exception e) {
            logger.error("Failed to recalculate score", e);
            return Response.error("Failed to recalculate score: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Response recalculateAllStudentScores(Long semesterId) {
        return recalculateAllStudentScores(semesterId, null);
    }

    @Override
    @Transactional
    public Response recalculateAllStudentScores(Long semesterId, DepartmentScope scope) {
        try {
            // Get semester
            Semester semester;
            if (semesterId != null) {
                Optional<Semester> semesterOpt = semesterRepository.findById(semesterId);
                if (semesterOpt.isEmpty()) {
                    return Response.error("Semester not found");
                }
                semester = semesterOpt.get();
            } else {
                // Get current semester
                semester = semesterRepository.findAll().stream()
                        .filter(Semester::isOpen)
                        .findFirst()
                        .orElse(semesterRepository.findAll().stream().findFirst().orElse(null));
                if (semester == null) {
                    return Response.error("No semester found");
                }
            }

            // Get all active students
            List<Student> students = scope != null && scope.manager()
                    ? studentRepository.findAll(DepartmentScopeSpec.student(scope.departmentIds()))
                    : studentRepository.findAll()
                            .stream()
                            .filter(s -> !s.isDeleted())
                            .collect(Collectors.toList());

            Map<String, Object> result = new HashMap<>();
            result.put("semesterId", semester.getId());
            result.put("semesterName", semester.getName());
            result.put("totalStudents", students.size());

            int successCount = 0;
            int errorCount = 0;
            List<Map<String, Object>> errors = new ArrayList<>();

            for (Student student : students) {
                try {
                    Response recalcResponse = recalculateStudentScore(student.getId(), semester.getId(), scope);
                    if (recalcResponse.isStatus()) {
                        successCount++;
                    } else {
                        errorCount++;
                        Map<String, Object> error = new HashMap<>();
                        error.put("studentId", student.getId());
                        error.put("studentCode", student.getStudentCode());
                        error.put("error", recalcResponse.getMessage());
                        errors.add(error);
                    }
                } catch (Exception e) {
                    errorCount++;
                    Map<String, Object> error = new HashMap<>();
                    error.put("studentId", student.getId());
                    error.put("studentCode", student.getStudentCode());
                    error.put("error", e.getMessage());
                    errors.add(error);
                    logger.error("Failed to recalculate score for student {}: {}", student.getId(), e.getMessage());
                }
            }

            result.put("successCount", successCount);
            result.put("errorCount", errorCount);
            result.put("errors", errors);

            logger.info("Recalculated scores for {} students (success: {}, errors: {})", 
                    students.size(), successCount, errorCount);

            return Response.success("Recalculated all student scores", result);
        } catch (Exception e) {
            logger.error("Failed to recalculate all student scores: {}", e.getMessage(), e);
            return Response.error("Failed to recalculate all student scores: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Response getScoreHistory(Long studentId, Long semesterId, ScoreType scoreType, Integer page, Integer size, Long requestingStudentId,
                                    LocalDateTime startDate, LocalDateTime endDate, String keyword) {
        return getScoreHistory(studentId, semesterId, scoreType, page, size, requestingStudentId,
                startDate, endDate, keyword, null);
    }

    @Override
    @Transactional(readOnly = true)
    public Response getScoreHistory(Long studentId, Long semesterId, ScoreType scoreType, Integer page, Integer size, Long requestingStudentId,
                                    LocalDateTime startDate, LocalDateTime endDate, String keyword, DepartmentScope scope) {
        try {
            guardStudentScoreAccess(studentId, scope);
            // Validate student
            Optional<Student> studentOpt = studentRepository.findByIdAndIsDeletedFalse(studentId);
            if (studentOpt.isEmpty()) {
                return Response.error("Student not found");
            }
            Student student = studentOpt.get();

            // Access control: Student can only view their own history
            if (requestingStudentId != null && !requestingStudentId.equals(studentId)) {
                return Response.error("You can only view your own score history");
            }

            // Validate semester
            Optional<Semester> semesterOpt = semesterRepository.findById(semesterId);
            if (semesterOpt.isEmpty()) {
                return Response.error("Semester not found");
            }
            Semester semester = semesterOpt.get();

            // Set default pagination
            int pageNum = (page != null && page >= 0) ? page : 0;
            int pageSize = (size != null && size > 0) ? size : 20;
            Pageable pageable = PageRequest.of(pageNum, pageSize);

            // Get current score
            BigDecimal currentScore = BigDecimal.ZERO;
            if (scoreType != null) {
                Optional<StudentScore> scoreOpt = studentScoreRepository
                        .findByStudentIdAndSemesterIdAndScoreType(studentId, semesterId, scoreType);
                if (scoreOpt.isPresent()) {
                    currentScore = scoreOpt.get().getScore() != null ? scoreOpt.get().getScore() : BigDecimal.ZERO;
                }
            }

            // DB-level pagination (Option A: aggregate-offset + DB page)
            Page<ScoreEntry> scoreEntryPage;
            if (scoreType != null) {
                scoreEntryPage = scoreEntryRepository.findWithActivityByStudentAndSemesterAndScoreType(
                        studentId, semesterId, scoreType, ScoreEntryStatus.ACTIVE, pageable);
            } else {
                scoreEntryPage = scoreEntryRepository.findWithActivityByStudentAndSemester(
                        studentId, semesterId, ScoreEntryStatus.ACTIVE, pageable);
            }

            List<ScoreEntry> pageEntries = scoreEntryPage.getContent();

            // Compute prior total (sum of all entries OLDER than this page's oldest entry)
            BigDecimal priorTotal = BigDecimal.ZERO;
            if (!pageEntries.isEmpty()) {
                ScoreEntry lastEntry = pageEntries.get(pageEntries.size() - 1);
                LocalDateTime cutoffTime = lastEntry.getCreatedAt();
                Long cutoffId = lastEntry.getId();
                if (scoreType != null) {
                    priorTotal = scoreEntryRepository.sumPointsBeforeCutoffWithScoreType(
                            studentId, semesterId, scoreType, ScoreEntryStatus.ACTIVE, cutoffTime, cutoffId);
                } else {
                    priorTotal = scoreEntryRepository.sumPointsBeforeCutoff(
                            studentId, semesterId, ScoreEntryStatus.ACTIVE, cutoffTime, cutoffId);
                }
            }

            // Batch-load series and progress to fix N+1
            Set<Long> seriesIds = new HashSet<>();
            Set<Long> progressIds = new HashSet<>();
            for (ScoreEntry entry : pageEntries) {
                if (entry.getActivity() != null && entry.getActivity().getSeriesId() != null) {
                    seriesIds.add(entry.getActivity().getSeriesId());
                }
                if (entry.getSourceType() == ScoreEntrySourceType.SERIES_PROGRESS && entry.getSourceId() != null) {
                    progressIds.add(entry.getSourceId());
                }
            }
            Map<Long, ActivitySeries> seriesMap = new HashMap<>();
            if (!seriesIds.isEmpty()) {
                seriesRepository.findAllById(seriesIds).forEach(s -> seriesMap.put(s.getId(), s));
            }
            Map<Long, vn.campuslife.entity.StudentSeriesProgress> progressMap = new HashMap<>();
            if (!progressIds.isEmpty()) {
                progressRepository.findAllById(progressIds).forEach(p -> progressMap.put(p.getId(), p));
            }

            // Build score history responses with running total
            List<ScoreHistoryDetailResponse> scoreHistoryResponses = new ArrayList<>();
            BigDecimal runningScore = priorTotal;
            for (ScoreEntry entry : pageEntries) {
                ScoreHistoryDetailResponse response = new ScoreHistoryDetailResponse();
                response.setId(entry.getId());
                response.setOldScore(runningScore);

                BigDecimal delta = entry.getPoints() != null ? entry.getPoints() : BigDecimal.ZERO;
                runningScore = runningScore.add(delta);

                response.setNewScore(runningScore);
                response.setChangeDate(entry.getCreatedAt() != null ? entry.getCreatedAt() : entry.getUpdatedAt());
                response.setReason(entry.getReason());
                response.setSourceType(entry.getSourceType().name());

                if (entry.getActivity() != null) {
                    response.setActivityId(entry.getActivity().getId());
                    response.setActivityName(entry.getActivity().getName());
                    if (entry.getActivity().getSeriesId() != null) {
                        response.setSeriesId(entry.getActivity().getSeriesId());
                        ActivitySeries series = seriesMap.get(entry.getActivity().getSeriesId());
                        if (series != null) {
                            response.setSeriesName(series.getName());
                        }
                    }
                }

                if (entry.getSourceType() == ScoreEntrySourceType.SERIES_PROGRESS) {
                    vn.campuslife.entity.StudentSeriesProgress progress = progressMap.get(entry.getSourceId());
                    if (progress != null) {
                        response.setSeriesId(progress.getSeries().getId());
                        response.setSeriesName(progress.getSeries().getName());
                    }
                }

                if (entry.getCreatedBy() != null) {
                    response.setChangedByUsername(entry.getCreatedBy().getUsername());
                    response.setChangedByFullName(null);
                }

                scoreHistoryResponses.add(response);
            }

            int historyTotalPages = scoreEntryPage.getTotalPages();

            // Get ActivityParticipation COMPLETED — filter by semester date range
            Page<ActivityParticipation> participationPage;
            if (semester.getStartDate() == null || semester.getEndDate() == null) {
                participationPage = Page.empty(pageable);
            } else {
                LocalDateTime rangeStart = semester.getStartDate().atStartOfDay();
                LocalDateTime rangeEndExclusive = semester.getEndDate().plusDays(1).atStartOfDay();
                if (scoreType != null) {
                    participationPage = participationRepository
                            .findByRegistration_StudentIdAndRegistration_Activity_ScoreType(
                                    studentId, scoreType, rangeStart, rangeEndExclusive, pageable);
                } else {
                    participationPage = participationRepository
                            .findByRegistration_StudentId_Completed(
                                    studentId, rangeStart, rangeEndExclusive, pageable);
                }
            }

            // Convert ActivityParticipation to DTO (batch-load series + ledger points)
            Set<Long> participationSeriesIds = new HashSet<>();
            Set<Long> participationActivityIds = new HashSet<>();
            for (ActivityParticipation ap : participationPage.getContent()) {
                Activity act = ap.getRegistration().getActivity();
                if (act.getId() != null) {
                    participationActivityIds.add(act.getId());
                }
                if (act.getSeriesId() != null) {
                    participationSeriesIds.add(act.getSeriesId());
                }
            }
            if (!participationSeriesIds.isEmpty()) {
                seriesRepository.findAllById(participationSeriesIds).forEach(s -> seriesMap.put(s.getId(), s));
            }

            Map<Long, BigDecimal> ledgerPointsByActivity = new HashMap<>();
            if (!participationActivityIds.isEmpty()) {
                List<Object[]> sums = scoreType != null
                        ? scoreEntryRepository.sumPointsByStudentSemesterScoreTypeAndActivityIds(
                                studentId, semesterId, scoreType, ScoreEntryStatus.ACTIVE, participationActivityIds)
                        : scoreEntryRepository.sumPointsByStudentSemesterAndActivityIds(
                                studentId, semesterId, ScoreEntryStatus.ACTIVE, participationActivityIds);
                for (Object[] row : sums) {
                    if (row[0] != null) {
                        ledgerPointsByActivity.put((Long) row[0],
                                row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO);
                    }
                }
            }

            List<ActivityParticipationDetailResponse> participationResponses = participationPage.getContent().stream()
                    .map(ap -> {
                        ActivityParticipationDetailResponse response = new ActivityParticipationDetailResponse();
                        response.setId(ap.getId());
                        Activity activity = ap.getRegistration().getActivity();
                        response.setActivityId(activity.getId());
                        response.setActivityName(activity.getName());
                        response.setActivityType(activity.getType());
                        // Prefer ledger total (source of truth); fall back to denormalized column
                        BigDecimal ledgerPoints = ledgerPointsByActivity.get(activity.getId());
                        response.setPointsEarned(ledgerPoints != null
                                ? ledgerPoints
                                : (ap.getPointsEarned() != null ? ap.getPointsEarned() : BigDecimal.ZERO));
                        response.setParticipationType(ap.getParticipationType());
                        response.setDate(ap.getDate());
                        response.setIsCompleted(ap.getIsCompleted());

                        // Determine sourceType
                        if (activity.getType() != null && activity.getType() == vn.campuslife.enumeration.ActivityType.MINIGAME) {
                            response.setSourceType("MINIGAME");
                        } else {
                            response.setSourceType("ACTIVITY");
                        }

                        // Get series info if activity belongs to series
                        if (activity.getSeriesId() != null) {
                            response.setSeriesId(activity.getSeriesId());
                            ActivitySeries series = seriesMap.get(activity.getSeriesId());
                            if (series != null) {
                                response.setSeriesName(series.getName());
                            }
                        }

                        return response;
                    })
                    .collect(Collectors.toList());

            // Build response
            ScoreHistoryViewResponse viewResponse = new ScoreHistoryViewResponse();
            viewResponse.setStudentId(studentId);
            viewResponse.setStudentCode(student.getStudentCode());
            viewResponse.setStudentName(student.getFullName());
            viewResponse.setSemesterId(semesterId);
            viewResponse.setSemesterName(semester.getName());
            viewResponse.setScoreType(scoreType);
            viewResponse.setCurrentScore(currentScore);
            viewResponse.setScoreHistories(scoreHistoryResponses);
            viewResponse.setActivityParticipations(participationResponses);
            
            // Calculate total records (combine both)
            long totalRecords = scoreEntryPage.getTotalElements() + participationPage.getTotalElements();
            viewResponse.setTotalRecords(totalRecords);
            viewResponse.setPage(pageNum);
            viewResponse.setSize(pageSize);
            int totalPages = Math.max(historyTotalPages, participationPage.getTotalPages());
            viewResponse.setTotalPages(totalPages);

            return Response.success("Score history retrieved successfully", viewResponse);
        } catch (Exception e) {
            logger.error("Failed to get score history: {}", e.getMessage(), e);
            return Response.error("Failed to get score history: " + e.getMessage());
        }
    }
}

