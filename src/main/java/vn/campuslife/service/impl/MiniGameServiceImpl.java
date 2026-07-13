package vn.campuslife.service.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.campuslife.config.UploadProperties;
import vn.campuslife.entity.Activity;
import vn.campuslife.entity.ActivityParticipation;
import vn.campuslife.entity.ActivityRegistration;
import vn.campuslife.entity.MiniGame;
import vn.campuslife.entity.MiniGameAnswer;
import vn.campuslife.entity.MiniGameAttempt;
import vn.campuslife.entity.MiniGameQuiz;
import vn.campuslife.entity.MiniGameQuizOption;
import vn.campuslife.entity.MiniGameQuizQuestion;
import vn.campuslife.entity.ScoreEntry;
import vn.campuslife.entity.Student;
import vn.campuslife.enumeration.AttemptStatus;
import vn.campuslife.enumeration.MiniGameType;
import vn.campuslife.enumeration.ParticipationType;
import vn.campuslife.enumeration.RegistrationStatus;
import vn.campuslife.enumeration.ScoreEntrySourceType;
import vn.campuslife.enumeration.ScoreEntryStatus;
import vn.campuslife.model.Response;
import vn.campuslife.model.activity.quiz.AttemptDetailResponse;
import vn.campuslife.model.activity.quiz.CreateMiniGameRequest;
import vn.campuslife.model.activity.quiz.MiniGameAttemptResponse;
import vn.campuslife.model.activity.quiz.MiniGameResponse;
import vn.campuslife.model.activity.quiz.QuizQuestionsEditResponse;
import vn.campuslife.model.activity.quiz.QuizQuestionsResponse;
import vn.campuslife.model.activity.quiz.StartAttemptResponse;
import vn.campuslife.model.activity.quiz.SubmitAttemptResponse;
import vn.campuslife.model.activity.quiz.UpdateMiniGameRequest;
import vn.campuslife.repository.ActivityParticipationRepository;
import vn.campuslife.repository.ActivityRegistrationRepository;
import vn.campuslife.repository.ActivityRepository;
import vn.campuslife.repository.MiniGameAnswerRepository;
import vn.campuslife.repository.MiniGameAttemptRepository;
import vn.campuslife.repository.MiniGameQuizOptionRepository;
import vn.campuslife.repository.MiniGameQuizQuestionRepository;
import vn.campuslife.repository.MiniGameQuizRepository;
import vn.campuslife.repository.MiniGameRepository;
import vn.campuslife.repository.ScoreEntryRepository;
import vn.campuslife.repository.StudentRepository;
import vn.campuslife.security.department.DepartmentAuthorizationService;
import vn.campuslife.security.department.DepartmentScope;
import vn.campuslife.security.department.DepartmentScopeSpec;
import vn.campuslife.service.ActivitySeriesService;
import vn.campuslife.service.MiniGameService;
import vn.campuslife.service.ReminderScheduleService;
import vn.campuslife.service.SemesterHelperService;
import vn.campuslife.service.ScoreRuleEngine;
import vn.campuslife.util.UrlUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MiniGameServiceImpl implements MiniGameService {

    private static final Logger logger = LoggerFactory.getLogger(MiniGameServiceImpl.class);

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
    private final ActivitySeriesService activitySeriesService;
    private final SemesterHelperService semesterHelperService;
    private final ScoreRuleEngine scoreRuleEngine;
    private final ScoreEntryRepository scoreEntryRepository;
    private final UploadProperties uploadProperties;
    private final DepartmentAuthorizationService departmentAuthorizationService;
    private final ReminderScheduleService reminderScheduleService;

    @Override
    @Transactional
    public Response createMiniGame(CreateMiniGameRequest request) {
        return createMiniGameInternal(request, null);
    }

    @Override
    @Transactional
    public Response createMiniGame(CreateMiniGameRequest request, DepartmentScope scope) {
        return createMiniGameInternal(request, scope);
    }

    private Response createMiniGameInternal(CreateMiniGameRequest request, DepartmentScope scope) {
        try {
            Long activityId = request.getActivityId();
            if (scope != null && scope.manager() && !scope.admin()) {
                departmentAuthorizationService.requireActivityAccess(activityId, scope);
            }
            String title = request.getTitle();
            String description = request.getDescription();
            Integer questionCount = request.getQuestionCount();
            Integer timeLimit = request.getTimeLimit();
            Integer requiredCorrectAnswers = request.getRequiredCorrectAnswers();
            Integer maxAttempts = request.getMaxAttempts();
            Boolean showAnswers = request.getShowAnswers();
            List<CreateMiniGameRequest.QuestionRequest> questions = request.getQuestions();

            Optional<Activity> activityOpt = activityRepository.findById(activityId);
            if (activityOpt.isEmpty()) {
                return Response.error("Activity not found");
            }

            Activity activity = activityOpt.get();
            if (activity.getType() != vn.campuslife.enumeration.ActivityType.MINIGAME) {
                return Response.error("Activity type must be MINIGAME");
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
            miniGame.setMaxAttempts(maxAttempts);
            miniGame.setShowAnswers(Boolean.TRUE.equals(showAnswers));
            MiniGame savedMiniGame = miniGameRepository.save(miniGame);

            // Tạo MiniGameQuiz
            MiniGameQuiz quiz = new MiniGameQuiz();
            quiz.setMiniGame(savedMiniGame);
            MiniGameQuiz savedQuiz = quizRepository.save(quiz);

            // Tạo questions và options
            int order = 0;
            if (questions != null) {
                for (CreateMiniGameRequest.QuestionRequest questionData : questions) {
                    MiniGameQuizQuestion question = new MiniGameQuizQuestion();
                    question.setQuestionText(questionData.getQuestionText());
                    String imageUrl = questionData.getImageUrl();
                    if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                        imageUrl = UrlUtils.toRelativePath(imageUrl, uploadProperties.getPublicUrl());
                    }
                    question.setImageUrl(imageUrl);
                    question.setMiniGameQuiz(savedQuiz);
                    question.setDisplayOrder(order++);
                    MiniGameQuizQuestion savedQuestion = questionRepository.save(question);

                    // Tạo options
                    List<CreateMiniGameRequest.QuestionRequest.OptionRequest> options = questionData.getOptions();
                    if (options != null) {
                        for (CreateMiniGameRequest.QuestionRequest.OptionRequest optionData : options) {
                            MiniGameQuizOption option = new MiniGameQuizOption();
                            option.setText(optionData.getText());
                            option.setCorrect(Boolean.TRUE.equals(optionData.getIsCorrect()));
                            option.setQuestion(savedQuestion);
                            optionRepository.save(option);
                        }
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
        return getMiniGameByActivityInternal(activityId, null);
    }

    @Override
    @Transactional(readOnly = true)
    public Response getMiniGameByActivity(Long activityId, DepartmentScope scope) {
        return getMiniGameByActivityInternal(activityId, scope);
    }

    private Response getMiniGameByActivityInternal(Long activityId, DepartmentScope scope) {
        try {
            if (scope != null && scope.manager() && !scope.admin()) {
                departmentAuthorizationService.requireActivityAccess(activityId, scope);
            }
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
            Optional<MiniGame> miniGameOpt = miniGameRepository.findByIdForUpdate(miniGameId);
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

            Activity activity = miniGame.getActivity();
            String registrationError = validateRegisteredForQuiz(activity, studentId);
            if (registrationError != null) {
                return Response.error(registrationError);
            }

            // Kiểm tra xem có attempt đang làm chưa
            Optional<MiniGameAttempt> inProgressOpt = attemptRepository.findInProgressAttempt(
                    studentId, miniGameId, AttemptStatus.IN_PROGRESS);
            if (inProgressOpt.isPresent()) {
                // Chỉ standalone: làm quiz = ATTENDED (series cần PASS mới tính)
                if (activity.getSeriesId() == null) {
                    markAttendedForQuizAttempt(activity, studentId);
                }
                StartAttemptResponse response = StartAttemptResponse.fromEntity(inProgressOpt.get());
                return Response.success("Resuming existing attempt", response);
            }

            // Kiểm tra maxAttempts cho attempt mới (dưới khóa MiniGame)
            if (miniGame.getMaxAttempts() != null) {
                List<MiniGameAttempt> allAttempts = attemptRepository.findByStudentIdAndMiniGameId(studentId,
                        miniGameId);
                int totalAttempts = allAttempts.size();
                if (totalAttempts >= miniGame.getMaxAttempts()) {
                    return Response.error("Bạn đã đạt số lần làm quiz tối đa (" + miniGame.getMaxAttempts() + " lần)");
                }
            }

            // Tạo attempt mới
            MiniGameAttempt attempt = new MiniGameAttempt();
            attempt.setMiniGame(miniGame);
            attempt.setStudent(studentOpt.get());
            attempt.setCorrectCount(0);
            attempt.setStatus(AttemptStatus.IN_PROGRESS);
            attempt.setStartedAt(LocalDateTime.now());
            MiniGameAttempt savedAttempt = attemptRepository.save(attempt);

            // Standalone minigame: làm quiz = đã tham dự (tránh no-show). Series: chỉ PASS mới tính milestone.
            if (activity.getSeriesId() == null) {
                markAttendedForQuizAttempt(activity, studentId);
            }

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

            Activity activity = attempt.getMiniGame().getActivity();
            String registrationError = validateRegisteredForQuiz(activity, studentId);
            if (registrationError != null) {
                return Response.error(registrationError);
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

            // Standalone: làm quiz (pass/fail) = ATTENDED. Series: chỉ PASS mới cập nhật milestone.
            if (activity.getSeriesId() == null) {
                markAttendedForQuizAttempt(activity, studentId);
            }

            // Tính điểm và tạo ActivityParticipation nếu đạt
            Object participation = null;
            if (attempt.getStatus() == AttemptStatus.PASSED) {
                // Đạt: Cộng điểm từ rewardPoints (standalone) hoặc series progress (series)
                calculateScoreAndCreateParticipation(attemptId);
                // Tìm participation vừa tạo
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
            if (attempt.getStatus() == AttemptStatus.FAILED) {
                applyExhaustedAttemptPenaltyIfNeeded(attempt);
            }

            logger.info("Submitted attempt {} with {} correct answers", attemptId, correctCount);
            SubmitAttemptResponse response = SubmitAttemptResponse.fromEntity(
                    attempt,
                    participation,
                    resolvePointsEarned(attempt));
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
                    .map(attempt -> MiniGameAttemptResponse.fromEntity(attempt, resolvePointsEarned(attempt)))
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

            // Bắt buộc đã đăng ký sự kiện (standalone) hoặc đăng ký series (nếu thuộc series)
            Optional<ActivityRegistration> registrationOpt = registrationRepository
                    .findByActivityIdAndStudentId(activity.getId(), student.getId());
            if (registrationOpt.isEmpty()) {
                return Response.error(registrationRequiredMessage(activity));
            }
            ActivityRegistration registration = registrationOpt.get();
            if (!isEligibleQuizRegistrationStatus(registration.getStatus())) {
                return Response.error(registrationRequiredMessage(activity));
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
            participation.setPointsEarned(BigDecimal.ZERO);

            participation = participationRepository.save(participation);

            // Set registration status to ATTENDED (quiz không có check-in/check-out)
            registration.setStatus(RegistrationStatus.ATTENDED);
            registrationRepository.save(registration);

            // Series: chỉ cập nhật milestone/progress — không cộng điểm rule của activity
            if (activity.getSeriesId() != null) {
                try {
                    activitySeriesService.updateStudentProgress(
                            student.getId(),
                            activity.getId());
                    logger.info("Updated series progress for minigame activity {} in series {}",
                            activity.getName(), activity.getSeriesId());
                } catch (Exception e) {
                    logger.warn("Failed to update series progress: {}", e.getMessage());
                }
            } else {
                scoreRuleEngine.applyMiniGamePassed(attempt, attempt.getStudent().getUser());
            }

            logger.info("Created participation for attempt {} (series: {})",
                    attemptId, activity.getSeriesId() != null);
            return Response.success("Score calculated and participation created", participation);
        } catch (Exception e) {
            logger.error("Failed to calculate score: {}", e.getMessage(), e);
            return Response.error("Failed to calculate score: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Response getQuestions(Long miniGameId, Long studentId) {
        try {
            Optional<MiniGame> miniGameOpt = miniGameRepository.findById(miniGameId);
            if (miniGameOpt.isEmpty()) {
                return Response.error("MiniGame not found");
            }

            MiniGame miniGame = miniGameOpt.get();
            if (!miniGame.isActive()) {
                return Response.error("MiniGame is not active");
            }

            String registrationError = validateRegisteredForQuiz(miniGame.getActivity(), studentId);
            if (registrationError != null) {
                return Response.error(registrationError);
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
            QuizQuestionsResponse response = QuizQuestionsResponse.fromEntities(miniGame, quiz,
                    uploadProperties.getPublicUrl());
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

            BigDecimal pointsEarned = resolvePointsEarned(attempt);

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
                    attempt,
                    quiz,
                    studentAnswers,
                    pointsEarned,
                    uploadProperties.getPublicUrl(),
                    miniGame.isShowAnswers());
            return Response.success("Attempt detail retrieved successfully", response);
        } catch (Exception e) {
            logger.error("Failed to get attempt detail: {}", e.getMessage(), e);
            return Response.error("Failed to get attempt detail: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Response updateMiniGame(Long miniGameId, UpdateMiniGameRequest request) {
        return updateMiniGameInternal(miniGameId, request, null);
    }

    @Override
    @Transactional
    public Response updateMiniGame(Long miniGameId, UpdateMiniGameRequest request, DepartmentScope scope) {
        return updateMiniGameInternal(miniGameId, request, scope);
    }

    private Response updateMiniGameInternal(Long miniGameId, UpdateMiniGameRequest request, DepartmentScope scope) {
        try {
            if (scope != null && scope.manager() && !scope.admin()) {
                departmentAuthorizationService.requireMiniGameAccess(miniGameId, scope);
            }
            Optional<MiniGame> miniGameOpt = miniGameRepository.findById(miniGameId);
            if (miniGameOpt.isEmpty()) {
                return Response.error("MiniGame not found");
            }

            MiniGame miniGame = miniGameOpt.get();

            // Cập nhật thông tin cơ bản
            if (request.getTitle() != null)
                miniGame.setTitle(request.getTitle());
            if (request.getDescription() != null)
                miniGame.setDescription(request.getDescription());
            if (request.getQuestionCount() != null)
                miniGame.setQuestionCount(request.getQuestionCount());
            if (request.getTimeLimit() != null)
                miniGame.setTimeLimit(request.getTimeLimit());
            if (request.getRequiredCorrectAnswers() != null)
                miniGame.setRequiredCorrectAnswers(request.getRequiredCorrectAnswers());
            if (request.getMaxAttempts() != null)
                miniGame.setMaxAttempts(request.getMaxAttempts());
            if (request.getShowAnswers() != null)
                miniGame.setShowAnswers(request.getShowAnswers());

            // Nếu có questions mới, xóa cũ và tạo mới
            List<CreateMiniGameRequest.QuestionRequest> questions = request.getQuestions();
            if (questions != null && !questions.isEmpty()) {
                // Lấy quiz hiện tại
                Optional<MiniGameQuiz> quizOpt = quizRepository.findByMiniGameId(miniGameId);
                if (quizOpt.isPresent()) {
                    MiniGameQuiz quiz = quizOpt.get();
                    // Xóa tất cả answers liên quan tới quiz này để tránh lỗi FK khi xóa options
                    answerRepository.deleteByQuizId(quiz.getId());
                    // Xóa tất cả questions và options cũ
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
                for (CreateMiniGameRequest.QuestionRequest questionData : questions) {
                    MiniGameQuizQuestion question = new MiniGameQuizQuestion();
                    question.setQuestionText(questionData.getQuestionText());
                    String imageUrl = questionData.getImageUrl();
                    if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                        imageUrl = UrlUtils.toRelativePath(imageUrl, uploadProperties.getPublicUrl());
                    }
                    question.setImageUrl(imageUrl);
                    question.setMiniGameQuiz(quiz);
                    question.setDisplayOrder(order++);
                    MiniGameQuizQuestion savedQuestion = questionRepository.save(question);

                    List<CreateMiniGameRequest.QuestionRequest.OptionRequest> options = questionData.getOptions();
                    if (options != null) {
                        for (CreateMiniGameRequest.QuestionRequest.OptionRequest optionData : options) {
                            MiniGameQuizOption option = new MiniGameQuizOption();
                            option.setText(optionData.getText());
                            option.setCorrect(Boolean.TRUE.equals(optionData.getIsCorrect()));
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
        return deleteMiniGameInternal(miniGameId, null);
    }

    @Override
    @Transactional
    public Response deleteMiniGame(Long miniGameId, DepartmentScope scope) {
        return deleteMiniGameInternal(miniGameId, scope);
    }

    private Response deleteMiniGameInternal(Long miniGameId, DepartmentScope scope) {
        try {
            if (scope != null && scope.manager() && !scope.admin()) {
                departmentAuthorizationService.requireMiniGameAccess(miniGameId, scope);
            }
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
        return getAllMiniGamesInternal(null);
    }

    @Override
    @Transactional(readOnly = true)
    public Response getAllMiniGames(DepartmentScope scope) {
        return getAllMiniGamesInternal(scope);
    }

    private Response getAllMiniGamesInternal(DepartmentScope scope) {
        try {
            List<MiniGame> miniGames = scope != null && scope.manager() && !scope.admin()
                    ? miniGameRepository.findAll(DepartmentScopeSpec.miniGame(scope.departmentIds()))
                    : miniGameRepository.findAll();
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
        return checkActivityHasQuizInternal(activityId, null);
    }

    @Override
    @Transactional(readOnly = true)
    public Response checkActivityHasQuiz(Long activityId, DepartmentScope scope) {
        return checkActivityHasQuizInternal(activityId, scope);
    }

    private Response checkActivityHasQuizInternal(Long activityId, DepartmentScope scope) {
        try {
            if (scope != null && scope.manager() && !scope.admin()) {
                departmentAuthorizationService.requireActivityAccess(activityId, scope);
            }
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
        return getQuestionsForEditInternal(miniGameId, null);
    }

    @Override
    @Transactional(readOnly = true)
    public Response getQuestionsForEdit(Long miniGameId, DepartmentScope scope) {
        return getQuestionsForEditInternal(miniGameId, scope);
    }

    private Response getQuestionsForEditInternal(Long miniGameId, DepartmentScope scope) {
        try {
            if (scope != null && scope.manager() && !scope.admin()) {
                departmentAuthorizationService.requireMiniGameAccess(miniGameId, scope);
            }
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
                    uploadProperties.getPublicUrl());

            return Response.success("Questions retrieved successfully for edit", response);
        } catch (Exception e) {
            logger.error("Failed to get questions for edit: {}", e.getMessage(), e);
            return Response.error("Failed to get questions for edit: " + e.getMessage());
        }
    }

    private BigDecimal resolvePointsEarned(MiniGameAttempt attempt) {
        if (attempt == null || attempt.getStatus() == AttemptStatus.IN_PROGRESS) {
            return BigDecimal.ZERO;
        }

        return scoreEntryRepository.findBySourceTypeAndSourceIdAndStatus(
                ScoreEntrySourceType.MINIGAME_ATTEMPT,
                attempt.getId(),
                ScoreEntryStatus.ACTIVE)
                .stream()
                .map(ScoreEntry::getPoints)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void applyExhaustedAttemptPenaltyIfNeeded(MiniGameAttempt attempt) {
        if (attempt == null || attempt.getStatus() != AttemptStatus.FAILED || attempt.getMiniGame() == null) {
            return;
        }

        MiniGame miniGame = attempt.getMiniGame();
        Activity activity = miniGame.getActivity();
        if (activity == null || activity.getSeriesId() != null) {
            return;
        }

        Integer maxAttempts = miniGame.getMaxAttempts();
        if (maxAttempts == null || maxAttempts <= 0) {
            return;
        }

        Long studentId = attempt.getStudent() != null ? attempt.getStudent().getId() : null;
        Long miniGameId = miniGame.getId();
        if (studentId == null || miniGameId == null) {
            return;
        }

        // Already passed once → keep pass result; later failed/exhausted attempts must not penalize.
        if (attemptRepository.existsByStudentIdAndMiniGameIdAndStatus(
                studentId, miniGameId, AttemptStatus.PASSED)) {
            logger.info(
                    "Skip exhausted penalty for student {} on minigame {} — already passed previously",
                    studentId, miniGameId);
            return;
        }

        // Participation COMPLETED is another "already passed" signal (idempotent pass path).
        Optional<ActivityRegistration> registrationOpt = registrationRepository
                .findByActivityIdAndStudentId(activity.getId(), studentId);
        if (registrationOpt.isPresent()) {
            Optional<ActivityParticipation> participationOpt = participationRepository
                    .findByRegistration(registrationOpt.get());
            if (participationOpt.isPresent()) {
                ActivityParticipation participation = participationOpt.get();
                if (participation.getParticipationType() == ParticipationType.COMPLETED
                        && Boolean.TRUE.equals(participation.getIsCompleted())) {
                    logger.info(
                            "Skip exhausted penalty for student {} on activity {} — participation already completed",
                            studentId, activity.getId());
                    return;
                }
            }
            // ATTENDED chỉ nghĩa là đã làm quiz (tránh no-show), vẫn có thể phạt hết lượt nếu fail hết attempts
        }

        int totalAttempts = attemptRepository.findByStudentIdAndMiniGameId(studentId, miniGameId).size();
        if (totalAttempts < maxAttempts) {
            return;
        }

        scoreRuleEngine.applyMiniGameExhaustedAttempts(attempt, attempt.getStudent().getUser());
    }

    /**
     * Chỉ dùng cho minigame standalone (không thuộc series).
     * Minigame không quét QR: bắt đầu/nộp quiz → ATTENDED để tránh phạt no-show.
     * Series: không gọi method này — chỉ PASS mới COMPLETED + updateStudentProgress (milestone).
     */
    private void markAttendedForQuizAttempt(Activity activity, Long studentId) {
        if (activity == null || studentId == null || activity.getSeriesId() != null) {
            return;
        }
        Optional<ActivityRegistration> registrationOpt = registrationRepository
                .findByActivityIdAndStudentId(activity.getId(), studentId);
        if (registrationOpt.isEmpty()) {
            return;
        }

        ActivityRegistration registration = registrationOpt.get();
        if (registration.getStatus() != RegistrationStatus.APPROVED
                && registration.getStatus() != RegistrationStatus.ATTENDED) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        Optional<ActivityParticipation> participationOpt = participationRepository.findByRegistration(registration);
        if (participationOpt.isEmpty()) {
            ActivityParticipation participation = new ActivityParticipation();
            participation.setRegistration(registration);
            participation.setParticipationType(ParticipationType.ATTENDED);
            participation.setDate(now);
            participation.setCheckInTime(now);
            participation.setIsCompleted(false);
            participation.setPointsEarned(BigDecimal.ZERO);
            participationRepository.save(participation);
        } else {
            ActivityParticipation participation = participationOpt.get();
            // Không hạ COMPLETED (đã pass quiz) về ATTENDED
            if (participation.getParticipationType() != ParticipationType.COMPLETED
                    && !Boolean.TRUE.equals(participation.getIsCompleted())) {
                if (participation.getCheckInTime() == null) {
                    participation.setCheckInTime(now);
                }
                participation.setParticipationType(ParticipationType.ATTENDED);
                participation.setDate(now);
                participationRepository.save(participation);
            }
        }

        if (registration.getStatus() != RegistrationStatus.ATTENDED) {
            registration.setStatus(RegistrationStatus.ATTENDED);
            registrationRepository.save(registration);
            try {
                reminderScheduleService.cancelPendingEventRemindersForRegistration(registration);
            } catch (Exception e) {
                logger.warn("Failed to cancel no-show reminders after quiz attendance: {}", e.getMessage());
            }
            logger.info("Marked registration {} as ATTENDED after quiz attempt on activity {}",
                    registration.getId(), activity.getId());
        }
    }

    /**
     * Quiz chỉ dành cho SV đã đăng ký sự kiện MINIGAME (standalone) hoặc đã đăng ký series
     * (registration được tạo trên từng activity con khi register series).
     */
    private String validateRegisteredForQuiz(Activity activity, Long studentId) {
        if (activity == null || studentId == null) {
            return "Activity not found";
        }
        Optional<ActivityRegistration> registrationOpt = registrationRepository
                .findByActivityIdAndStudentId(activity.getId(), studentId);
        if (registrationOpt.isEmpty() || !isEligibleQuizRegistrationStatus(registrationOpt.get().getStatus())) {
            return registrationRequiredMessage(activity);
        }
        return null;
    }

    private boolean isEligibleQuizRegistrationStatus(RegistrationStatus status) {
        return status == RegistrationStatus.APPROVED || status == RegistrationStatus.ATTENDED;
    }

    private String registrationRequiredMessage(Activity activity) {
        if (activity != null && activity.getSeriesId() != null) {
            return "Bạn phải đăng ký chuỗi sự kiện trước khi làm quiz này";
        }
        return "Bạn phải đăng ký sự kiện trước khi làm quiz này";
    }
}
