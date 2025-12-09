package vn.campuslife.service.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.campuslife.entity.*;
import vn.campuslife.enumeration.ParticipationType;
import vn.campuslife.enumeration.Role;
import vn.campuslife.enumeration.ScoreType;
import vn.campuslife.model.*;
import vn.campuslife.repository.*;
import vn.campuslife.service.ScoreService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
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
    private final ScoreHistoryRepository scoreHistoryRepository;
    private final UserRepository userRepository;
    private final ActivityRepository activityRepository;
    private final ActivitySeriesRepository seriesRepository;

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
            default:
                return scoreType.name();
        }
    }

    @Override
    @Transactional
    public Response recalculateStudentScore(Long studentId, Long semesterId) {
        try {
            // Validate student
            Optional<Student> studentOpt = studentRepository.findByIdAndIsDeletedFalse(studentId);
            if (studentOpt.isEmpty()) {
                return Response.error("Student not found");
            }
            Student student = studentOpt.get();

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

            Map<String, Object> result = new HashMap<>();
            result.put("studentId", studentId);
            result.put("studentCode", student.getStudentCode());
            result.put("studentName", student.getFullName());
            result.put("semesterId", semester.getId());
            result.put("semesterName", semester.getName());

            List<Map<String, Object>> updatedScores = new ArrayList<>();

            // Get all ActivityParticipation COMPLETED của student
            List<ActivityParticipation> participations = participationRepository
                    .findAll()
                    .stream()
                    .filter(p -> p.getRegistration().getStudent().getId().equals(studentId)
                            && p.getParticipationType().equals(ParticipationType.COMPLETED)
                            && p.getRegistration().getActivity().getScoreType() != null)
                    .collect(Collectors.toList());

            // Group by scoreType và tính tổng điểm từ participations
            Map<ScoreType, BigDecimal> participationScores = participations.stream()
                    .collect(Collectors.groupingBy(
                            p -> p.getRegistration().getActivity().getScoreType(),
                            Collectors.reducing(
                                    BigDecimal.ZERO,
                                    p -> p.getPointsEarned() != null ? p.getPointsEarned() : BigDecimal.ZERO,
                                    BigDecimal::add)));

            // Get milestone points từ StudentSeriesProgress
            List<StudentSeriesProgress> allProgress = progressRepository.findAll()
                    .stream()
                    .filter(p -> p.getStudent().getId().equals(studentId))
                    .collect(Collectors.toList());

            Map<ScoreType, BigDecimal> milestoneScores = new HashMap<>();
            for (StudentSeriesProgress progress : allProgress) {
                ActivitySeries series = progress.getSeries();
                if (series.getScoreType() != null && progress.getPointsEarned() != null) {
                    ScoreType scoreType = series.getScoreType();
                    BigDecimal currentMilestone = milestoneScores.getOrDefault(scoreType, BigDecimal.ZERO);
                    milestoneScores.put(scoreType, currentMilestone.add(progress.getPointsEarned()));
                }
            }

            // Combine participation scores và milestone scores
            Set<ScoreType> allScoreTypes = new HashSet<>();
            allScoreTypes.addAll(participationScores.keySet());
            allScoreTypes.addAll(milestoneScores.keySet());

            // Get system user for history
            User systemUser = userRepository.findAll().stream()
                    .filter(user -> user.getRole() == Role.ADMIN || user.getRole() == Role.MANAGER)
                    .findFirst()
                    .orElse(userRepository.findById(1L).orElse(null));

            for (ScoreType scoreType : allScoreTypes) {
                BigDecimal participationTotal = participationScores.getOrDefault(scoreType, BigDecimal.ZERO);
                BigDecimal milestoneTotal = milestoneScores.getOrDefault(scoreType, BigDecimal.ZERO);
                BigDecimal totalScore = participationTotal.add(milestoneTotal);

                // Get or create StudentScore
                Optional<StudentScore> scoreOpt = studentScoreRepository
                        .findByStudentIdAndSemesterIdAndScoreType(studentId, semester.getId(), scoreType);

                StudentScore score;
                BigDecimal oldScore = BigDecimal.ZERO;
                if (scoreOpt.isPresent()) {
                    score = scoreOpt.get();
                    oldScore = score.getScore() != null ? score.getScore() : BigDecimal.ZERO;
                } else {
                    // Create new StudentScore if not exists
                    score = new StudentScore();
                    score.setStudent(student);
                    score.setSemester(semester);
                    score.setScoreType(scoreType);
                    score.setScore(BigDecimal.ZERO);
                }

                // Update score
                score.setScore(totalScore);
                studentScoreRepository.save(score);

                // Create history if score changed
                if (oldScore.compareTo(totalScore) != 0) {
                    ScoreHistory history = new ScoreHistory();
                    history.setScore(score);
                    history.setOldScore(oldScore);
                    history.setNewScore(totalScore);
                    history.setChangedBy(systemUser);
                    history.setChangeDate(LocalDateTime.now());
                    history.setReason("Recalculated score: Participation (" + participationTotal + ") + Milestone (" + milestoneTotal + ")");
                    scoreHistoryRepository.save(history);
                }

                Map<String, Object> scoreInfo = new HashMap<>();
                scoreInfo.put("scoreType", scoreType.name());
                scoreInfo.put("participationScore", participationTotal);
                scoreInfo.put("milestoneScore", milestoneTotal);
                scoreInfo.put("totalScore", totalScore);
                scoreInfo.put("oldScore", oldScore);
                scoreInfo.put("updated", oldScore.compareTo(totalScore) != 0);
                updatedScores.add(scoreInfo);

                logger.info("Recalculated {} score for student {}: {} -> {} (participation: {}, milestone: {})",
                        scoreType, studentId, oldScore, totalScore, participationTotal, milestoneTotal);
            }

            result.put("updatedScores", updatedScores);
            result.put("totalScoreTypes", updatedScores.size());

            return Response.success("Recalculated student score successfully", result);
        } catch (Exception e) {
            logger.error("Failed to recalculate student score: {}", e.getMessage(), e);
            return Response.error("Failed to recalculate student score: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Response recalculateAllStudentScores(Long semesterId) {
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
            List<Student> students = studentRepository.findAll()
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
                    Response recalcResponse = recalculateStudentScore(student.getId(), semester.getId());
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
    public Response getScoreHistory(Long studentId, Long semesterId, ScoreType scoreType, Integer page, Integer size, Long requestingStudentId) {
        try {
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

            // Get ScoreHistory
            Page<ScoreHistory> scoreHistoryPage;
            if (scoreType != null) {
                scoreHistoryPage = scoreHistoryRepository
                        .findByScore_StudentIdAndScore_SemesterIdAndScore_ScoreType(studentId, semesterId, scoreType, pageable);
            } else {
                scoreHistoryPage = scoreHistoryRepository
                        .findByScore_StudentIdAndScore_SemesterId(studentId, semesterId, pageable);
            }

            // Convert ScoreHistory to DTO
            List<ScoreHistoryDetailResponse> scoreHistoryResponses = scoreHistoryPage.getContent().stream()
                    .map(sh -> {
                        ScoreHistoryDetailResponse response = new ScoreHistoryDetailResponse();
                        response.setId(sh.getId());
                        response.setOldScore(sh.getOldScore());
                        response.setNewScore(sh.getNewScore());
                        response.setChangeDate(sh.getChangeDate());
                        response.setReason(sh.getReason());
                        response.setActivityId(sh.getActivityId());
                        response.setChangedByUsername(sh.getChangedBy().getUsername());
                        // User doesn't have fullName, only Student does
                        response.setChangedByFullName(null);

                        // Parse sourceType from reason
                        String reason = sh.getReason() != null ? sh.getReason().toLowerCase() : "";
                        if (reason.contains("minigame quiz")) {
                            response.setSourceType("MINIGAME");
                        } else if (reason.contains("milestone points from series")) {
                            response.setSourceType("MILESTONE");
                            // Parse seriesId from reason: "Milestone points from series: {seriesId}"
                            try {
                                String[] parts = sh.getReason().split(":");
                                if (parts.length > 1) {
                                    Long parsedSeriesId = Long.parseLong(parts[parts.length - 1].trim());
                                    response.setSeriesId(parsedSeriesId);
                                }
                            } catch (Exception e) {
                                logger.warn("Failed to parse seriesId from reason: {}", sh.getReason());
                            }
                        } else if (reason.contains("recalculated score")) {
                            response.setSourceType("RECALCULATED");
                        } else {
                            response.setSourceType("ACTIVITY");
                        }

                        // Get activity name if activityId exists
                        if (sh.getActivityId() != null) {
                            Optional<Activity> activityOpt = activityRepository.findById(sh.getActivityId());
                            if (activityOpt.isPresent()) {
                                response.setActivityName(activityOpt.get().getName());
                            }
                        }

                        // Get series name if seriesId exists
                        if (response.getSeriesId() != null) {
                            Optional<ActivitySeries> seriesOpt = seriesRepository.findById(response.getSeriesId());
                            if (seriesOpt.isPresent()) {
                                response.setSeriesName(seriesOpt.get().getName());
                            }
                        }

                        return response;
                    })
                    .collect(Collectors.toList());

            // Get ActivityParticipation COMPLETED
            Page<ActivityParticipation> participationPage;
            if (scoreType != null) {
                participationPage = participationRepository
                        .findByRegistration_StudentIdAndRegistration_Activity_ScoreType(studentId, scoreType, pageable);
            } else {
                participationPage = participationRepository
                        .findByRegistration_StudentId_Completed(studentId, pageable);
            }

            // Convert ActivityParticipation to DTO
            List<ActivityParticipationDetailResponse> participationResponses = participationPage.getContent().stream()
                    .map(ap -> {
                        ActivityParticipationDetailResponse response = new ActivityParticipationDetailResponse();
                        response.setId(ap.getId());
                        Activity activity = ap.getRegistration().getActivity();
                        response.setActivityId(activity.getId());
                        response.setActivityName(activity.getName());
                        response.setActivityType(activity.getType());
                        response.setPointsEarned(ap.getPointsEarned());
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
                            Optional<ActivitySeries> seriesOpt = seriesRepository.findById(activity.getSeriesId());
                            if (seriesOpt.isPresent()) {
                                response.setSeriesName(seriesOpt.get().getName());
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
            long totalRecords = scoreHistoryPage.getTotalElements() + participationPage.getTotalElements();
            viewResponse.setTotalRecords(totalRecords);
            viewResponse.setPage(pageNum);
            viewResponse.setSize(pageSize);
            // Calculate total pages (use the larger of the two)
            int totalPages = Math.max(scoreHistoryPage.getTotalPages(), participationPage.getTotalPages());
            viewResponse.setTotalPages(totalPages);

            return Response.success("Score history retrieved successfully", viewResponse);
        } catch (Exception e) {
            logger.error("Failed to get score history: {}", e.getMessage(), e);
            return Response.error("Failed to get score history: " + e.getMessage());
        }
    }
}
