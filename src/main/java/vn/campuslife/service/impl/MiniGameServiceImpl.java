package vn.campuslife.service.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.campuslife.entity.*;
import vn.campuslife.enumeration.AttemptStatus;
import vn.campuslife.enumeration.MiniGameType;
import vn.campuslife.enumeration.ParticipationType;
import vn.campuslife.enumeration.RegistrationStatus;
import vn.campuslife.enumeration.ScoreType;
import vn.campuslife.model.*;
import vn.campuslife.repository.*;
import vn.campuslife.service.ActivitySeriesService;
import vn.campuslife.service.MiniGameService;
import vn.campuslife.service.SemesterHelperService;
import vn.campuslife.util.UrlUtils;
import org.springframework.beans.factory.annotation.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MiniGameServiceImpl implements MiniGameService {

    private static final Logger logger = LoggerFactory.getLogger(MiniGameServiceImpl.class);

    @Value("${app.upload.public-url:http://localhost:8080}")
    private String publicUrl;

    private final MiniGameRepository miniGameRepository;
    private final MiniGameQuizRepository quizRepository;
    private final MiniGameQuizQuestionRepository questionRepository;
    private final MiniGameQuizOptionRepository optionRepository;
    private final MiniGameAttemptRepository attemptRepository;
    private final MiniGameAnswerRepository answerRepository;
    private final ActivityRepository activityRepository;
    private final StudentRepository studentRepository;
    private final ActivityRegistrationRepository registrationRepository;
    private final ActivityParticipationRepository participationRepository;
    private final StudentScoreRepository studentScoreRepository;
    private final ScoreHistoryRepository scoreHistoryRepository;
    private final SemesterRepository semesterRepository;
    private final UserRepository userRepository;
    private final ActivitySeriesService activitySeriesService;
    private final SemesterHelperService semesterHelperService;

    @Override
    @Transactional
    public Response createMiniGame(Long activityId, String title, String description, Integer questionCount,
            Integer timeLimit, Integer requiredCorrectAnswers, BigDecimal rewardPoints, Integer maxAttempts,
            List<Map<String, Object>> questions) {
        try {
            Optional<Activity> activityOpt = activityRepository.findById(activityId);
            if (activityOpt.isEmpty()) {
                return Response.error("Activity not found");
            }

            Activity activity = activityOpt.get();
            if (activity.getType() != vn.campuslife.enumeration.ActivityType.MINIGAME) {
                return Response.error("Activity type must be MINIGAME");
            }

            // Validation: Nếu activity đơn lẻ (không thuộc series), rewardPoints nên có giá
            // trị
            // Nếu activity thuộc series, rewardPoints có thể null (sẽ tính từ milestone)
            if (activity.getSeriesId() == null
                    && (rewardPoints == null || rewardPoints.compareTo(BigDecimal.ZERO) <= 0)) {
                logger.warn("Standalone minigame should have rewardPoints > 0. Activity: {}", activityId);
                // Không fail, chỉ warning (có thể có trường hợp đặc biệt)
            }

            // Kiểm tra xem activity đã có minigame chưa (đảm bảo 1 activity chỉ có 1
            // minigame)
            Optional<MiniGame> existingMiniGameOpt = miniGameRepository.findByActivityId(activityId);
            if (existingMiniGameOpt.isPresent()) {
                return Response.error("Activity already has a minigame. Use update API to modify it.");
            }

            // Tạo MiniGame
            MiniGame miniGame = new MiniGame();
            miniGame.setTitle(title);
            miniGame.setDescription(description);
            miniGame.setQuestionCount(questionCount);
            miniGame.setTimeLimit(timeLimit);
            miniGame.setActive(true);
            miniGame.setType(MiniGameType.QUIZ);
            miniGame.setActivity(activity);
            miniGame.setRequiredCorrectAnswers(requiredCorrectAnswers);
            miniGame.setRewardPoints(rewardPoints);
            miniGame.setMaxAttempts(maxAttempts);
            MiniGame savedMiniGame = miniGameRepository.save(miniGame);

            // Tạo MiniGameQuiz
            MiniGameQuiz quiz = new MiniGameQuiz();
            quiz.setMiniGame(savedMiniGame);
            MiniGameQuiz savedQuiz = quizRepository.save(quiz);

            // Tạo questions và options
            int order = 0;
            for (Map<String, Object> questionData : questions) {
                MiniGameQuizQuestion question = new MiniGameQuizQuestion();
                question.setQuestionText((String) questionData.get("questionText"));
                // Normalize imageUrl to relative path for storage (extract from full URL if
                // needed)
                String imageUrl = (String) questionData.get("imageUrl");
                if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                    imageUrl = UrlUtils.toRelativePath(imageUrl, publicUrl);
                }
                question.setImageUrl(imageUrl);
                question.setMiniGameQuiz(savedQuiz);
                question.setDisplayOrder(order++);
                MiniGameQuizQuestion savedQuestion = questionRepository.save(question);

                // Tạo options
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> options = (List<Map<String, Object>>) questionData.get("options");
                if (options != null) {
                    for (Map<String, Object> optionData : options) {
                        MiniGameQuizOption option = new MiniGameQuizOption();
                        option.setText((String) optionData.get("text"));
                        option.setCorrect((Boolean) optionData.getOrDefault("isCorrect", false));
                        option.setQuestion(savedQuestion);
                        optionRepository.save(option);
                    }
                }
            }

            logger.info("Created minigame {} for activity {}", savedMiniGame.getId(), activityId);
            return Response.success("MiniGame created successfully", savedMiniGame);
        } catch (Exception e) {
            logger.error("Failed to create minigame: {}", e.getMessage(), e);
            return Response.error("Failed to create minigame: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Response getMiniGameByActivity(Long activityId) {
        try {
            Optional<MiniGame> miniGameOpt = miniGameRepository.findByActivityId(activityId);
            if (miniGameOpt.isEmpty()) {
                return Response.error("MiniGame not found for this activity");
            }
            MiniGameResponse response = MiniGameResponse.fromEntity(miniGameOpt.get());
            return Response.success("MiniGame retrieved successfully", response);
        } catch (Exception e) {
            logger.error("Failed to get minigame: {}", e.getMessage(), e);
            return Response.error("Failed to get minigame: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Response startAttempt(Long miniGameId, Long studentId) {
        try {
            Optional<MiniGame> miniGameOpt = miniGameRepository.findById(miniGameId);
            if (miniGameOpt.isEmpty()) {
                return Response.error("MiniGame not found");
            }

            Optional<Student> studentOpt = studentRepository.findById(studentId);
            if (studentOpt.isEmpty()) {
                return Response.error("Student not found");
            }

            MiniGame miniGame = miniGameOpt.get();
            if (!miniGame.isActive()) {
                return Response.error("MiniGame is not active");
            }

            // Kiểm tra maxAttempts
            if (miniGame.getMaxAttempts() != null) {
                List<MiniGameAttempt> allAttempts = attemptRepository.findByStudentIdAndMiniGameId(studentId,
                        miniGameId);
                int totalAttempts = allAttempts.size();
                if (totalAttempts >= miniGame.getMaxAttempts()) {
                    return Response.error("Bạn đã đạt số lần làm quiz tối đa (" + miniGame.getMaxAttempts() + " lần)");
                }
            }

            // Kiểm tra xem có attempt đang làm chưa
            Optional<MiniGameAttempt> inProgressOpt = attemptRepository.findInProgressAttempt(
                    studentId, miniGameId, AttemptStatus.IN_PROGRESS);
            if (inProgressOpt.isPresent()) {
                StartAttemptResponse response = StartAttemptResponse.fromEntity(inProgressOpt.get());
                return Response.success("Resuming existing attempt", response);
            }

            // Tạo attempt mới
            MiniGameAttempt attempt = new MiniGameAttempt();
            attempt.setMiniGame(miniGame);
            attempt.setStudent(studentOpt.get());
            attempt.setCorrectCount(0);
            attempt.setStatus(AttemptStatus.IN_PROGRESS);
            attempt.setStartedAt(LocalDateTime.now());
            MiniGameAttempt savedAttempt = attemptRepository.save(attempt);

            logger.info("Started attempt {} for student {} and minigame {}", savedAttempt.getId(), studentId,
                    miniGameId);
            StartAttemptResponse response = StartAttemptResponse.fromEntity(savedAttempt);
            return Response.success("Attempt started successfully", response);
        } catch (Exception e) {
            logger.error("Failed to start attempt: {}", e.getMessage(), e);
            return Response.error("Failed to start attempt: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Response submitAttempt(Long attemptId, Long studentId, Map<Long, Long> answers) {
        try {
            Optional<MiniGameAttempt> attemptOpt = attemptRepository.findById(attemptId);
            if (attemptOpt.isEmpty()) {
                return Response.error("Attempt not found");
            }

            MiniGameAttempt attempt = attemptOpt.get();
            if (!attempt.getStudent().getId().equals(studentId)) {
                return Response.error("Unauthorized to submit this attempt");
            }

            if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
                return Response.error("Attempt is not in progress");
            }

            // Lưu answers và tính điểm
            int correctCount = 0;
            for (Map.Entry<Long, Long> entry : answers.entrySet()) {
                Long questionId = entry.getKey();
                Long optionId = entry.getValue();

                Optional<MiniGameQuizQuestion> questionOpt = questionRepository.findById(questionId);
                Optional<MiniGameQuizOption> optionOpt = optionRepository.findById(optionId);

                if (questionOpt.isPresent() && optionOpt.isPresent()) {
                    MiniGameQuizOption selectedOption = optionOpt.get();
                    boolean isCorrect = selectedOption.isCorrect();

                    MiniGameAnswer answer = new MiniGameAnswer();
                    answer.setAttempt(attempt);
                    answer.setQuestion(questionOpt.get());
                    answer.setSelectedOption(selectedOption);
                    answer.setIsCorrect(isCorrect);
                    answerRepository.save(answer);

                    if (isCorrect) {
                        correctCount++;
                    }
                }
            }

            // Cập nhật attempt
            attempt.setCorrectCount(correctCount);
            attempt.setSubmittedAt(LocalDateTime.now());

            // Xác định status dựa trên requiredCorrectAnswers
            MiniGame miniGame = attempt.getMiniGame();
            if (miniGame.getRequiredCorrectAnswers() != null) {
                if (correctCount >= miniGame.getRequiredCorrectAnswers()) {
                    attempt.setStatus(AttemptStatus.PASSED);
                } else {
                    attempt.setStatus(AttemptStatus.FAILED);
                }
            } else {
                // Nếu không có requiredCorrectAnswers, coi như PASSED nếu có điểm
                attempt.setStatus(AttemptStatus.PASSED);
            }

            attemptRepository.save(attempt);

            // Tính điểm và tạo ActivityParticipation nếu đạt
            Object participation = null;
            if (attempt.getStatus() == AttemptStatus.PASSED) {
                // Đạt: Cộng điểm từ rewardPoints
                calculateScoreAndCreateParticipation(attemptId);
                // Tìm participation vừa tạo
                Activity activity = miniGame.getActivity();
                Optional<ActivityRegistration> registrationOpt = registrationRepository
                        .findByActivityIdAndStudentId(activity.getId(), studentId);
                if (registrationOpt.isPresent()) {
                    Optional<ActivityParticipation> participationOpt = participationRepository
                            .findByRegistration(registrationOpt.get());
                    if (participationOpt.isPresent()) {
                        participation = participationOpt.get();
                    }
                }
            }
            // Không đạt (FAILED): Không làm gì (không trừ điểm)

            logger.info("Submitted attempt {} with {} correct answers", attemptId, correctCount);
            SubmitAttemptResponse response = SubmitAttemptResponse.fromEntity(attempt, participation);
            return Response.success("Attempt submitted successfully", response);
        } catch (Exception e) {
            logger.error("Failed to submit attempt: {}", e.getMessage(), e);
            return Response.error("Failed to submit attempt: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Response getStudentAttempts(Long studentId, Long miniGameId) {
        try {
            List<MiniGameAttempt> attempts = attemptRepository.findByStudentIdAndMiniGameId(studentId, miniGameId);
            List<MiniGameAttemptResponse> responses = attempts.stream()
                    .map(attempt -> {
                        BigDecimal pointsEarned = BigDecimal.ZERO;
                        if (attempt.getStatus() == AttemptStatus.PASSED &&
                                attempt.getMiniGame().getRewardPoints() != null) {
                            pointsEarned = attempt.getMiniGame().getRewardPoints();
                        }
                        return MiniGameAttemptResponse.fromEntity(attempt, pointsEarned);
                    })
                    .collect(Collectors.toList());
            return Response.success("Attempts retrieved successfully", responses);
        } catch (Exception e) {
            logger.error("Failed to get attempts: {}", e.getMessage(), e);
            return Response.error("Failed to get attempts: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Response calculateScoreAndCreateParticipation(Long attemptId) {
        try {
            Optional<MiniGameAttempt> attemptOpt = attemptRepository.findById(attemptId);
            if (attemptOpt.isEmpty()) {
                return Response.error("Attempt not found");
            }

            MiniGameAttempt attempt = attemptOpt.get();
            if (attempt.getStatus() != AttemptStatus.PASSED) {
                return Response.error("Attempt did not pass");
            }

            MiniGame miniGame = attempt.getMiniGame();
            Activity activity = miniGame.getActivity();
            Student student = attempt.getStudent();

            // Tính điểm (có thể null nếu quiz thuộc series)
            BigDecimal pointsEarned = miniGame.getRewardPoints() != null
                    ? miniGame.getRewardPoints()
                    : BigDecimal.ZERO;

            // Validation: Nếu activity đơn lẻ và không có rewardPoints, không tạo
            // participation
            // Nếu activity thuộc series, vẫn tạo participation (pointsEarned = 0) để update
            // series progress
            if (activity.getSeriesId() == null && pointsEarned.compareTo(BigDecimal.ZERO) <= 0) {
                logger.info("No points to award for standalone quiz attempt {}", attemptId);
                return Response.success("No points to award", null);
            }

            // Tìm hoặc tạo ActivityRegistration
            Optional<ActivityRegistration> registrationOpt = registrationRepository
                    .findByActivityIdAndStudentId(activity.getId(), student.getId());

            ActivityRegistration registration;
            if (registrationOpt.isPresent()) {
                registration = registrationOpt.get();
                if (registration.getStatus() != RegistrationStatus.APPROVED) {
                    registration.setStatus(RegistrationStatus.APPROVED);
                    registrationRepository.save(registration);
                }
            } else {
                // Tạo registration mới
                registration = new ActivityRegistration();
                registration.setActivity(activity);
                registration.setStudent(student);
                registration.setStatus(RegistrationStatus.APPROVED);
                registration.setRegisteredDate(LocalDateTime.now());
                // Nếu activity thuộc series, lưu seriesId để đồng bộ đăng ký chuỗi
                if (activity.getSeriesId() != null) {
                    registration.setSeriesId(activity.getSeriesId());
                }
                registration.setTicketCode(java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase());
                registration = registrationRepository.save(registration);
            }

            // QUAN TRỌNG: Kiểm tra xem đã có participation COMPLETED chưa
            // Điểm quiz chỉ được ghi nhận 1 lần khi PASSED lần đầu
            // Sau khi đã pass, dù làm lại pass hay fail đều không ảnh hưởng điểm
            Optional<ActivityParticipation> existingParticipationOpt = participationRepository
                    .findByRegistration(registration);
            if (existingParticipationOpt.isPresent()) {
                ActivityParticipation existingParticipation = existingParticipationOpt.get();
                // Nếu đã có participation COMPLETED (đã pass trước đó)
                if (existingParticipation.getParticipationType() == ParticipationType.COMPLETED
                        && existingParticipation.getIsCompleted()) {
                    logger.info(
                            "Participation already exists for quiz (already passed). Points already awarded. Attempt: {}",
                            attemptId);
                    // Không tạo lại, không cộng điểm thêm
                    return Response.success("Participation already exists. Points already awarded.",
                            existingParticipation);
                }
                // Nếu có participation nhưng chưa COMPLETED (trường hợp hiếm), xóa để tạo mới
                // Không trừ điểm vì chưa có điểm được cộng
                participationRepository.delete(existingParticipation);
            }

            // Tạo participation mới (chỉ khi chưa có participation COMPLETED)
            ActivityParticipation participation = new ActivityParticipation();
            participation.setRegistration(registration);
            participation.setParticipationType(ParticipationType.COMPLETED);
            participation.setDate(LocalDateTime.now());
            participation.setIsCompleted(true);

            // Xử lý điểm: Phân biệt activity đơn lẻ và activity trong series
            if (activity.getSeriesId() != null) {
                // Activity trong series → KHÔNG tính điểm từ rewardPoints
                participation.setPointsEarned(BigDecimal.ZERO);
            } else {
                // Activity đơn lẻ → tính điểm bình thường từ rewardPoints
                participation.setPointsEarned(pointsEarned);
            }

            participation = participationRepository.save(participation);

            // Set registration status to ATTENDED (quiz không có check-in/check-out)
            registration.setStatus(RegistrationStatus.ATTENDED);
            registrationRepository.save(registration);

            // Xử lý cập nhật điểm và series progress
            if (activity.getSeriesId() != null) {
                // Update series progress (điểm milestone sẽ được tính tự động)
                try {
                    activitySeriesService.updateStudentProgress(
                            student.getId(),
                            activity.getId());
                    logger.info("Updated series progress for minigame activity {} in series {}",
                            activity.getName(), activity.getSeriesId());
                } catch (Exception e) {
                    logger.warn("Failed to update series progress: {}", e.getMessage());
                    // Không fail nếu update series progress lỗi
                }
            } else {
                // Activity đơn lẻ → cập nhật StudentScore
                // Chỉ update nếu có điểm (pointsEarned > 0)
                if (pointsEarned.compareTo(BigDecimal.ZERO) > 0) {
                    updateStudentScoreFromParticipation(participation);
                } else {
                    logger.warn("Standalone quiz has no rewardPoints, skipping score update. Activity: {}",
                            activity.getId());
                }
            }

            logger.info("Created participation for attempt {} (series: {}, pointsEarned: {})",
                    attemptId, activity.getSeriesId() != null, pointsEarned);
            return Response.success("Score calculated and participation created", participation);
        } catch (Exception e) {
            logger.error("Failed to calculate score: {}", e.getMessage(), e);
            return Response.error("Failed to calculate score: " + e.getMessage());
        }
    }

    /**
     * Helper method để cập nhật StudentScore từ ActivityParticipation
     */
    private void updateStudentScoreFromParticipation(ActivityParticipation participation) {
        try {
            Student student = participation.getRegistration().getStudent();
            Activity activity = participation.getRegistration().getActivity();

            // Use SemesterHelperService to find semester based on activity timing
            Semester semester = semesterHelperService.getSemesterForActivity(activity);

            if (semester == null) {
                logger.warn("No semester found for score aggregation");
                return;
            }

            Optional<StudentScore> scoreOpt = studentScoreRepository
                    .findByStudentIdAndSemesterIdAndScoreType(
                            student.getId(),
                            semester.getId(),
                            activity.getScoreType());

            if (scoreOpt.isEmpty()) {
                logger.warn("No score record found for student {} scoreType {} in semester {}",
                        student.getId(), activity.getScoreType(), semester.getId());
                return;
            }

            StudentScore score = scoreOpt.get();

            // Tính lại tổng điểm từ tất cả ActivityParticipation COMPLETED trong cùng
            // semester
            // ✅ UPDATED: Filter thêm theo semester để đảm bảo tính đúng
            List<ActivityParticipation> allParticipations = participationRepository
                    .findAll()
                    .stream()
                    .filter(p -> {
                        if (!p.getRegistration().getStudent().getId().equals(student.getId())) {
                            return false;
                        }
                        if (!p.getRegistration().getActivity().getScoreType().equals(activity.getScoreType())) {
                            return false;
                        }
                        if (!p.getParticipationType().equals(ParticipationType.COMPLETED)) {
                            return false;
                        }
                        // Filter theo semester
                        Semester pSemester = semesterHelperService.getSemesterForActivity(
                                p.getRegistration().getActivity());
                        return pSemester != null && pSemester.getId().equals(semester.getId());
                    })
                    .collect(java.util.stream.Collectors.toList());

            BigDecimal totalFromParticipations = allParticipations.stream()
                    .map(p -> p.getPointsEarned() != null ? p.getPointsEarned() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // QUAN TRỌNG: Giữ nguyên điểm milestone từ series (nếu có)
            // Tính điểm milestone = điểm hiện tại - điểm từ participations cũ
            BigDecimal oldScore = score.getScore() != null ? score.getScore() : BigDecimal.ZERO;

            // Tính điểm từ participations CŨ (không bao gồm participation hiện tại)
            BigDecimal oldParticipationScore = allParticipations.stream()
                    .filter(p -> !p.getId().equals(participation.getId())) // Loại bỏ participation hiện tại
                    .map(p -> p.getPointsEarned() != null ? p.getPointsEarned() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Điểm milestone = điểm hiện tại - điểm từ participations cũ
            BigDecimal milestonePoints = oldScore.subtract(oldParticipationScore);
            if (milestonePoints.compareTo(BigDecimal.ZERO) < 0) {
                milestonePoints = BigDecimal.ZERO; // Không cho âm
            }

            // Tổng điểm MỚI = điểm từ participations MỚI + điểm milestone (giữ nguyên)
            BigDecimal total = totalFromParticipations.add(milestonePoints);

            // Cập nhật
            score.setScore(total);
            studentScoreRepository.save(score);

            // Tạo history
            User systemUser = userRepository.findAll().stream()
                    .filter(user -> user.getRole() == vn.campuslife.enumeration.Role.ADMIN
                            || user.getRole() == vn.campuslife.enumeration.Role.MANAGER)
                    .findFirst()
                    .orElse(null);

            ScoreHistory history = new ScoreHistory();
            history.setScore(score);
            history.setOldScore(oldScore);
            history.setNewScore(total);
            history.setChangedBy(systemUser != null ? systemUser : userRepository.findById(1L).orElse(null));
            history.setChangeDate(LocalDateTime.now());
            history.setReason("Score from minigame quiz: " + activity.getName());
            history.setActivityId(activity.getId());
            scoreHistoryRepository.save(history);

            logger.info(
                    "Updated student score from minigame participation: {} -> {} for student {} (participation: {}, milestone: {})",
                    oldScore, total, student.getId(), totalFromParticipations, milestonePoints);
        } catch (Exception e) {
            logger.error("Failed to update student score: {}", e.getMessage(), e);
        }
    }

    /**
     * Helper method để trừ điểm khi xóa participation cũ (re-attempt)
     */
    private void updateStudentScoreFromParticipationRemoval(ActivityParticipation participation) {
        try {
            Activity activity = participation.getRegistration().getActivity();
            Student student = participation.getRegistration().getStudent();
            ScoreType scoreType = activity.getScoreType();

            if (scoreType == null) {
                logger.warn("Activity {} has no scoreType, skipping score update", activity.getId());
                return;
            }

            // ✅ USE: SemesterHelperService to find semester based on activity timing
            Semester semester = semesterHelperService.getSemesterForActivity(activity);

            if (semester == null) {
                logger.warn("No semester found for score update");
                return;
            }

            Optional<StudentScore> scoreOpt = studentScoreRepository
                    .findByStudentIdAndSemesterIdAndScoreType(student.getId(), semester.getId(), scoreType);

            if (scoreOpt.isEmpty()) {
                logger.warn("No {} score record found for student {} in semester {}",
                        scoreType, student.getId(), semester.getId());
                return;
            }

            StudentScore score = scoreOpt.get();

            // Lấy tất cả participations của student cho scoreType này trong semester này
            // ✅ UPDATED: Filter thêm theo semester để đảm bảo tính đúng
            List<ActivityParticipation> allParticipations = participationRepository
                    .findByStudentIdAndScoreType(student.getId(), scoreType)
                    .stream()
                    .filter(p -> {
                        Semester pSemester = semesterHelperService.getSemesterForActivity(
                                p.getRegistration().getActivity());
                        return pSemester != null && pSemester.getId().equals(semester.getId());
                    })
                    .collect(Collectors.toList());

            // Tính tổng điểm từ tất cả participations (trừ participation đang xóa)
            BigDecimal totalFromParticipations = allParticipations.stream()
                    .filter(p -> p.getId() == null || !p.getId().equals(participation.getId()))
                    .map(p -> p.getPointsEarned() != null ? p.getPointsEarned() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // QUAN TRỌNG: Giữ nguyên điểm milestone từ series (nếu có)
            BigDecimal oldScore = score.getScore() != null ? score.getScore() : BigDecimal.ZERO;

            // Tính điểm từ participations CŨ (bao gồm participation đang xóa)
            BigDecimal oldParticipationScore = allParticipations.stream()
                    .map(p -> p.getPointsEarned() != null ? p.getPointsEarned() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Điểm milestone = điểm hiện tại - điểm từ participations cũ
            BigDecimal milestonePoints = oldScore.subtract(oldParticipationScore);
            if (milestonePoints.compareTo(BigDecimal.ZERO) < 0) {
                milestonePoints = BigDecimal.ZERO; // Không cho âm
            }

            // Tổng điểm MỚI = điểm từ participations MỚI (đã trừ participation xóa) + điểm
            // milestone (giữ nguyên)
            BigDecimal total = totalFromParticipations.add(milestonePoints);

            // Cập nhật
            score.setScore(total);
            studentScoreRepository.save(score);

            // ✅ Tạo history để ghi lại việc xóa participation
            User systemUser = userRepository.findAll().stream()
                    .filter(user -> user.getRole() == vn.campuslife.enumeration.Role.ADMIN
                            || user.getRole() == vn.campuslife.enumeration.Role.MANAGER)
                    .findFirst()
                    .orElse(null);

            ScoreHistory history = new ScoreHistory();
            history.setScore(score);
            history.setOldScore(oldScore);
            history.setNewScore(total);
            history.setChangedBy(systemUser != null ? systemUser : userRepository.findById(1L).orElse(null));
            history.setChangeDate(LocalDateTime.now());
            history.setReason("Removed minigame participation (re-attempt). Activity: " + activity.getName() +
                            ". Milestone preserved: " + milestonePoints);
            history.setActivityId(activity.getId());
            scoreHistoryRepository.save(history);

            logger.info("Removed participation score, updated {} score: {} -> {} for student {} in semester {}",
                    scoreType, oldScore, total, student.getId(), semester.getId());
        } catch (Exception e) {
            logger.error("Failed to remove participation score: {}", e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Response getQuestions(Long miniGameId) {
        try {
            Optional<MiniGame> miniGameOpt = miniGameRepository.findById(miniGameId);
            if (miniGameOpt.isEmpty()) {
                return Response.error("MiniGame not found");
            }

            MiniGame miniGame = miniGameOpt.get();
            if (!miniGame.isActive()) {
                return Response.error("MiniGame is not active");
            }

            // Lấy quiz
            Optional<MiniGameQuiz> quizOpt = quizRepository.findByMiniGameId(miniGame.getId());
            if (quizOpt.isEmpty()) {
                return Response.error("Quiz not found for this minigame");
            }

            MiniGameQuiz quiz = quizOpt.get();

            // Lấy tất cả questions và sắp xếp theo displayOrder
            List<MiniGameQuizQuestion> questions = new ArrayList<>(quiz.getQuestions());
            questions.sort((q1, q2) -> {
                Integer order1 = q1.getDisplayOrder() != null ? q1.getDisplayOrder() : 0;
                Integer order2 = q2.getDisplayOrder() != null ? q2.getDisplayOrder() : 0;
                return order1.compareTo(order2);
            });

            // Build response using DTO (KHÔNG có isCorrect để student không biết đáp án)
            QuizQuestionsResponse response = QuizQuestionsResponse.fromEntities(miniGame, quiz, publicUrl);
            return Response.success("Questions retrieved successfully", response);
        } catch (Exception e) {
            logger.error("Failed to get questions: {}", e.getMessage(), e);
            return Response.error("Failed to get questions: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Response getAttemptDetail(Long attemptId, Long studentId) {
        try {
            Optional<MiniGameAttempt> attemptOpt = attemptRepository.findById(attemptId);
            if (attemptOpt.isEmpty()) {
                return Response.error("Attempt not found");
            }

            MiniGameAttempt attempt = attemptOpt.get();

            // Kiểm tra quyền: student chỉ xem được attempt của chính mình
            if (!attempt.getStudent().getId().equals(studentId)) {
                return Response.error("You can only view your own attempts");
            }

            MiniGame miniGame = attempt.getMiniGame();
            MiniGameQuiz quiz = quizRepository.findByMiniGameId(miniGame.getId())
                    .orElse(null);

            // Tính điểm đã nhận (chỉ khi PASSED)
            BigDecimal pointsEarned = BigDecimal.ZERO;
            if (attempt.getStatus() == AttemptStatus.PASSED && miniGame.getRewardPoints() != null) {
                pointsEarned = miniGame.getRewardPoints();
            }

            // Lấy student answers nếu đã submit
            Map<Long, Long> studentAnswers = null;
            if (attempt.getStatus() != AttemptStatus.IN_PROGRESS && quiz != null) {
                List<MiniGameAnswer> answers = answerRepository.findByAttemptId(attemptId);
                studentAnswers = new HashMap<>();
                for (MiniGameAnswer answer : answers) {
                    studentAnswers.put(answer.getQuestion().getId(), answer.getSelectedOption().getId());
                }
            }

            // Build response using DTO
            AttemptDetailResponse response = AttemptDetailResponse.fromEntities(
                    attempt, quiz, studentAnswers, pointsEarned, publicUrl);
            return Response.success("Attempt detail retrieved successfully", response);
        } catch (Exception e) {
            logger.error("Failed to get attempt detail: {}", e.getMessage(), e);
            return Response.error("Failed to get attempt detail: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Response updateMiniGame(Long miniGameId, String title, String description, Integer questionCount,
            Integer timeLimit, Integer requiredCorrectAnswers, BigDecimal rewardPoints,
            Integer maxAttempts, List<Map<String, Object>> questions) {
        try {
            Optional<MiniGame> miniGameOpt = miniGameRepository.findById(miniGameId);
            if (miniGameOpt.isEmpty()) {
                return Response.error("MiniGame not found");
            }

            MiniGame miniGame = miniGameOpt.get();

            // Cập nhật thông tin cơ bản
            if (title != null)
                miniGame.setTitle(title);
            if (description != null)
                miniGame.setDescription(description);
            if (questionCount != null)
                miniGame.setQuestionCount(questionCount);
            if (timeLimit != null)
                miniGame.setTimeLimit(timeLimit);
            if (requiredCorrectAnswers != null)
                miniGame.setRequiredCorrectAnswers(requiredCorrectAnswers);
            if (rewardPoints != null)
                miniGame.setRewardPoints(rewardPoints);
            if (maxAttempts != null)
                miniGame.setMaxAttempts(maxAttempts);

            // Nếu có questions mới, xóa cũ và tạo mới
            if (questions != null && !questions.isEmpty()) {
                // Lấy quiz hiện tại
                Optional<MiniGameQuiz> quizOpt = quizRepository.findByMiniGameId(miniGameId);
                if (quizOpt.isPresent()) {
                    MiniGameQuiz quiz = quizOpt.get();
                    // Xóa tất cả answers liên quan tới quiz này để tránh lỗi FK khi xóa options
                    answerRepository.deleteByQuizId(quiz.getId());
                    // Xóa tất cả questions và options cũ (cascade sẽ xóa options sau khi answers đã
                    // bị xóa)
                    questionRepository.deleteAll(quiz.getQuestions());
                    quiz.getQuestions().clear();
                }

                // Tạo lại questions và options
                MiniGameQuiz quiz = quizOpt.orElseGet(() -> {
                    MiniGameQuiz newQuiz = new MiniGameQuiz();
                    newQuiz.setMiniGame(miniGame);
                    return quizRepository.save(newQuiz);
                });

                int order = 0;
                for (Map<String, Object> questionData : questions) {
                    MiniGameQuizQuestion question = new MiniGameQuizQuestion();
                    question.setQuestionText((String) questionData.get("questionText"));
                    // Normalize imageUrl to relative path for storage (extract from full URL if
                    // needed)
                    String imageUrl = (String) questionData.get("imageUrl");
                    if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                        imageUrl = UrlUtils.toRelativePath(imageUrl, publicUrl);
                    }
                    question.setImageUrl(imageUrl);
                    question.setMiniGameQuiz(quiz);
                    question.setDisplayOrder(order++);
                    MiniGameQuizQuestion savedQuestion = questionRepository.save(question);

                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> options = (List<Map<String, Object>>) questionData.get("options");
                    if (options != null) {
                        for (Map<String, Object> optionData : options) {
                            MiniGameQuizOption option = new MiniGameQuizOption();
                            option.setText((String) optionData.get("text"));
                            option.setCorrect((Boolean) optionData.getOrDefault("isCorrect", false));
                            option.setQuestion(savedQuestion);
                            optionRepository.save(option);
                        }
                    }
                }
            }

            MiniGame updated = miniGameRepository.save(miniGame);
            logger.info("Updated minigame {}", miniGameId);
            return Response.success("MiniGame updated successfully", updated);
        } catch (Exception e) {
            logger.error("Failed to update minigame: {}", e.getMessage(), e);
            return Response.error("Failed to update minigame: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Response deleteMiniGame(Long miniGameId) {
        try {
            Optional<MiniGame> miniGameOpt = miniGameRepository.findById(miniGameId);
            if (miniGameOpt.isEmpty()) {
                return Response.error("MiniGame not found");
            }

            MiniGame miniGame = miniGameOpt.get();
            miniGame.setActive(false);
            miniGameRepository.save(miniGame);

            logger.info("Deleted (deactivated) minigame {}", miniGameId);
            return Response.success("MiniGame deleted successfully", null);
        } catch (Exception e) {
            logger.error("Failed to delete minigame: {}", e.getMessage(), e);
            return Response.error("Failed to delete minigame: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Response getAllMiniGames() {
        try {
            List<MiniGame> miniGames = miniGameRepository.findAll();
            List<MiniGameResponse> responses = miniGames.stream()
                    .map(MiniGameResponse::fromEntity)
                    .collect(Collectors.toList());
            return Response.success("MiniGames retrieved successfully", responses);
        } catch (Exception e) {
            logger.error("Failed to get all minigames: {}", e.getMessage(), e);
            return Response.error("Failed to get all minigames: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Response checkActivityHasQuiz(Long activityId) {
        try {
            // Kiểm tra activity tồn tại
            Optional<Activity> activityOpt = activityRepository.findById(activityId);
            if (activityOpt.isEmpty()) {
                return Response.error("Activity not found");
            }

            Activity activity = activityOpt.get();
            if (activity.getType() != vn.campuslife.enumeration.ActivityType.MINIGAME) {
                return Response.error("Activity type is not MINIGAME");
            }

            // Kiểm tra xem có minigame chưa
            Optional<MiniGame> miniGameOpt = miniGameRepository.findByActivityId(activityId);
            if (miniGameOpt.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("hasQuiz", false);
                response.put("message", "Activity does not have a minigame/quiz yet");
                return Response.success("Activity does not have quiz", response);
            }

            MiniGame miniGame = miniGameOpt.get();

            // Kiểm tra xem có quiz chưa
            Optional<MiniGameQuiz> quizOpt = quizRepository.findByMiniGameId(miniGame.getId());
            boolean hasQuiz = quizOpt.isPresent();

            Map<String, Object> response = new HashMap<>();
            response.put("hasQuiz", hasQuiz);
            response.put("miniGameId", miniGame.getId());
            response.put("miniGameTitle", miniGame.getTitle());
            response.put("isActive", miniGame.isActive());
            if (hasQuiz) {
                MiniGameQuiz quiz = quizOpt.get();
                response.put("quizId", quiz.getId());
                response.put("questionCount", quiz.getQuestions() != null ? quiz.getQuestions().size() : 0);
            }
            response.put("message",
                    hasQuiz ? "Activity already has a minigame/quiz" : "Activity has minigame but no quiz yet");

            return Response.success("Check completed", response);
        } catch (Exception e) {
            logger.error("Failed to check activity quiz: {}", e.getMessage(), e);
            return Response.error("Failed to check activity quiz: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Response getQuestionsForEdit(Long miniGameId) {
        try {
            Optional<MiniGame> miniGameOpt = miniGameRepository.findById(miniGameId);
            if (miniGameOpt.isEmpty()) {
                return Response.error("MiniGame not found");
            }

            MiniGame miniGame = miniGameOpt.get();

            // Lấy quiz (có thể không có nếu chưa tạo quiz)
            Optional<MiniGameQuiz> quizOpt = quizRepository.findByMiniGameId(miniGame.getId());

            // Build response với đáp án đúng (cho admin/manager edit)
            // Nếu chưa có quiz, trả về questions rỗng
            QuizQuestionsEditResponse response = QuizQuestionsEditResponse.fromEntities(
                    miniGame,
                    quizOpt.orElse(null),
                    publicUrl);

            return Response.success("Questions retrieved successfully for edit", response);
        } catch (Exception e) {
            logger.error("Failed to get questions for edit: {}", e.getMessage(), e);
            return Response.error("Failed to get questions for edit: " + e.getMessage());
        }
    }

}
