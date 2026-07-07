package vn.campuslife.service.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import vn.campuslife.entity.*;
import vn.campuslife.enumeration.*;
import vn.campuslife.model.Response;
import vn.campuslife.model.score.ScoreBreakdownResponse;
import vn.campuslife.model.statistics.*;
import vn.campuslife.repository.*;
import vn.campuslife.security.department.DepartmentAuthorizationService;
import vn.campuslife.security.department.DepartmentScope;
import vn.campuslife.security.department.DepartmentScopeSpec;
import vn.campuslife.service.StatisticsService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private static final Logger logger = LoggerFactory.getLogger(StatisticsServiceImpl.class);

    private final ActivityRepository activityRepository;
    private final ActivityRegistrationRepository activityRegistrationRepository;
    private final ActivityParticipationRepository activityParticipationRepository;
    private final StudentRepository studentRepository;
    private final StudentScoreRepository studentScoreRepository;
    private final ActivitySeriesRepository activitySeriesRepository;
    private final StudentSeriesProgressRepository studentSeriesProgressRepository;
    private final MiniGameRepository miniGameRepository;
    private final MiniGameAttemptRepository miniGameAttemptRepository;
    private final SemesterRepository semesterRepository;
    private final DepartmentRepository departmentRepository;
    private final ScoreEntryRepository scoreEntryRepository;
    private final DepartmentAuthorizationService departmentAuthorizationService;

    @Override
    public Response getDashboardOverview(Long studentId, DepartmentScope scope) {
        if (scope == null || !scope.manager()) {
            return getDashboardOverview(studentId);
        }
        try {
            DashboardOverviewResponse response = new DashboardOverviewResponse();
            List<Activity> activities = activityRepository.findAll(DepartmentScopeSpec.activity(scope.departmentIds()));
            List<Student> students = studentRepository.findAll(DepartmentScopeSpec.student(scope.departmentIds()));
            List<ActivitySeries> series = activitySeriesRepository.findAll(DepartmentScopeSpec.activitySeries(scope.departmentIds()));
            List<ActivityRegistration> registrations =
                    activityRegistrationRepository.findAll(DepartmentScopeSpec.activityRegistration(scope.departmentIds()));
            List<ActivityParticipation> participations =
                    activityParticipationRepository.findAll(DepartmentScopeSpec.activityParticipation(scope.departmentIds()));

            response.setTotalActivities((long) activities.size());
            response.setTotalStudents((long) students.size());
            response.setTotalSeries((long) series.size());
            response.setTotalMiniGames(0L);
            response.setMonthlyRegistrations((long) registrations.size());
            response.setMonthlyParticipations((long) participations.size());
            response.setAverageParticipationRate(registrations.isEmpty()
                    ? 0.0
                    : (double) participations.size() / registrations.size());
            response.setTopActivities(List.of());
            response.setTopStudents(List.of());
            return Response.success("Dashboard overview retrieved successfully", response);
        } catch (Exception e) {
            logger.error("Error getting scoped dashboard overview: {}", e.getMessage(), e);
            return Response.error("Failed to get dashboard overview: " + e.getMessage());
        }
    }

    @Override
    public Response getActivityStatistics(String activityType, String scoreType, Long departmentId,
            LocalDateTime startDate, LocalDateTime endDate, DepartmentScope scope) {
        if (scope == null || !scope.manager()) {
            return getActivityStatistics(activityType, scoreType, departmentId, startDate, endDate);
        }
        try {
            Set<Long> deptFilter = departmentAuthorizationService.managerDepartmentFilter(scope, departmentId);
            List<Activity> activities = activityRepository.findAll(DepartmentScopeSpec.activity(deptFilter));
            ActivityStatisticsResponse response = new ActivityStatisticsResponse();
            response.setTotalActivities((long) activities.size());

            Map<ActivityType, Long> countByType = new HashMap<>();
            for (ActivityType type : ActivityType.values()) {
                countByType.put(type, activities.stream().filter(activity -> activity.getType() == type).count());
            }
            response.setCountByType(countByType);
            Map<String, Long> countByStatus = new HashMap<>();
            countByStatus.put("draft", activities.stream().filter(Activity::isDraft).count());
            countByStatus.put("published", activities.stream().filter(activity -> !activity.isDraft()).count());
            countByStatus.put("deleted", 0L);
            response.setCountByStatus(countByStatus);

            Map<Long, Long> countByDepartment = new HashMap<>();
            for (Long deptId : deptFilter) {
                countByDepartment.put(deptId, activities.stream()
                        .filter(activity -> activity.getOrganizers().stream().anyMatch(dept -> deptId.equals(dept.getId())))
                        .count());
            }
            response.setCountByDepartment(countByDepartment);
            response.setTopActivitiesByRegistrations(List.of());
            response.setParticipationRates(List.of());
            response.setActivitiesInSeries(activities.stream().filter(activity -> activity.getSeriesId() != null).count());
            response.setStandaloneActivities(activities.stream().filter(activity -> activity.getSeriesId() == null).count());
            return Response.success("Activity statistics retrieved successfully", response);
        } catch (Exception e) {
            logger.error("Error getting scoped activity statistics: {}", e.getMessage(), e);
            return Response.error("Failed to get activity statistics: " + e.getMessage());
        }
    }

    @Override
    public Response getStudentStatistics(Long departmentId, Long classId, Long semesterId, DepartmentScope scope) {
        if (scope == null || !scope.manager()) {
            return getStudentStatistics(departmentId, classId, semesterId);
        }
        try {
            Set<Long> deptFilter = departmentAuthorizationService.managerDepartmentFilter(scope, departmentId);
            if (classId != null) {
                departmentAuthorizationService.requireStudentClassAccess(classId, scope);
            }
            List<Student> students = studentRepository.findAll(DepartmentScopeSpec.student(deptFilter));
            if (classId != null) {
                students = students.stream()
                        .filter(student -> student.getStudentClass() != null
                                && classId.equals(student.getStudentClass().getId()))
                        .collect(Collectors.toList());
            }
            StudentStatisticsResponse response = new StudentStatisticsResponse();
            response.setTotalStudents((long) students.size());
            Map<Long, Long> countByDepartment = new HashMap<>();
            for (Long deptId : deptFilter) {
                countByDepartment.put(deptId, students.stream()
                        .filter(student -> student.getDepartment() != null && deptId.equals(student.getDepartment().getId()))
                        .count());
            }
            response.setCountByDepartment(countByDepartment);
            response.setCountByClass(new HashMap<>());
            response.setTopParticipants(List.of());
            response.setInactiveStudents(List.of());
            response.setLowParticipationRateStudents(List.of());
            return Response.success("Student statistics retrieved successfully", response);
        } catch (Exception e) {
            logger.error("Error getting scoped student statistics: {}", e.getMessage(), e);
            return Response.error("Failed to get student statistics: " + e.getMessage());
        }
    }

    @Override
    public Response getScoreStatistics(String scoreType, Long semesterId, Long departmentId,
            Long classId, Long studentId, DepartmentScope scope) {
        if (scope == null || !scope.manager()) {
            return getScoreStatistics(scoreType, semesterId, departmentId, classId, studentId);
        }
        try {
            if (studentId != null) {
                departmentAuthorizationService.requireStudentAccess(studentId, scope);
            }
            Set<Long> deptFilter = departmentAuthorizationService.managerDepartmentFilter(scope, departmentId);
            if (classId != null) {
                departmentAuthorizationService.requireStudentClassAccess(classId, scope);
            }

            List<StudentScore> scores = studentScoreRepository.findAll(DepartmentScopeSpec.studentScore(deptFilter));
            if (semesterId != null) {
                scores = scores.stream()
                        .filter(score -> score.getSemester() != null && semesterId.equals(score.getSemester().getId()))
                        .collect(Collectors.toList());
            }
            if (scoreType != null) {
                scores = scores.stream()
                        .filter(score -> score.getScoreType().name().equals(scoreType))
                        .collect(Collectors.toList());
            }
            if (classId != null) {
                scores = scores.stream()
                        .filter(score -> score.getStudent().getStudentClass() != null
                                && classId.equals(score.getStudent().getStudentClass().getId()))
                        .collect(Collectors.toList());
            }
            if (studentId != null) {
                scores = scores.stream()
                        .filter(score -> studentId.equals(score.getStudent().getId()))
                        .collect(Collectors.toList());
            }

            ScoreStatisticsResponse response = new ScoreStatisticsResponse();
            Map<ScoreType, ScoreStatisticsResponse.ScoreTypeStatistics> statisticsByType = new HashMap<>();
            for (ScoreType type : ScoreType.values()) {
                List<StudentScore> typeScores = scores.stream()
                        .filter(score -> score.getScoreType() == type)
                        .collect(Collectors.toList());
                if (typeScores.isEmpty()) {
                    continue;
                }
                List<BigDecimal> values = typeScores.stream()
                        .map(StudentScore::getScore)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal avg = values.isEmpty()
                        ? BigDecimal.ZERO
                        : sum.divide(BigDecimal.valueOf(values.size()), 2, java.math.RoundingMode.HALF_UP);
                ScoreStatisticsResponse.ScoreTypeStatistics stats = new ScoreStatisticsResponse.ScoreTypeStatistics();
                stats.setScoreType(type);
                stats.setAverageScore(avg);
                stats.setMaxScore(values.stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO));
                stats.setMinScore(values.stream().min(BigDecimal::compareTo).orElse(BigDecimal.ZERO));
                stats.setTotalStudents((long) typeScores.size());
                statisticsByType.put(type, stats);
            }
            response.setStatisticsByType(statisticsByType);
            response.setTopStudents(scores.stream()
                    .sorted((a, b) -> nullSafeScore(b).compareTo(nullSafeScore(a)))
                    .limit(10)
                    .map(this::toTopStudentScoreItem)
                    .collect(Collectors.toList()));
            response.setAverageByDepartment(new HashMap<>());
            response.setAverageByClass(new HashMap<>());
            response.setAverageBySemester(new HashMap<>());
            response.setScoreDistribution(new HashMap<>());
            return Response.success("Score statistics retrieved successfully", response);
        } catch (Exception e) {
            logger.error("Error getting scoped score statistics: {}", e.getMessage(), e);
            return Response.error("Failed to get score statistics: " + e.getMessage());
        }
    }

    @Override
    public Response getScoreBreakdown(Long semesterId, Long studentId, Long departmentId, DepartmentScope scope) {
        if (scope == null || !scope.manager()) {
            return getScoreBreakdown(semesterId, studentId, departmentId);
        }
        try {
            if (studentId != null) {
                departmentAuthorizationService.requireStudentAccess(studentId, scope);
            }
            Set<Long> deptFilter = departmentAuthorizationService.managerDepartmentFilter(scope, departmentId);
            List<ScoreEntry> entries = scoreEntryRepository.findAll(DepartmentScopeSpec.scoreEntry(deptFilter));
            if (semesterId != null) {
                entries = entries.stream()
                        .filter(entry -> entry.getSemester() != null && semesterId.equals(entry.getSemester().getId()))
                        .collect(Collectors.toList());
            }
            if (studentId != null) {
                entries = entries.stream()
                        .filter(entry -> entry.getStudent() != null && studentId.equals(entry.getStudent().getId()))
                        .collect(Collectors.toList());
            }

            ScoreBreakdownResponse response = new ScoreBreakdownResponse();
            response.setSemesterId(semesterId);
            response.setStudentId(studentId);
            response.setBreakdowns(entries.stream()
                    .collect(Collectors.groupingBy(ScoreEntry::getSourceType))
                    .entrySet()
                    .stream()
                    .map(entry -> {
                        ScoreBreakdownResponse.SourceBreakdown breakdown = new ScoreBreakdownResponse.SourceBreakdown();
                        breakdown.setSourceType(entry.getKey());
                        breakdown.setEntryCount((long) entry.getValue().size());
                        breakdown.setTotalPoints(entry.getValue().stream()
                                .map(ScoreEntry::getPoints)
                                .filter(Objects::nonNull)
                                .reduce(BigDecimal.ZERO, BigDecimal::add));
                        return breakdown;
                    })
                    .collect(Collectors.toList()));
            return Response.success("Score breakdown retrieved successfully", response);
        } catch (Exception e) {
            logger.error("Error getting scoped score breakdown: {}", e.getMessage(), e);
            return Response.error("Failed to get score breakdown: " + e.getMessage());
        }
    }

    private BigDecimal nullSafeScore(StudentScore score) {
        return score.getScore() != null ? score.getScore() : BigDecimal.ZERO;
    }

    private ScoreStatisticsResponse.TopStudentScoreItem toTopStudentScoreItem(StudentScore score) {
        ScoreStatisticsResponse.TopStudentScoreItem item = new ScoreStatisticsResponse.TopStudentScoreItem();
        item.setStudentId(score.getStudent().getId());
        item.setStudentName(score.getStudent().getFullName());
        item.setStudentCode(score.getStudent().getStudentCode());
        item.setScoreType(score.getScoreType());
        item.setScore(nullSafeScore(score));
        if (score.getSemester() != null) {
            item.setSemesterId(score.getSemester().getId());
            item.setSemesterName(score.getSemester().getName());
        }
        return item;
    }

    @Override
    public Response getDashboardOverview(Long studentId) {
        try {
            DashboardOverviewResponse response = new DashboardOverviewResponse();

            // Total counts
            response.setTotalActivities(activityRepository.count());
            response.setTotalStudents(studentRepository.countAllActive());
            response.setTotalSeries(activitySeriesRepository.countAllActive());
            response.setTotalMiniGames(miniGameRepository.count());

            // Monthly statistics (current month)
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime startOfMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
            LocalDateTime endOfMonth = now.withDayOfMonth(now.toLocalDate().lengthOfMonth())
                    .withHour(23).withMinute(59).withSecond(59);

            Long monthlyRegistrations = activityRegistrationRepository.countByDateRange(startOfMonth, endOfMonth);
            Long monthlyParticipations = activityParticipationRepository.countByDateRange(startOfMonth, endOfMonth);
            response.setMonthlyRegistrations(monthlyRegistrations);
            response.setMonthlyParticipations(monthlyParticipations);

            // Average participation rate
            if (monthlyRegistrations > 0) {
                response.setAverageParticipationRate((double) monthlyParticipations / monthlyRegistrations);
            } else {
                response.setAverageParticipationRate(0.0);
            }

            // Top 5 activities by registrations
            Pageable top5Page = PageRequest.of(0, 5);
            List<Object[]> topActivitiesData = activityRegistrationRepository
                    .findTopActivitiesByRegistrations(top5Page);
            List<DashboardOverviewResponse.TopActivityItem> topActivities = new ArrayList<>();
            for (Object[] data : topActivitiesData) {
                Long activityId = (Long) data[0];
                Long regCount = (Long) data[1];
                Activity activity = activityRepository.findById(activityId).orElse(null);
                if (activity != null) {
                    Long partCount = activityParticipationRepository.countByActivityId(activityId);
                    DashboardOverviewResponse.TopActivityItem item = new DashboardOverviewResponse.TopActivityItem();
                    item.setActivityId(activityId);
                    item.setActivityName(activity.getName());
                    item.setRegistrationCount(regCount);
                    item.setParticipationCount(partCount);
                    topActivities.add(item);
                }
            }
            response.setTopActivities(topActivities);

            // Top 5 students by participations
            List<Object[]> topStudentsData = activityParticipationRepository.findTopStudentsByParticipations(top5Page);
            List<DashboardOverviewResponse.TopStudentItem> topStudents = new ArrayList<>();
            for (Object[] data : topStudentsData) {
                Long topStudentId = (Long) data[0];
                Long partCount = (Long) data[1];
                Student student = studentRepository.findById(topStudentId).orElse(null);
                if (student != null) {
                    DashboardOverviewResponse.TopStudentItem item = new DashboardOverviewResponse.TopStudentItem();
                    item.setStudentId(topStudentId);
                    item.setStudentName(student.getFullName());
                    item.setStudentCode(student.getStudentCode());
                    item.setParticipationCount(partCount);
                    topStudents.add(item);
                }
            }
            response.setTopStudents(topStudents);

            return Response.success("Dashboard overview retrieved successfully", response);
        } catch (Exception e) {
            logger.error("Error getting dashboard overview: {}", e.getMessage(), e);
            return Response.error("Failed to get dashboard overview: " + e.getMessage());
        }
    }

    @Override
    public Response getActivityStatistics(String activityType, String scoreType, Long departmentId,
            LocalDateTime startDate, LocalDateTime endDate) {
        try {
            ActivityStatisticsResponse response = new ActivityStatisticsResponse();

            // Total activities
            response.setTotalActivities(activityRepository.count());

            // Count by type
            Map<ActivityType, Long> countByType = new HashMap<>();
            for (ActivityType type : ActivityType.values()) {
                countByType.put(type, activityRepository.countByType(type));
            }
            response.setCountByType(countByType);

            // Count by status
            Map<String, Long> countByStatus = new HashMap<>();
            countByStatus.put("draft", activityRepository.countByIsDraft(true));
            countByStatus.put("published", activityRepository.countByIsDraft(false));
            countByStatus.put("deleted", activityRepository.count() - activityRepository.count());
            response.setCountByStatus(countByStatus);

            // Top activities by registrations
            Pageable top10Page = PageRequest.of(0, 10);
            List<Object[]> topActivitiesData = activityRegistrationRepository
                    .findTopActivitiesByRegistrations(top10Page);
            List<ActivityStatisticsResponse.TopActivityItem> topActivities = new ArrayList<>();
            for (Object[] data : topActivitiesData) {
                Long activityId = (Long) data[0];
                Long regCount = (Long) data[1];
                Activity activity = activityRepository.findById(activityId).orElse(null);
                if (activity != null) {
                    Long partCount = activityParticipationRepository.countByActivityId(activityId);
                    ActivityStatisticsResponse.TopActivityItem item = new ActivityStatisticsResponse.TopActivityItem();
                    item.setActivityId(activityId);
                    item.setActivityName(activity.getName());
                    item.setRegistrationCount(regCount);
                    item.setParticipationCount(partCount);
                    topActivities.add(item);
                }
            }
            response.setTopActivitiesByRegistrations(topActivities);

            // Participation rates
            List<ActivityStatisticsResponse.ActivityParticipationRate> participationRates = new ArrayList<>();
            for (ActivityStatisticsResponse.TopActivityItem item : topActivities) {
                Double rate = item.getRegistrationCount() > 0
                        ? (double) item.getParticipationCount() / item.getRegistrationCount()
                        : 0.0;
                ActivityStatisticsResponse.ActivityParticipationRate rateItem = new ActivityStatisticsResponse.ActivityParticipationRate();
                rateItem.setActivityId(item.getActivityId());
                rateItem.setActivityName(item.getActivityName());
                rateItem.setRegistrationCount(item.getRegistrationCount());
                rateItem.setParticipationCount(item.getParticipationCount());
                rateItem.setParticipationRate(rate);
                participationRates.add(rateItem);
            }
            response.setParticipationRates(participationRates);

            // Count by department
            Map<Long, Long> countByDepartment = new HashMap<>();
            List<Department> departments = departmentRepository.findAll();
            for (Department dept : departments) {
                Long count = activityRepository.countByDepartmentId(dept.getId());
                if (count > 0) {
                    countByDepartment.put(dept.getId(), count);
                }
            }
            response.setCountByDepartment(countByDepartment);

            // Activities in series vs standalone
            response.setActivitiesInSeries(activityRepository.countActivitiesInSeries());
            response.setStandaloneActivities(activityRepository.countStandaloneActivities());

            return Response.success("Activity statistics retrieved successfully", response);
        } catch (Exception e) {
            logger.error("Error getting activity statistics: {}", e.getMessage(), e);
            return Response.error("Failed to get activity statistics: " + e.getMessage());
        }
    }

    @Override
    public Response getStudentStatistics(Long departmentId, Long classId, Long semesterId) {
        try {
            StudentStatisticsResponse response = new StudentStatisticsResponse();

            // Total students
            response.setTotalStudents(studentRepository.countAllActive());

            // Count by department
            Map<Long, Long> countByDepartment = new HashMap<>();
            List<Department> departments = departmentRepository.findAll();
            for (Department dept : departments) {
                Long count = studentRepository.countByDepartmentId(dept.getId());
                if (count > 0) {
                    countByDepartment.put(dept.getId(), count);
                }
            }
            response.setCountByDepartment(countByDepartment);

            // Count by class (if needed, can be expanded)
            Map<Long, Long> countByClass = new HashMap<>();
            // This would require a StudentClass repository query
            response.setCountByClass(countByClass);

            // Top participants
            Pageable top10Page = PageRequest.of(0, 10);
            List<Object[]> topParticipantsData = activityParticipationRepository
                    .findTopStudentsByParticipations(top10Page);
            List<StudentStatisticsResponse.TopParticipantItem> topParticipants = new ArrayList<>();
            for (Object[] data : topParticipantsData) {
                Long studentId = (Long) data[0];
                Long partCount = (Long) data[1];
                Student student = studentRepository.findById(studentId).orElse(null);
                if (student != null) {
                    StudentStatisticsResponse.TopParticipantItem item = new StudentStatisticsResponse.TopParticipantItem();
                    item.setStudentId(studentId);
                    item.setStudentName(student.getFullName());
                    item.setStudentCode(student.getStudentCode());
                    item.setParticipationCount(partCount);
                    topParticipants.add(item);
                }
            }
            response.setTopParticipants(topParticipants);

            // Inactive students
            List<Student> inactiveStudents = studentRepository.findInactiveStudents();
            List<StudentStatisticsResponse.InactiveStudentItem> inactiveItems = new ArrayList<>();
            for (Student student : inactiveStudents) {
                String deptName = student.getDepartment() != null ? student.getDepartment().getName() : "N/A";
                StudentStatisticsResponse.InactiveStudentItem item = new StudentStatisticsResponse.InactiveStudentItem();
                item.setStudentId(student.getId());
                item.setStudentName(student.getFullName());
                item.setStudentCode(student.getStudentCode());
                item.setDepartmentName(deptName);
                inactiveItems.add(item);
            }
            response.setInactiveStudents(inactiveItems);

            // Low participation rate students
            List<StudentStatisticsResponse.LowParticipationRateItem> lowRateItems = new ArrayList<>();
            for (StudentStatisticsResponse.TopParticipantItem participant : topParticipants) {
                Long regCount = activityRegistrationRepository.countByStudentId(participant.getStudentId());
                Long partCount = participant.getParticipationCount();
                if (regCount > partCount && regCount > 0) {
                    Double rate = (double) partCount / regCount;
                    if (rate < 0.5) { // Less than 50% participation rate
                        StudentStatisticsResponse.LowParticipationRateItem item = new StudentStatisticsResponse.LowParticipationRateItem();
                        item.setStudentId(participant.getStudentId());
                        item.setStudentName(participant.getStudentName());
                        item.setStudentCode(participant.getStudentCode());
                        item.setRegistrationCount(regCount);
                        item.setParticipationCount(partCount);
                        item.setParticipationRate(rate);
                        lowRateItems.add(item);
                    }
                }
            }
            response.setLowParticipationRateStudents(lowRateItems);

            return Response.success("Student statistics retrieved successfully", response);
        } catch (Exception e) {
            logger.error("Error getting student statistics: {}", e.getMessage(), e);
            return Response.error("Failed to get student statistics: " + e.getMessage());
        }
    }

    @Override
    public Response getScoreStatistics(String scoreType, Long semesterId, Long departmentId,
            Long classId, Long studentId) {
        try {
            ScoreStatisticsResponse response = new ScoreStatisticsResponse();

            // Get current semester if not provided
            Semester semester = null;
            if (semesterId != null) {
                semester = semesterRepository.findById(semesterId).orElse(null);
            } else {
                semester = semesterRepository.findAll().stream()
                        .filter(Semester::isOpen)
                        .findFirst()
                        .orElse(semesterRepository.findAll().stream().findFirst().orElse(null));
            }

            if (semester == null) {
                return Response.error("No semester found");
            }

            // Statistics by score type
            Map<ScoreType, ScoreStatisticsResponse.ScoreTypeStatistics> statisticsByType = new HashMap<>();
            for (ScoreType type : ScoreType.values()) {
                if (scoreType != null && !type.name().equals(scoreType)) {
                    continue;
                }

                BigDecimal avg = studentScoreRepository.calculateAverageByScoreTypeAndSemester(type, semester.getId());
                Object[] maxMin = studentScoreRepository.findMaxMinByScoreTypeAndSemester(type, semester.getId());
                Long totalStudents = (long) studentScoreRepository
                        .findBySemesterIdAndScoreTypeOrderByScoreDesc(semester.getId(), type).size();

                ScoreStatisticsResponse.ScoreTypeStatistics stats = new ScoreStatisticsResponse.ScoreTypeStatistics();
                stats.setScoreType(type);
                stats.setAverageScore(avg != null ? avg : BigDecimal.ZERO);
                
                // Safely extract max and min scores
                BigDecimal maxScore = BigDecimal.ZERO;
                BigDecimal minScore = BigDecimal.ZERO;
                if (maxMin != null && maxMin.length >= 2) {
                    if (maxMin[0] != null) {
                        if (maxMin[0] instanceof BigDecimal) {
                            maxScore = (BigDecimal) maxMin[0];
                        } else if (maxMin[0] instanceof Number) {
                            maxScore = BigDecimal.valueOf(((Number) maxMin[0]).doubleValue());
                        }
                    }
                    if (maxMin[1] != null) {
                        if (maxMin[1] instanceof BigDecimal) {
                            minScore = (BigDecimal) maxMin[1];
                        } else if (maxMin[1] instanceof Number) {
                            minScore = BigDecimal.valueOf(((Number) maxMin[1]).doubleValue());
                        }
                    }
                }
                
                stats.setMaxScore(maxScore);
                stats.setMinScore(minScore);
                stats.setTotalStudents(totalStudents);

                statisticsByType.put(type, stats);
            }
            response.setStatisticsByType(statisticsByType);

            // Top students
            List<ScoreStatisticsResponse.TopStudentScoreItem> topStudents = new ArrayList<>();
            for (ScoreType type : ScoreType.values()) {
                if (scoreType != null && !type.name().equals(scoreType)) {
                    continue;
                }
                List<StudentScore> scores = studentScoreRepository
                        .findBySemesterIdAndScoreTypeOrderByScoreDesc(semester.getId(), type);
                for (int i = 0; i < Math.min(10, scores.size()); i++) {
                    StudentScore score = scores.get(i);
                    if (studentId != null && !score.getStudent().getId().equals(studentId)) {
                        continue;
                    }
                    ScoreStatisticsResponse.TopStudentScoreItem item = new ScoreStatisticsResponse.TopStudentScoreItem();
                    item.setStudentId(score.getStudent().getId());
                    item.setStudentName(score.getStudent().getFullName());
                    item.setStudentCode(score.getStudent().getStudentCode());
                    item.setScoreType(type);
                    item.setScore(score.getScore());
                    item.setSemesterId(semester.getId());
                    item.setSemesterName(semester.getName());
                    topStudents.add(item);
                }
            }
            response.setTopStudents(topStudents);

            // Average by department
            Map<Long, BigDecimal> averageByDepartment = new HashMap<>();
            for (ScoreType type : ScoreType.values()) {
                if (scoreType != null && !type.name().equals(scoreType)) {
                    continue;
                }
                for (Department dept : departmentRepository.findAll()) {
                    BigDecimal avg = studentScoreRepository.calculateAverageByDepartmentAndScoreType(dept.getId(),
                            type);
                    if (avg != null && avg.compareTo(BigDecimal.ZERO) > 0) {
                        averageByDepartment.put(dept.getId(), avg);
                    }
                }
            }
            response.setAverageByDepartment(averageByDepartment);

            // Score distribution (histogram)
            Map<String, Long> scoreDistribution = new HashMap<>();
            for (ScoreType type : ScoreType.values()) {
                if (scoreType != null && !type.name().equals(scoreType)) {
                    continue;
                }
                List<StudentScore> scores = studentScoreRepository
                        .findBySemesterIdAndScoreTypeOrderByScoreDesc(semester.getId(), type);
                for (StudentScore score : scores) {
                    BigDecimal s = score.getScore() != null ? score.getScore() : BigDecimal.ZERO;
                    String range = getScoreRange(s);
                    scoreDistribution.put(range, scoreDistribution.getOrDefault(range, 0L) + 1);
                }
            }
            response.setScoreDistribution(scoreDistribution);

            return Response.success("Score statistics retrieved successfully", response);
        } catch (Exception e) {
            logger.error("Error getting score statistics: {}", e.getMessage(), e);
            return Response.error("Failed to get score statistics: " + e.getMessage());
        }
    }

    private String getScoreRange(BigDecimal score) {
        int s = score.intValue();
        if (s < 10)
            return "0-10";
        if (s < 20)
            return "10-20";
        if (s < 30)
            return "20-30";
        if (s < 40)
            return "30-40";
        if (s < 50)
            return "40-50";
        if (s < 60)
            return "50-60";
        if (s < 70)
            return "60-70";
        if (s < 80)
            return "70-80";
        if (s < 90)
            return "80-90";
        return "90-100";
    }

    @Override
    public Response getSeriesStatistics(Long seriesId, Long semesterId) {
        try {
            SeriesStatisticsResponse response = new SeriesStatisticsResponse();

            // Total series
            response.setTotalSeries(activitySeriesRepository.countAllActive());

            // Series details
            List<ActivitySeries> allSeries = activitySeriesRepository.findAll().stream()
                    .filter(s -> !s.isDeleted())
                    .collect(Collectors.toList());

            List<SeriesStatisticsResponse.SeriesDetailItem> seriesDetails = new ArrayList<>();
            Map<Long, Long> studentsPerSeries = new HashMap<>();
            Map<Long, BigDecimal> milestonePointsAwarded = new HashMap<>();

            for (ActivitySeries series : allSeries) {
                if (seriesId != null && !series.getId().equals(seriesId)) {
                    continue;
                }

                // Get total activities in series (only non-deleted)
                Long totalActivities = (long) activityRepository.findBySeriesIdAndIsDeletedFalse(series.getId()).size();
                
                // Get registered students (students who registered for at least one activity in series)
                Long registeredStudents = activitySeriesRepository.countStudentsBySeriesId(series.getId());
                
                // Get completed students (students who completed all activities in series)
                // Note: This counts students where completedCount >= totalActivities
                Long completedStudents = studentSeriesProgressRepository
                        .countCompletedStudentsBySeriesId(series.getId());
                
                // Debug logging
                logger.debug("Series {} ({}): totalActivities={}, registeredStudents={}, completedStudents={}", 
                        series.getId(), series.getName(), totalActivities, registeredStudents, completedStudents);
                
                // Calculate completion rate: completedStudents / registeredStudents
                // If no students registered, rate is 0
                Double completionRate = registeredStudents > 0
                        ? (double) completedStudents / registeredStudents
                        : 0.0;
                
                // Additional validation: completion rate should be between 0 and 1
                if (completionRate > 1.0) {
                    logger.warn("Series {}: completionRate > 1.0 ({}), this may indicate a data inconsistency", 
                            series.getId(), completionRate);
                    completionRate = 1.0; // Cap at 100%
                }

                SeriesStatisticsResponse.SeriesDetailItem item = new SeriesStatisticsResponse.SeriesDetailItem();
                item.setSeriesId(series.getId());
                item.setSeriesName(series.getName());
                item.setTotalActivities(totalActivities);
                item.setRegisteredStudents(registeredStudents);
                item.setCompletedStudents(completedStudents);
                item.setCompletionRate(completionRate);
                seriesDetails.add(item);

                studentsPerSeries.put(series.getId(), registeredStudents);

                // Calculate milestone points awarded (simplified - would need to query
                // StudentScore)
                milestonePointsAwarded.put(series.getId(), BigDecimal.ZERO);
            }

            response.setSeriesDetails(seriesDetails);
            response.setStudentsPerSeries(studentsPerSeries);
            response.setMilestonePointsAwarded(milestonePointsAwarded);

            // Popular series
            List<SeriesStatisticsResponse.PopularSeriesItem> popularSeries = new ArrayList<>();
            for (SeriesStatisticsResponse.SeriesDetailItem detail : seriesDetails) {
                SeriesStatisticsResponse.PopularSeriesItem item = new SeriesStatisticsResponse.PopularSeriesItem();
                item.setSeriesId(detail.getSeriesId());
                item.setSeriesName(detail.getSeriesName());
                item.setStudentCount(detail.getRegisteredStudents());
                item.setTotalActivities(detail.getTotalActivities());
                popularSeries.add(item);
            }
            popularSeries.sort((a, b) -> Long.compare(b.getStudentCount(), a.getStudentCount()));
            response.setPopularSeries(popularSeries);

            return Response.success("Series statistics retrieved successfully", response);
        } catch (Exception e) {
            logger.error("Error getting series statistics: {}", e.getMessage(), e);
            return Response.error("Failed to get series statistics: " + e.getMessage());
        }
    }

    @Override
    public Response getMiniGameStatistics(Long miniGameId, LocalDateTime startDate, LocalDateTime endDate) {
        try {
            MiniGameStatisticsResponse response = new MiniGameStatisticsResponse();

            // Total minigames
            response.setTotalMiniGames(miniGameRepository.count());

            // Total attempts
            Long totalAttempts = miniGameAttemptRepository.countAll();
            Long passedAttempts = miniGameAttemptRepository.countByStatus(AttemptStatus.PASSED);
            Long failedAttempts = miniGameAttemptRepository.countByStatus(AttemptStatus.FAILED);

            response.setTotalAttempts(totalAttempts);
            response.setPassedAttempts(passedAttempts);
            response.setFailedAttempts(failedAttempts);
            response.setPassRate(totalAttempts > 0 ? (double) passedAttempts / totalAttempts : 0.0);

            // MiniGame details
            List<MiniGame> allMiniGames = miniGameRepository.findAll();
            Map<Long, MiniGameStatisticsResponse.MiniGameDetailItem> miniGameDetails = new HashMap<>();
            Map<Long, BigDecimal> averageScoreByMiniGame = new HashMap<>();
            Map<Long, Double> averageCorrectAnswersByMiniGame = new HashMap<>();

            for (MiniGame miniGame : allMiniGames) {
                if (miniGameId != null && !miniGame.getId().equals(miniGameId)) {
                    continue;
                }

                Long totalAttemptsForGame = miniGameAttemptRepository.countByMiniGameId(miniGame.getId());
                Long passedForGame = miniGameAttemptRepository.countByMiniGameIdAndStatus(miniGame.getId(),
                        AttemptStatus.PASSED);
                Long failedForGame = miniGameAttemptRepository.countByMiniGameIdAndStatus(miniGame.getId(),
                        AttemptStatus.FAILED);
                Double passRateForGame = totalAttemptsForGame > 0
                        ? (double) passedForGame / totalAttemptsForGame
                        : 0.0;
                BigDecimal avgScore = miniGameAttemptRepository.calculateAverageScoreByMiniGameId(miniGame.getId());
                Double avgCorrect = miniGameAttemptRepository
                        .calculateAverageCorrectAnswersByMiniGameId(miniGame.getId());

                MiniGameStatisticsResponse.MiniGameDetailItem item = new MiniGameStatisticsResponse.MiniGameDetailItem();
                item.setMiniGameId(miniGame.getId());
                item.setTitle(miniGame.getTitle());
                item.setTotalAttempts(totalAttemptsForGame);
                item.setPassedAttempts(passedForGame);
                item.setFailedAttempts(failedForGame);
                item.setPassRate(passRateForGame);
                item.setAverageScore(avgScore != null ? avgScore : BigDecimal.ZERO);
                miniGameDetails.put(miniGame.getId(), item);

                if (avgScore != null) {
                    averageScoreByMiniGame.put(miniGame.getId(), avgScore);
                }
                if (avgCorrect != null) {
                    averageCorrectAnswersByMiniGame.put(miniGame.getId(), avgCorrect);
                }
            }

            response.setMiniGameDetails(miniGameDetails);
            response.setAverageScoreByMiniGame(averageScoreByMiniGame);
            response.setAverageCorrectAnswersByMiniGame(averageCorrectAnswersByMiniGame);

            // Popular minigames
            Pageable top10Page = PageRequest.of(0, 10);
            List<Object[]> popularData = miniGameAttemptRepository.findTopMiniGamesByAttempts(top10Page);
            List<MiniGameStatisticsResponse.PopularMiniGameItem> popularMiniGames = new ArrayList<>();
            for (Object[] data : popularData) {
                Long mgId = (Long) data[0];
                Long attemptCount = (Long) data[1];
                MiniGame mg = miniGameRepository.findById(mgId).orElse(null);
                if (mg != null) {
                    Long uniqueStudents = miniGameAttemptRepository.countUniqueStudentsByMiniGameId(mgId);
                    MiniGameStatisticsResponse.PopularMiniGameItem item = new MiniGameStatisticsResponse.PopularMiniGameItem();
                    item.setMiniGameId(mgId);
                    item.setTitle(mg.getTitle());
                    item.setAttemptCount(attemptCount);
                    item.setUniqueStudentCount(uniqueStudents);
                    popularMiniGames.add(item);
                }
            }
            response.setPopularMiniGames(popularMiniGames);

            return Response.success("MiniGame statistics retrieved successfully", response);
        } catch (Exception e) {
            logger.error("Error getting minigame statistics: {}", e.getMessage(), e);
            return Response.error("Failed to get minigame statistics: " + e.getMessage());
        }
    }

    @Override
    public Response getScoreBreakdown(Long semesterId, Long studentId, Long departmentId) {
        try {
            // Resolve semester
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

            ScoreBreakdownResponse response = new ScoreBreakdownResponse();
            response.setSemesterId(semester.getId());
            response.setSemesterName(semester.getName());
            response.setStudentId(studentId);

            List<Object[]> results;
            if (studentId != null) {
                results = scoreEntryRepository.sumPointsBySourceTypeForStudent(
                        studentId, semester.getId(), ScoreEntryStatus.ACTIVE);
            } else {
                results = scoreEntryRepository.sumPointsBySourceType(
                        semester.getId(), ScoreEntryStatus.ACTIVE);
            }

            List<ScoreBreakdownResponse.SourceBreakdown> breakdowns = new ArrayList<>();
            for (Object[] row : results) {
                ScoreBreakdownResponse.SourceBreakdown item = new ScoreBreakdownResponse.SourceBreakdown();
                item.setSourceType((ScoreEntrySourceType) row[0]);
                item.setTotalPoints(row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO);
                // Count entries per source type (approximate from sum)
                item.setEntryCount(null); // Not available from aggregate query
                breakdowns.add(item);
            }

            // Get entry counts separately if needed
            for (ScoreBreakdownResponse.SourceBreakdown breakdown : breakdowns) {
                List<ScoreEntry> entries;
                if (studentId != null) {
                    entries = scoreEntryRepository.findByStudentIdAndSemesterIdAndStatusOrderByCreatedAtAsc(
                            studentId, semester.getId(), ScoreEntryStatus.ACTIVE);
                } else {
                    entries = scoreEntryRepository.findAll(); // Fallback
                }
                long count = entries.stream()
                        .filter(e -> e.getSourceType() == breakdown.getSourceType())
                        .count();
                breakdown.setEntryCount(count);
            }

            response.setBreakdowns(breakdowns);
            return Response.success("Score breakdown retrieved successfully", response);
        } catch (Exception e) {
            logger.error("Error getting score breakdown: {}", e.getMessage(), e);
            return Response.error("Failed to get score breakdown: " + e.getMessage());
        }
    }

}
