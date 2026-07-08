package vn.campuslife.service.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
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
            return Response.success("Dashboard overview retrieved successfully",
                    buildDashboardOverviewResponse(scope.departmentIds()));
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
            List<Activity> activities = loadActivities(deptFilter, activityType, scoreType, startDate, endDate);
            return Response.success("Activity statistics retrieved successfully",
                    buildActivityStatisticsResponse(activities, deptFilter));
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
            List<Student> students = loadStudents(deptFilter, classId, semesterId);
            return Response.success("Student statistics retrieved successfully",
                    buildStudentStatisticsResponse(students, deptFilter));
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

            List<StudentScore> scores = loadFilteredScores(semesterId, scoreType, deptFilter, classId, studentId);
            return Response.success("Score statistics retrieved successfully",
                    buildScoreStatisticsFromScores(scores, scoreType));
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
            Semester semester = resolveSemester(semesterId);
            if (semester == null) {
                return Response.error(semesterId != null ? "Semester not found" : "No semester found");
            }
            List<ScoreEntry> entries = loadScoreBreakdownEntries(semester.getId(), studentId, deptFilter);
            return Response.success("Score breakdown retrieved successfully",
                    buildScoreBreakdownResponse(semester, studentId, entries));
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
            if (studentId != null) {
                return Response.success("Dashboard overview retrieved successfully",
                        buildStudentDashboardOverviewResponse(studentId));
            }
            return Response.success("Dashboard overview retrieved successfully",
                    buildDashboardOverviewResponse(null));
        } catch (Exception e) {
            logger.error("Error getting dashboard overview: {}", e.getMessage(), e);
            return Response.error("Failed to get dashboard overview: " + e.getMessage());
        }
    }

    private DashboardOverviewResponse buildDashboardOverviewResponse(Set<Long> deptIds) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endOfMonth = now.withDayOfMonth(now.toLocalDate().lengthOfMonth())
                .withHour(23).withMinute(59).withSecond(59);

        if (deptIds == null) {
            DashboardOverviewResponse response = new DashboardOverviewResponse();
            response.setTotalActivities(activityRepository.count());
            response.setTotalStudents(studentRepository.countAllActive());
            response.setTotalSeries(activitySeriesRepository.countAllActive());
            response.setTotalMiniGames(miniGameRepository.count());

            Long monthlyRegistrations = activityRegistrationRepository.countByDateRange(startOfMonth, endOfMonth);
            Long monthlyParticipations = activityParticipationRepository.countByDateRange(startOfMonth, endOfMonth);
            response.setMonthlyRegistrations(monthlyRegistrations);
            response.setMonthlyParticipations(monthlyParticipations);
            response.setAverageParticipationRate(monthlyRegistrations > 0
                    ? (double) monthlyParticipations / monthlyRegistrations
                    : 0.0);

            Pageable top5Page = PageRequest.of(0, 5);
            response.setTopActivities(loadTopDashboardActivitiesFromRepository(top5Page));
            response.setTopStudents(loadTopDashboardStudentsFromRepository(top5Page));
            return response;
        }

        List<Activity> activities = activityRepository.findAll(DepartmentScopeSpec.activity(deptIds));
        Set<Long> activityIds = activities.stream().map(Activity::getId).collect(Collectors.toSet());
        List<Student> students = studentRepository.findAll(DepartmentScopeSpec.student(deptIds));
        Set<Long> studentIds = students.stream().map(Student::getId).collect(Collectors.toSet());

        List<ActivityRegistration> registrations =
                activityRegistrationRepository.findAll(DepartmentScopeSpec.activityRegistration(deptIds));
        List<ActivityParticipation> participations =
                activityParticipationRepository.findAll(DepartmentScopeSpec.activityParticipation(deptIds));

        long monthlyRegistrations = registrations.stream()
                .filter(registration -> isWithinRange(resolveRegistrationDate(registration), startOfMonth, endOfMonth))
                .count();
        long monthlyParticipations = participations.stream()
                .filter(participation -> isWithinRange(participation.getDate(), startOfMonth, endOfMonth))
                .count();

        DashboardOverviewResponse response = new DashboardOverviewResponse();
        response.setTotalActivities((long) activities.size());
        response.setTotalStudents((long) students.size());
        response.setTotalSeries((long) activitySeriesRepository.findAll(DepartmentScopeSpec.activitySeries(deptIds)).size());
        response.setTotalMiniGames((long) miniGameRepository.findAll(DepartmentScopeSpec.miniGame(deptIds)).size());
        response.setMonthlyRegistrations(monthlyRegistrations);
        response.setMonthlyParticipations(monthlyParticipations);
        response.setAverageParticipationRate(monthlyRegistrations > 0
                ? (double) monthlyParticipations / monthlyRegistrations
                : 0.0);
        response.setTopActivities(buildTopDashboardActivities(registrations, participations, activityIds, 5));
        response.setTopStudents(buildTopDashboardStudents(participations, studentIds, 5));
        return response;
    }

    private DashboardOverviewResponse buildStudentDashboardOverviewResponse(Long studentId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endOfMonth = now.withDayOfMonth(now.toLocalDate().lengthOfMonth())
                .withHour(23).withMinute(59).withSecond(59);

        List<ActivityRegistration> registrations =
                activityRegistrationRepository.findByStudentIdAndStudentIsDeletedFalse(studentId);
        List<ActivityParticipation> participations = activityParticipationRepository.findAll(
                (root, query, cb) -> cb.equal(root.get("registration").get("student").get("id"), studentId));

        long monthlyRegistrations = registrations.stream()
                .filter(registration -> isWithinRange(resolveRegistrationDate(registration), startOfMonth, endOfMonth))
                .count();
        long monthlyParticipations = participations.stream()
                .filter(participation -> isWithinRange(participation.getDate(), startOfMonth, endOfMonth))
                .count();

        Set<Long> registeredActivityIds = registrations.stream()
                .map(registration -> registration.getActivity().getId())
                .collect(Collectors.toSet());
        Set<Long> registeredSeriesIds = registrations.stream()
                .map(ActivityRegistration::getSeriesId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        long participatedActivityIds = participations.stream()
                .map(participation -> participation.getRegistration().getActivity().getId())
                .distinct()
                .count();

        DashboardOverviewResponse response = new DashboardOverviewResponse();
        response.setTotalActivities((long) registeredActivityIds.size());
        response.setTotalStudents(0L);
        response.setTotalSeries((long) registeredSeriesIds.size());
        response.setTotalMiniGames(Optional.ofNullable(
                miniGameAttemptRepository.countDistinctMiniGamesByStudentId(studentId)).orElse(0L));
        response.setMonthlyRegistrations(monthlyRegistrations);
        response.setMonthlyParticipations(monthlyParticipations);
        response.setAverageParticipationRate(monthlyRegistrations > 0
                ? (double) monthlyParticipations / monthlyRegistrations
                : (registrations.isEmpty() ? 0.0 : (double) participatedActivityIds / registeredActivityIds.size()));
        response.setTopActivities(buildStudentTopActivities(registrations, participations));
        response.setTopStudents(List.of());
        return response;
    }

    private List<DashboardOverviewResponse.TopActivityItem> buildStudentTopActivities(
            List<ActivityRegistration> registrations,
            List<ActivityParticipation> participations) {
        Set<Long> participatedActivityIds = participations.stream()
                .map(participation -> participation.getRegistration().getActivity().getId())
                .collect(Collectors.toSet());

        return registrations.stream()
                .sorted(Comparator.comparing(
                        this::resolveRegistrationDate,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(5)
                .map(registration -> {
                    Activity activity = registration.getActivity();
                    DashboardOverviewResponse.TopActivityItem item = new DashboardOverviewResponse.TopActivityItem();
                    item.setActivityId(activity.getId());
                    item.setActivityName(activity.getName());
                    item.setRegistrationCount(1L);
                    item.setParticipationCount(participatedActivityIds.contains(activity.getId()) ? 1L : 0L);
                    return item;
                })
                .collect(Collectors.toList());
    }

    private List<DashboardOverviewResponse.TopActivityItem> loadTopDashboardActivitiesFromRepository(Pageable pageable) {
        List<DashboardOverviewResponse.TopActivityItem> topActivities = new ArrayList<>();
        for (Object[] data : activityRegistrationRepository.findTopActivitiesByRegistrations(pageable)) {
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
        return topActivities;
    }

    private List<DashboardOverviewResponse.TopStudentItem> loadTopDashboardStudentsFromRepository(Pageable pageable) {
        List<DashboardOverviewResponse.TopStudentItem> topStudents = new ArrayList<>();
        for (Object[] data : activityParticipationRepository.findTopStudentsByParticipations(pageable)) {
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
        return topStudents;
    }

    private List<DashboardOverviewResponse.TopActivityItem> buildTopDashboardActivities(
            List<ActivityRegistration> registrations,
            List<ActivityParticipation> participations,
            Set<Long> activityIds,
            int limit) {
        Map<Long, Long> registrationCounts = registrations.stream()
                .filter(registration -> registration.getActivity() != null
                        && activityIds.contains(registration.getActivity().getId()))
                .collect(Collectors.groupingBy(
                        registration -> registration.getActivity().getId(),
                        Collectors.counting()));
        Map<Long, Long> participationCounts = participations.stream()
                .filter(participation -> participation.getRegistration() != null
                        && participation.getRegistration().getActivity() != null
                        && activityIds.contains(participation.getRegistration().getActivity().getId()))
                .collect(Collectors.groupingBy(
                        participation -> participation.getRegistration().getActivity().getId(),
                        Collectors.counting()));

        return registrationCounts.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .limit(limit)
                .map(entry -> {
                    Long activityId = entry.getKey();
                    Activity activity = activityRepository.findById(activityId).orElse(null);
                    if (activity == null) {
                        return null;
                    }
                    DashboardOverviewResponse.TopActivityItem item = new DashboardOverviewResponse.TopActivityItem();
                    item.setActivityId(activityId);
                    item.setActivityName(activity.getName());
                    item.setRegistrationCount(entry.getValue());
                    item.setParticipationCount(participationCounts.getOrDefault(activityId, 0L));
                    return item;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private List<DashboardOverviewResponse.TopStudentItem> buildTopDashboardStudents(
            List<ActivityParticipation> participations,
            Set<Long> studentIds,
            int limit) {
        Map<Long, Long> participationCounts = participations.stream()
                .filter(participation -> participation.getRegistration() != null
                        && participation.getRegistration().getStudent() != null
                        && studentIds.contains(participation.getRegistration().getStudent().getId()))
                .collect(Collectors.groupingBy(
                        participation -> participation.getRegistration().getStudent().getId(),
                        Collectors.counting()));

        return participationCounts.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .limit(limit)
                .map(entry -> {
                    Student student = studentRepository.findById(entry.getKey()).orElse(null);
                    if (student == null) {
                        return null;
                    }
                    DashboardOverviewResponse.TopStudentItem item = new DashboardOverviewResponse.TopStudentItem();
                    item.setStudentId(student.getId());
                    item.setStudentName(student.getFullName());
                    item.setStudentCode(student.getStudentCode());
                    item.setParticipationCount(entry.getValue());
                    return item;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private LocalDateTime resolveRegistrationDate(ActivityRegistration registration) {
        return registration.getRegisteredDate() != null
                ? registration.getRegisteredDate()
                : registration.getCreatedAt();
    }

    private boolean isWithinRange(LocalDateTime value, LocalDateTime start, LocalDateTime end) {
        return value != null && !value.isBefore(start) && !value.isAfter(end);
    }

    @Override
    public Response getActivityStatistics(String activityType, String scoreType, Long departmentId,
            LocalDateTime startDate, LocalDateTime endDate) {
        try {
            if (hasActivityStatisticsFilters(activityType, scoreType, departmentId, startDate, endDate)) {
                Set<Long> deptFilter = departmentId != null ? Set.of(departmentId) : null;
                List<Activity> activities = loadActivities(deptFilter, activityType, scoreType, startDate, endDate);
                return Response.success("Activity statistics retrieved successfully",
                        buildActivityStatisticsResponse(activities, deptFilter));
            }

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
            if (departmentId != null || classId != null || semesterId != null) {
                Set<Long> deptFilter = departmentId != null ? Set.of(departmentId) : null;
                List<Student> students = loadStudents(deptFilter, classId, semesterId);
                return Response.success("Student statistics retrieved successfully",
                        buildStudentStatisticsResponse(students, deptFilter));
            }

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
            if (departmentId != null || classId != null || studentId != null) {
                Set<Long> deptFilter = departmentId != null ? Set.of(departmentId) : null;
                List<StudentScore> scores = loadFilteredScores(semesterId, scoreType, deptFilter, classId, studentId);
                return Response.success("Score statistics retrieved successfully",
                        buildScoreStatisticsFromScores(scores, scoreType));
            }

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
            List<ActivitySeries> allSeries = activitySeriesRepository.findAll().stream()
                    .filter(s -> !s.isDeleted())
                    .collect(Collectors.toList());
            return buildSeriesStatisticsResponse(seriesId, semesterId, allSeries);
        } catch (Exception e) {
            logger.error("Error getting series statistics: {}", e.getMessage(), e);
            return Response.error("Failed to get series statistics: " + e.getMessage());
        }
    }

    @Override
    public Response getSeriesStatistics(Long seriesId, Long semesterId, DepartmentScope scope) {
        if (scope == null || !scope.manager()) {
            return getSeriesStatistics(seriesId, semesterId);
        }
        try {
            if (seriesId != null) {
                departmentAuthorizationService.requireSeriesAccess(seriesId, scope);
            }
            List<ActivitySeries> scopedSeries =
                    activitySeriesRepository.findAll(DepartmentScopeSpec.activitySeries(scope.departmentIds()));
            return buildSeriesStatisticsResponse(seriesId, semesterId, scopedSeries);
        } catch (Exception e) {
            logger.error("Error getting scoped series statistics: {}", e.getMessage(), e);
            return Response.error("Failed to get series statistics: " + e.getMessage());
        }
    }

    private Response buildSeriesStatisticsResponse(Long seriesId, Long semesterId, List<ActivitySeries> seriesList) {
        SeriesStatisticsResponse response = new SeriesStatisticsResponse();
        response.setTotalSeries((long) seriesList.size());

        List<SeriesStatisticsResponse.SeriesDetailItem> seriesDetails = new ArrayList<>();
        Map<Long, Long> studentsPerSeries = new HashMap<>();
        Map<Long, BigDecimal> milestonePointsAwarded = new HashMap<>();

        for (ActivitySeries series : seriesList) {
            if (seriesId != null && !series.getId().equals(seriesId)) {
                continue;
            }

            Long totalActivities = (long) activityRepository.findBySeriesIdAndIsDeletedFalse(series.getId()).size();
            Long registeredStudents = activitySeriesRepository.countStudentsBySeriesId(series.getId());
            Long completedStudents = studentSeriesProgressRepository.countCompletedStudentsBySeriesId(series.getId());

            logger.debug("Series {} ({}): totalActivities={}, registeredStudents={}, completedStudents={}",
                    series.getId(), series.getName(), totalActivities, registeredStudents, completedStudents);

            Double completionRate = registeredStudents > 0
                    ? (double) completedStudents / registeredStudents
                    : 0.0;
            if (completionRate > 1.0) {
                logger.warn("Series {}: completionRate > 1.0 ({}), capping at 1.0",
                        series.getId(), completionRate);
                completionRate = 1.0;
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
            milestonePointsAwarded.put(series.getId(), BigDecimal.ZERO);
        }

        response.setSeriesDetails(seriesDetails);
        response.setStudentsPerSeries(studentsPerSeries);
        response.setMilestonePointsAwarded(milestonePointsAwarded);

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
    }

    @Override
    public Response getMiniGameStatistics(Long miniGameId, LocalDateTime startDate, LocalDateTime endDate) {
        try {
            List<MiniGame> allMiniGames = miniGameRepository.findAll();
            return buildMiniGameStatisticsResponse(miniGameId, startDate, endDate, allMiniGames);
        } catch (Exception e) {
            logger.error("Error getting minigame statistics: {}", e.getMessage(), e);
            return Response.error("Failed to get minigame statistics: " + e.getMessage());
        }
    }

    @Override
    public Response getMiniGameStatistics(Long miniGameId, LocalDateTime startDate, LocalDateTime endDate,
                                          DepartmentScope scope) {
        if (scope == null || !scope.manager()) {
            return getMiniGameStatistics(miniGameId, startDate, endDate);
        }
        try {
            if (miniGameId != null) {
                departmentAuthorizationService.requireMiniGameAccess(miniGameId, scope);
            }
            List<MiniGame> scopedMiniGames =
                    miniGameRepository.findAll(DepartmentScopeSpec.miniGame(scope.departmentIds()));
            return buildMiniGameStatisticsResponse(miniGameId, startDate, endDate, scopedMiniGames);
        } catch (Exception e) {
            logger.error("Error getting scoped minigame statistics: {}", e.getMessage(), e);
            return Response.error("Failed to get minigame statistics: " + e.getMessage());
        }
    }

    private Response buildMiniGameStatisticsResponse(Long miniGameId, LocalDateTime startDate, LocalDateTime endDate,
                                                     List<MiniGame> miniGames) {
        MiniGameStatisticsResponse response = new MiniGameStatisticsResponse();
        response.setTotalMiniGames((long) miniGames.size());

        long totalAttempts = 0;
        long passedAttempts = 0;
        long failedAttempts = 0;

        Map<Long, MiniGameStatisticsResponse.MiniGameDetailItem> miniGameDetails = new HashMap<>();
        Map<Long, BigDecimal> averageScoreByMiniGame = new HashMap<>();
        Map<Long, Double> averageCorrectAnswersByMiniGame = new HashMap<>();

        for (MiniGame miniGame : miniGames) {
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
            Double avgCorrect = miniGameAttemptRepository.calculateAverageCorrectAnswersByMiniGameId(miniGame.getId());

            totalAttempts += totalAttemptsForGame != null ? totalAttemptsForGame : 0;
            passedAttempts += passedForGame != null ? passedForGame : 0;
            failedAttempts += failedForGame != null ? failedForGame : 0;

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

        response.setTotalAttempts(totalAttempts);
        response.setPassedAttempts(passedAttempts);
        response.setFailedAttempts(failedAttempts);
        response.setPassRate(totalAttempts > 0 ? (double) passedAttempts / totalAttempts : 0.0);
        response.setMiniGameDetails(miniGameDetails);
        response.setAverageScoreByMiniGame(averageScoreByMiniGame);
        response.setAverageCorrectAnswersByMiniGame(averageCorrectAnswersByMiniGame);

        Set<Long> scopedMiniGameIds = miniGames.stream().map(MiniGame::getId).collect(Collectors.toSet());
        Pageable top10Page = PageRequest.of(0, 10);
        List<Object[]> popularData = miniGameAttemptRepository.findTopMiniGamesByAttempts(top10Page);
        List<MiniGameStatisticsResponse.PopularMiniGameItem> popularMiniGames = new ArrayList<>();
        for (Object[] data : popularData) {
            Long mgId = (Long) data[0];
            if (!scopedMiniGameIds.contains(mgId)) {
                continue;
            }
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
    }

    @Override
    public Response getScoreBreakdown(Long semesterId, Long studentId, Long departmentId) {
        try {
            Semester semester = resolveSemester(semesterId);
            if (semester == null) {
                return Response.error(semesterId != null ? "Semester not found" : "No semester found");
            }
            Set<Long> deptFilter = departmentId != null ? Set.of(departmentId) : null;
            List<ScoreEntry> entries = loadScoreBreakdownEntries(semester.getId(), studentId, deptFilter);
            return Response.success("Score breakdown retrieved successfully",
                    buildScoreBreakdownResponse(semester, studentId, entries));
        } catch (Exception e) {
            logger.error("Error getting score breakdown: {}", e.getMessage(), e);
            return Response.error("Failed to get score breakdown: " + e.getMessage());
        }
    }

    private boolean hasActivityStatisticsFilters(String activityType, String scoreType, Long departmentId,
            LocalDateTime startDate, LocalDateTime endDate) {
        return departmentId != null
                || (activityType != null && !activityType.isBlank())
                || (scoreType != null && !scoreType.isBlank())
                || startDate != null
                || endDate != null;
    }

    private List<Activity> loadActivities(Set<Long> deptIds, String activityType, String scoreType,
            LocalDateTime startDate, LocalDateTime endDate) {
        List<Activity> activities;
        if (deptIds != null) {
            activities = activityRepository.findAll(DepartmentScopeSpec.activity(deptIds));
        } else {
            activities = activityRepository.findByIsDeletedFalse();
        }
        return applyActivityFilters(activities, activityType, scoreType, startDate, endDate);
    }

    private List<Activity> applyActivityFilters(List<Activity> activities, String activityType, String scoreType,
            LocalDateTime startDate, LocalDateTime endDate) {
        List<Activity> result = new ArrayList<>(activities);
        if (activityType != null && !activityType.isBlank()) {
            ActivityType type = ActivityType.valueOf(activityType);
            result = result.stream().filter(activity -> activity.getType() == type).collect(Collectors.toList());
        }
        if (scoreType != null && !scoreType.isBlank()) {
            ScoreType parsedScoreType = ScoreType.valueOf(scoreType);
            Set<Long> activityIds = activityRepository
                    .findByScoreTypeAndIsDeletedFalseOrderByStartDateAsc(parsedScoreType)
                    .stream()
                    .map(Activity::getId)
                    .collect(Collectors.toSet());
            result = result.stream().filter(activity -> activityIds.contains(activity.getId())).collect(Collectors.toList());
        }
        if (startDate != null) {
            result = result.stream()
                    .filter(activity -> activity.getStartDate() != null && !activity.getStartDate().isBefore(startDate))
                    .collect(Collectors.toList());
        }
        if (endDate != null) {
            result = result.stream()
                    .filter(activity -> activity.getStartDate() != null && !activity.getStartDate().isAfter(endDate))
                    .collect(Collectors.toList());
        }
        return result;
    }

    private ActivityStatisticsResponse buildActivityStatisticsResponse(List<Activity> activities,
            Set<Long> deptFilter) {
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

        Set<Long> activityIds = activities.stream().map(Activity::getId).collect(Collectors.toSet());
        Pageable top10Page = PageRequest.of(0, 10);
        List<ActivityStatisticsResponse.TopActivityItem> topActivities = new ArrayList<>();
        for (Object[] data : activityRegistrationRepository.findTopActivitiesByRegistrations(top10Page)) {
            Long activityId = (Long) data[0];
            if (!activityIds.contains(activityId)) {
                continue;
            }
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

        List<ActivityStatisticsResponse.ActivityParticipationRate> participationRates = new ArrayList<>();
        for (ActivityStatisticsResponse.TopActivityItem item : topActivities) {
            Double rate = item.getRegistrationCount() > 0
                    ? (double) item.getParticipationCount() / item.getRegistrationCount()
                    : 0.0;
            ActivityStatisticsResponse.ActivityParticipationRate rateItem =
                    new ActivityStatisticsResponse.ActivityParticipationRate();
            rateItem.setActivityId(item.getActivityId());
            rateItem.setActivityName(item.getActivityName());
            rateItem.setRegistrationCount(item.getRegistrationCount());
            rateItem.setParticipationCount(item.getParticipationCount());
            rateItem.setParticipationRate(rate);
            participationRates.add(rateItem);
        }
        response.setParticipationRates(participationRates);

        Map<Long, Long> countByDepartment = new HashMap<>();
        if (deptFilter != null && !deptFilter.isEmpty()) {
            for (Long deptId : deptFilter) {
                countByDepartment.put(deptId, activities.stream()
                        .filter(activity -> activity.getOrganizers().stream()
                                .anyMatch(dept -> deptId.equals(dept.getId())))
                        .count());
            }
        } else {
            for (Department dept : departmentRepository.findAll()) {
                long count = activities.stream()
                        .filter(activity -> activity.getOrganizers().stream()
                                .anyMatch(organizer -> dept.getId().equals(organizer.getId())))
                        .count();
                if (count > 0) {
                    countByDepartment.put(dept.getId(), count);
                }
            }
        }
        response.setCountByDepartment(countByDepartment);
        response.setActivitiesInSeries(activities.stream().filter(activity -> activity.getSeriesId() != null).count());
        response.setStandaloneActivities(activities.stream().filter(activity -> activity.getSeriesId() == null).count());
        return response;
    }

    private List<Student> loadStudents(Set<Long> deptIds, Long classId, Long semesterId) {
        List<Student> students;
        if (deptIds != null) {
            students = studentRepository.findAll(DepartmentScopeSpec.student(deptIds));
        } else {
            students = studentRepository.findAll(activeStudentSpec());
        }
        if (classId != null) {
            students = students.stream()
                    .filter(student -> student.getStudentClass() != null
                            && classId.equals(student.getStudentClass().getId()))
                    .collect(Collectors.toList());
        }
        if (semesterId != null) {
            Set<Long> studentIdsWithScores = studentScoreRepository.findBySemesterIdOrderByScoreDesc(semesterId)
                    .stream()
                    .map(score -> score.getStudent().getId())
                    .collect(Collectors.toSet());
            students = students.stream()
                    .filter(student -> studentIdsWithScores.contains(student.getId()))
                    .collect(Collectors.toList());
        }
        return students;
    }

    private Specification<Student> activeStudentSpec() {
        return (root, query, cb) -> cb.isFalse(root.get("isDeleted"));
    }

    private StudentStatisticsResponse buildStudentStatisticsResponse(List<Student> students, Set<Long> deptFilter) {
        StudentStatisticsResponse response = new StudentStatisticsResponse();
        response.setTotalStudents((long) students.size());

        Map<Long, Long> countByDepartment = new HashMap<>();
        if (deptFilter != null && !deptFilter.isEmpty()) {
            for (Long deptId : deptFilter) {
                countByDepartment.put(deptId, students.stream()
                        .filter(student -> student.getDepartment() != null && deptId.equals(student.getDepartment().getId()))
                        .count());
            }
        } else {
            students.stream()
                    .filter(student -> student.getDepartment() != null)
                    .forEach(student -> countByDepartment.merge(student.getDepartment().getId(), 1L, Long::sum));
        }
        response.setCountByDepartment(countByDepartment);

        Map<Long, Long> countByClass = new HashMap<>();
        students.stream()
                .filter(student -> student.getStudentClass() != null)
                .forEach(student -> countByClass.merge(student.getStudentClass().getId(), 1L, Long::sum));
        response.setCountByClass(countByClass);

        Set<Long> studentIds = students.stream().map(Student::getId).collect(Collectors.toSet());
        Pageable top10Page = PageRequest.of(0, 10);
        List<StudentStatisticsResponse.TopParticipantItem> topParticipants = new ArrayList<>();
        for (Object[] data : activityParticipationRepository.findTopStudentsByParticipations(top10Page)) {
            Long participantId = (Long) data[0];
            if (!studentIds.contains(participantId)) {
                continue;
            }
            Long partCount = (Long) data[1];
            Student student = studentRepository.findById(participantId).orElse(null);
            if (student != null) {
                StudentStatisticsResponse.TopParticipantItem item = new StudentStatisticsResponse.TopParticipantItem();
                item.setStudentId(participantId);
                item.setStudentName(student.getFullName());
                item.setStudentCode(student.getStudentCode());
                item.setParticipationCount(partCount);
                topParticipants.add(item);
            }
        }
        response.setTopParticipants(topParticipants);

        List<StudentStatisticsResponse.InactiveStudentItem> inactiveItems = new ArrayList<>();
        for (Student student : studentRepository.findInactiveStudents()) {
            if (!studentIds.contains(student.getId())) {
                continue;
            }
            String deptName = student.getDepartment() != null ? student.getDepartment().getName() : "N/A";
            StudentStatisticsResponse.InactiveStudentItem item = new StudentStatisticsResponse.InactiveStudentItem();
            item.setStudentId(student.getId());
            item.setStudentName(student.getFullName());
            item.setStudentCode(student.getStudentCode());
            item.setDepartmentName(deptName);
            inactiveItems.add(item);
        }
        response.setInactiveStudents(inactiveItems);

        List<StudentStatisticsResponse.LowParticipationRateItem> lowRateItems = new ArrayList<>();
        for (StudentStatisticsResponse.TopParticipantItem participant : topParticipants) {
            Long regCount = activityRegistrationRepository.countByStudentId(participant.getStudentId());
            Long partCount = participant.getParticipationCount();
            if (regCount > partCount && regCount > 0) {
                double rate = (double) partCount / regCount;
                if (rate < 0.5) {
                    StudentStatisticsResponse.LowParticipationRateItem item =
                            new StudentStatisticsResponse.LowParticipationRateItem();
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
        return response;
    }

    private List<StudentScore> loadFilteredScores(Long semesterId, String scoreType, Set<Long> deptIds,
            Long classId, Long studentId) {
        List<StudentScore> scores;
        if (deptIds != null) {
            scores = studentScoreRepository.findAll(DepartmentScopeSpec.studentScore(deptIds));
        } else if (classId != null) {
            scores = studentScoreRepository.findAll((root, query, cb) -> cb.and(
                    cb.isFalse(root.get("student").get("isDeleted")),
                    cb.equal(root.get("student").get("studentClass").get("id"), classId)));
        } else {
            scores = studentScoreRepository.findAll((root, query, cb) -> cb.isFalse(root.get("student").get("isDeleted")));
        }
        if (semesterId != null) {
            scores = scores.stream()
                    .filter(score -> score.getSemester() != null && semesterId.equals(score.getSemester().getId()))
                    .collect(Collectors.toList());
        }
        if (scoreType != null && !scoreType.isBlank()) {
            scores = scores.stream()
                    .filter(score -> score.getScoreType().name().equals(scoreType))
                    .collect(Collectors.toList());
        }
        if (classId != null && deptIds != null) {
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
        return scores;
    }

    private ScoreStatisticsResponse buildScoreStatisticsFromScores(List<StudentScore> scores, String scoreType) {
        ScoreStatisticsResponse response = new ScoreStatisticsResponse();
        Map<ScoreType, ScoreStatisticsResponse.ScoreTypeStatistics> statisticsByType = new HashMap<>();
        for (ScoreType type : ScoreType.values()) {
            if (scoreType != null && !type.name().equals(scoreType)) {
                continue;
            }
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

        Map<Long, BigDecimal> averageByDepartment = new HashMap<>();
        scores.stream()
                .filter(score -> score.getStudent().getDepartment() != null && score.getScore() != null)
                .collect(Collectors.groupingBy(score -> score.getStudent().getDepartment().getId()))
                .forEach((deptId, deptScores) -> {
                    BigDecimal avg = deptScores.stream()
                            .map(StudentScore::getScore)
                            .reduce(BigDecimal.ZERO, BigDecimal::add)
                            .divide(BigDecimal.valueOf(deptScores.size()), 2, java.math.RoundingMode.HALF_UP);
                    averageByDepartment.put(deptId, avg);
                });
        response.setAverageByDepartment(averageByDepartment);

        Map<Long, BigDecimal> averageByClass = new HashMap<>();
        scores.stream()
                .filter(score -> score.getStudent().getStudentClass() != null && score.getScore() != null)
                .collect(Collectors.groupingBy(score -> score.getStudent().getStudentClass().getId()))
                .forEach((classIdKey, classScores) -> {
                    BigDecimal avg = classScores.stream()
                            .map(StudentScore::getScore)
                            .reduce(BigDecimal.ZERO, BigDecimal::add)
                            .divide(BigDecimal.valueOf(classScores.size()), 2, java.math.RoundingMode.HALF_UP);
                    averageByClass.put(classIdKey, avg);
                });
        response.setAverageByClass(averageByClass);

        Map<Long, BigDecimal> averageBySemester = new HashMap<>();
        scores.stream()
                .filter(score -> score.getSemester() != null && score.getScore() != null)
                .collect(Collectors.groupingBy(score -> score.getSemester().getId()))
                .forEach((semesterIdKey, semesterScores) -> {
                    BigDecimal avg = semesterScores.stream()
                            .map(StudentScore::getScore)
                            .reduce(BigDecimal.ZERO, BigDecimal::add)
                            .divide(BigDecimal.valueOf(semesterScores.size()), 2, java.math.RoundingMode.HALF_UP);
                    averageBySemester.put(semesterIdKey, avg);
                });
        response.setAverageBySemester(averageBySemester);

        Map<String, Long> scoreDistribution = new HashMap<>();
        for (StudentScore score : scores) {
            BigDecimal value = score.getScore() != null ? score.getScore() : BigDecimal.ZERO;
            String range = getScoreRange(value);
            scoreDistribution.put(range, scoreDistribution.getOrDefault(range, 0L) + 1);
        }
        response.setScoreDistribution(scoreDistribution);
        return response;
    }

    private Semester resolveSemester(Long semesterId) {
        if (semesterId != null) {
            return semesterRepository.findById(semesterId).orElse(null);
        }
        return semesterRepository.findAll().stream()
                .filter(Semester::isOpen)
                .findFirst()
                .orElse(semesterRepository.findAll().stream().findFirst().orElse(null));
    }

    private List<ScoreEntry> loadScoreBreakdownEntries(Long semesterId, Long studentId, Set<Long> deptIds) {
        List<ScoreEntry> entries;
        if (deptIds != null) {
            entries = scoreEntryRepository.findAll(DepartmentScopeSpec.scoreEntry(deptIds));
        } else {
            entries = scoreEntryRepository.findAll((root, query, cb) ->
                    cb.equal(root.get("status"), ScoreEntryStatus.ACTIVE));
        }
        return entries.stream()
                .filter(entry -> entry.getSemester() != null && semesterId.equals(entry.getSemester().getId()))
                .filter(entry -> entry.getStatus() == ScoreEntryStatus.ACTIVE)
                .filter(entry -> studentId == null
                        || (entry.getStudent() != null && studentId.equals(entry.getStudent().getId())))
                .collect(Collectors.toList());
    }

    private ScoreBreakdownResponse buildScoreBreakdownResponse(Semester semester, Long studentId,
            List<ScoreEntry> entries) {
        ScoreBreakdownResponse response = new ScoreBreakdownResponse();
        response.setSemesterId(semester.getId());
        response.setSemesterName(semester.getName());
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
        return response;
    }

}
