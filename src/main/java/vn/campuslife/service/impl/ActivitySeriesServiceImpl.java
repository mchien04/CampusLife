package vn.campuslife.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.campuslife.entity.*;
import vn.campuslife.enumeration.ScoreType;
import vn.campuslife.model.Response;
import vn.campuslife.repository.*;
import vn.campuslife.service.ActivitySeriesService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ActivitySeriesServiceImpl implements ActivitySeriesService {

    private static final Logger logger = LoggerFactory.getLogger(ActivitySeriesServiceImpl.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final ActivitySeriesRepository seriesRepository;
    private final StudentSeriesProgressRepository progressRepository;
    private final ActivityRepository activityRepository;
    private final StudentRepository studentRepository;
    private final StudentScoreRepository studentScoreRepository;
    private final ScoreHistoryRepository scoreHistoryRepository;
    private final SemesterRepository semesterRepository;
    private final UserRepository userRepository;
    private final ActivityParticipationRepository participationRepository;

    @Override
    @Transactional
    public Response createSeries(String name, String description, String milestonePointsJson, 
                                vn.campuslife.enumeration.ScoreType scoreType, Long mainActivityId) {
        try {
            ActivitySeries series = new ActivitySeries();
            series.setName(name);
            series.setDescription(description);
            series.setMilestonePoints(milestonePointsJson);
            series.setScoreType(scoreType);
            series.setCreatedAt(LocalDateTime.now());

            if (mainActivityId != null) {
                Optional<Activity> mainActivityOpt = activityRepository.findById(mainActivityId);
                if (mainActivityOpt.isPresent()) {
                    series.setMainActivity(mainActivityOpt.get());
                }
            }

            ActivitySeries saved = seriesRepository.save(series);
            logger.info("Created activity series: {} with scoreType: {}", saved.getId(), scoreType);
            return Response.success("Activity series created successfully", saved);
        } catch (Exception e) {
            logger.error("Failed to create series: {}", e.getMessage(), e);
            return Response.error("Failed to create series: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Response addActivityToSeries(Long activityId, Long seriesId, Integer order) {
        try {
            Optional<Activity> activityOpt = activityRepository.findById(activityId);
            if (activityOpt.isEmpty()) {
                return Response.error("Activity not found");
            }

            Optional<ActivitySeries> seriesOpt = seriesRepository.findById(seriesId);
            if (seriesOpt.isEmpty()) {
                return Response.error("Series not found");
            }

            Activity activity = activityOpt.get();
            activity.setSeriesId(seriesId);
            activity.setSeriesOrder(order);
            activityRepository.save(activity);

            logger.info("Added activity {} to series {} with order {}", activityId, seriesId, order);
            return Response.success("Activity added to series successfully", activity);
        } catch (Exception e) {
            logger.error("Failed to add activity to series: {}", e.getMessage(), e);
            return Response.error("Failed to add activity to series: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Response updateStudentProgress(Long studentId, Long activityId) {
        try {
            Optional<Activity> activityOpt = activityRepository.findById(activityId);
            if (activityOpt.isEmpty()) {
                return Response.error("Activity not found");
            }

            Activity activity = activityOpt.get();
            if (activity.getSeriesId() == null) {
                return Response.success("Activity is not part of a series", null);
            }

            Optional<Student> studentOpt = studentRepository.findById(studentId);
            if (studentOpt.isEmpty()) {
                return Response.error("Student not found");
            }

            Optional<ActivitySeries> seriesOpt = seriesRepository.findById(activity.getSeriesId());
            if (seriesOpt.isEmpty()) {
                return Response.error("Series not found");
            }

            Student student = studentOpt.get();
            ActivitySeries series = seriesOpt.get();

            // Tìm hoặc tạo progress record
            Optional<StudentSeriesProgress> progressOpt = progressRepository
                    .findByStudentIdAndSeriesId(studentId, activity.getSeriesId());

            StudentSeriesProgress progress;
            if (progressOpt.isPresent()) {
                progress = progressOpt.get();
            } else {
                progress = new StudentSeriesProgress();
                progress.setStudent(student);
                progress.setSeries(series);
                progress.setCompletedActivityIds("[]");
                progress.setCompletedCount(0);
                progress.setPointsEarned(BigDecimal.ZERO);
            }

            // Parse completed activity IDs
            String completedIdsJson = progress.getCompletedActivityIds() != null 
                    ? progress.getCompletedActivityIds() : "[]";
            List<Long> completedIds;
            try {
                completedIds = objectMapper.readValue(completedIdsJson, new TypeReference<List<Long>>() {});
            } catch (Exception e) {
                completedIds = new ArrayList<>();
            }

            // Kiểm tra xem activity đã được thêm chưa
            if (!completedIds.contains(activityId)) {
                completedIds.add(activityId);
                progress.setCompletedCount(completedIds.size());
                progress.setCompletedActivityIds(objectMapper.writeValueAsString(completedIds));
                progress.setLastUpdated(LocalDateTime.now());
                progressRepository.save(progress);

                // Tính lại milestone points
                calculateMilestonePoints(studentId, activity.getSeriesId());
            }

            logger.info("Updated progress for student {} in series {}", studentId, activity.getSeriesId());
            return Response.success("Student progress updated", progress);
        } catch (Exception e) {
            logger.error("Failed to update student progress: {}", e.getMessage(), e);
            return Response.error("Failed to update student progress: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Response calculateMilestonePoints(Long studentId, Long seriesId) {
        try {
            Optional<StudentSeriesProgress> progressOpt = progressRepository
                    .findByStudentIdAndSeriesId(studentId, seriesId);
            if (progressOpt.isEmpty()) {
                return Response.error("Progress not found");
            }

            Optional<ActivitySeries> seriesOpt = seriesRepository.findById(seriesId);
            if (seriesOpt.isEmpty()) {
                return Response.error("Series not found");
            }

            StudentSeriesProgress progress = progressOpt.get();
            ActivitySeries series = seriesOpt.get();

            if (series.getMilestonePoints() == null || series.getMilestonePoints().isEmpty()) {
                return Response.success("No milestone points configured", null);
            }

            // Parse milestone points JSON
            Map<String, Integer> milestonePoints;
            try {
                milestonePoints = objectMapper.readValue(series.getMilestonePoints(),
                        new TypeReference<Map<String, Integer>>() {});
            } catch (Exception e) {
                logger.error("Failed to parse milestone points: {}", e.getMessage());
                return Response.error("Invalid milestone points format");
            }

            // Tìm điểm milestone phù hợp với số sự kiện đã tham gia
            Integer completedCount = progress.getCompletedCount();
            BigDecimal pointsToAward = BigDecimal.ZERO;

            // Tìm milestone cao nhất mà student đã đạt được
            for (Map.Entry<String, Integer> entry : milestonePoints.entrySet()) {
                try {
                    Integer milestoneCount = Integer.parseInt(entry.getKey());
                    if (completedCount >= milestoneCount) {
                        Integer milestonePointsValue = entry.getValue();
                        if (milestonePointsValue > pointsToAward.intValue()) {
                            pointsToAward = BigDecimal.valueOf(milestonePointsValue);
                        }
                    }
                } catch (NumberFormatException e) {
                    logger.warn("Invalid milestone key: {}", entry.getKey());
                }
            }

            // Cập nhật pointsEarned nếu có thay đổi
            if (pointsToAward.compareTo(progress.getPointsEarned()) > 0) {
                BigDecimal oldPoints = progress.getPointsEarned();
                progress.setPointsEarned(pointsToAward);
                progressRepository.save(progress);

                // Cập nhật StudentScore REN_LUYEN
                updateRenLuyenScoreFromMilestone(studentId, seriesId, oldPoints, pointsToAward);

                logger.info("Awarded milestone points {} to student {} for series {}",
                        pointsToAward, studentId, seriesId);
            }

            return Response.success("Milestone points calculated", progress);
        } catch (Exception e) {
            logger.error("Failed to calculate milestone points: {}", e.getMessage(), e);
            return Response.error("Failed to calculate milestone points: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Response checkMinimumRequirement(Long studentId, Long seriesId) {
        // TODO: Implement penalty logic if student doesn't meet minimum requirement
        // This would need additional fields in ActivitySeries: minimumRequired, penaltyPoints
        return Response.success("Minimum requirement check not yet implemented", null);
    }

    /**
     * Cập nhật điểm từ milestone (dùng scoreType từ series)
     */
    private void updateRenLuyenScoreFromMilestone(Long studentId, Long seriesId, BigDecimal oldPoints, BigDecimal newPoints) {
        try {
            // Lấy series để lấy scoreType
            Optional<ActivitySeries> seriesOpt = seriesRepository.findById(seriesId);
            if (seriesOpt.isEmpty()) {
                logger.warn("Series not found: {}", seriesId);
                return;
            }
            ActivitySeries series = seriesOpt.get();
            ScoreType scoreType = series.getScoreType();
            
            // Lấy semester hiện tại
            Semester currentSemester = semesterRepository.findAll().stream()
                    .filter(Semester::isOpen)
                    .findFirst()
                    .orElse(semesterRepository.findAll().stream().findFirst().orElse(null));

            if (currentSemester == null) {
                logger.warn("No semester found for milestone score update");
                return;
            }

            Optional<StudentScore> scoreOpt = studentScoreRepository
                    .findByStudentIdAndSemesterIdAndScoreType(studentId, currentSemester.getId(), scoreType);

            if (scoreOpt.isEmpty()) {
                logger.warn("No {} score record found for student {}", scoreType, studentId);
                return;
            }

            StudentScore score = scoreOpt.get();
            BigDecimal currentScore = score.getScore() != null ? score.getScore() : BigDecimal.ZERO;
            
            // Trừ điểm cũ, cộng điểm mới
            BigDecimal updatedScore = currentScore.subtract(oldPoints).add(newPoints);
            BigDecimal oldTotalScore = score.getScore();
            score.setScore(updatedScore);
            studentScoreRepository.save(score);

            // Tạo history
            User systemUser = userRepository.findAll().stream()
                    .filter(user -> user.getRole() == vn.campuslife.enumeration.Role.ADMIN 
                            || user.getRole() == vn.campuslife.enumeration.Role.MANAGER)
                    .findFirst()
                    .orElse(null);

            ScoreHistory history = new ScoreHistory();
            history.setScore(score);
            history.setOldScore(oldTotalScore);
            history.setNewScore(updatedScore);
            history.setChangedBy(systemUser != null ? systemUser : userRepository.findById(1L).orElse(null));
            history.setChangeDate(LocalDateTime.now());
            history.setReason("Milestone points from series: " + seriesId);
            scoreHistoryRepository.save(history);

            logger.info("Updated {} score from milestone: {} -> {} for student {}",
                    scoreType, oldTotalScore, updatedScore, studentId);
        } catch (Exception e) {
            logger.error("Failed to update REN_LUYEN score from milestone: {}", e.getMessage(), e);
        }
    }
}

