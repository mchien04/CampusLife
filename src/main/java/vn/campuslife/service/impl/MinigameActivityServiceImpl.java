package vn.campuslife.service.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.campuslife.config.UploadProperties;
import vn.campuslife.entity.Activity;
import vn.campuslife.entity.Department;
import vn.campuslife.entity.MiniGame;
import vn.campuslife.entity.MiniGameQuiz;
import vn.campuslife.entity.MiniGameQuizOption;
import vn.campuslife.entity.MiniGameQuizQuestion;
import vn.campuslife.model.Response;
import vn.campuslife.model.activity.minigame.MinigameActivityCreateRequest;
import vn.campuslife.model.activity.minigame.MinigameActivityUpdateRequest;
import vn.campuslife.model.activity.quiz.CreateMiniGameRequest;
import vn.campuslife.repository.ActivityRepository;
import vn.campuslife.repository.DepartmentRepository;
import vn.campuslife.repository.MiniGameAnswerRepository;
import vn.campuslife.repository.MiniGameQuizOptionRepository;
import vn.campuslife.repository.MiniGameQuizQuestionRepository;
import vn.campuslife.repository.MiniGameQuizRepository;
import vn.campuslife.repository.MiniGameRepository;
import vn.campuslife.service.ActivityRegistrationAutoService;
import vn.campuslife.service.ActivityScoreRuleService;
import vn.campuslife.service.MinigameActivityService;
import vn.campuslife.service.ReminderScheduleService;
import vn.campuslife.service.mapper.MinigameActivityMapper;
import vn.campuslife.service.validator.MinigameActivityValidator;
import vn.campuslife.util.UrlUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MinigameActivityServiceImpl implements MinigameActivityService {

    private static final Logger logger = LoggerFactory.getLogger(MinigameActivityServiceImpl.class);

    private final ActivityRepository activityRepository;
    private final MiniGameRepository miniGameRepository;
    private final DepartmentRepository departmentRepository;
    private final MiniGameQuizRepository quizRepository;
    private final MiniGameQuizQuestionRepository questionRepository;
    private final MiniGameQuizOptionRepository optionRepository;
    private final MiniGameAnswerRepository answerRepository;
    private final ActivityScoreRuleService activityScoreRuleService;
    private final ActivityRegistrationAutoService autoRegisterService;
    private final ReminderScheduleService reminderScheduleService;
    private final MinigameActivityValidator validator;
    private final MinigameActivityMapper mapper;
    private final UploadProperties uploadProperties;

    @Override
    @Transactional
    public Response createMinigame(MinigameActivityCreateRequest request) {
        try {
            validator.validate(request);

            Set<Department> organizers = resolveOrganizers(request.getOrganizerIds());

            Activity shell = mapper.toShellEntity(request);
            shell.setOrganizers(organizers);

            Activity savedShell = activityRepository.save(shell);

            // Generate checkInCode
            if (savedShell.getCheckInCode() == null || savedShell.getCheckInCode().isBlank()) {
                String random = UUID.randomUUID().toString().substring(0, 8).toUpperCase().replace("-", "");
                String checkInCode = String.format("ACT-%06d-%s", savedShell.getId(), random);
                savedShell.setCheckInCode(checkInCode);
                savedShell = activityRepository.save(savedShell);
            }

            if (request.getScoreRules() != null && !request.getScoreRules().isEmpty()) {
                activityScoreRuleService.replaceRules(savedShell.getId(), request.getScoreRules());
            }

            autoRegisterService.autoRegisterStudents(savedShell);
            reminderScheduleService.syncEventRemindersForActivity(savedShell);

            MiniGame miniGame = mapper.toMiniGameEntity(request.getQuiz(), savedShell);
            miniGame = miniGameRepository.save(miniGame);

            // Persist quiz, questions, and options
            persistQuizQuestionsAndOptions(miniGame, request.getQuiz());

            return Response.success("Minigame created successfully", mapper.toResponse(savedShell, miniGame));
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid minigame create request: {}", e.getMessage());
            return Response.error(e.getMessage());
        } catch (Exception e) {
            logger.error("Failed to create minigame activity: {}", e.getMessage(), e);
            return Response.error("Failed to create minigame due to server error");
        }
    }

    @Override
    @Transactional
    public Response updateMinigame(Long id, MinigameActivityUpdateRequest request) {
        try {
            Optional<Activity> opt = activityRepository.findByIdAndIsDeletedFalse(id);
            if (opt.isEmpty()) {
                return Response.error("Activity not found");
            }
            Activity shell = opt.get();

            mapper.applyShellUpdate(shell, request);

            if (request.getOrganizerIds() != null && !request.getOrganizerIds().isEmpty()) {
                shell.setOrganizers(resolveOrganizers(request.getOrganizerIds()));
            }

            Activity savedShell = activityRepository.save(shell);

            if (request.getScoreRules() != null) {
                activityScoreRuleService.replaceRules(savedShell.getId(), request.getScoreRules());
            }

            autoRegisterService.autoRegisterStudents(savedShell);

            MiniGame miniGame = null;
            if (request.getQuiz() != null) {
                Optional<MiniGame> miniGameOpt = miniGameRepository.findByActivityId(shell.getId());
                if (miniGameOpt.isPresent()) {
                    miniGame = miniGameOpt.get();
                    mapper.applyMiniGameUpdate(miniGame, request.getQuiz());
                    miniGame = miniGameRepository.save(miniGame);

                    // Rebuild quiz questions/options if provided
                    if (request.getQuiz().getQuestions() != null && !request.getQuiz().getQuestions().isEmpty()) {
                        rebuildQuizQuestionsAndOptions(miniGame, request.getQuiz());
                    }
                } else {
                    miniGame = mapper.toMiniGameEntity(new MinigameActivityCreateRequest.QuizConfigRequest(
                            request.getQuiz().getTitle(), request.getQuiz().getQuestionCount(),
                            request.getQuiz().getTimeLimit(), request.getQuiz().getRequiredCorrectAnswers(),
                            request.getQuiz().getMaxAttempts(), request.getQuiz().getShowAnswers(),
                            request.getQuiz().getQuestions()), savedShell);
                    miniGame = miniGameRepository.save(miniGame);

                    // Persist quiz, questions, and options
                    persistQuizQuestionsAndOptions(miniGame, request.getQuiz());
                }
            } else {
                miniGame = miniGameRepository.findByActivityId(shell.getId()).orElse(null);
            }

            return Response.success("Minigame updated successfully", mapper.toResponse(savedShell, miniGame));
        } catch (IllegalArgumentException e) {
            return Response.error(e.getMessage());
        } catch (Exception e) {
            logger.error("Failed to update minigame activity: {}", e.getMessage(), e);
            return Response.error("Failed to update minigame");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Response getMinigame(Long id) {
        Optional<Activity> opt = activityRepository.findByIdAndIsDeletedFalse(id);
        if (opt.isEmpty()) {
            return Response.error("Activity not found");
        }
        Activity shell = opt.get();
        MiniGame miniGame = miniGameRepository.findByActivityId(shell.getId()).orElse(null);
        
        return Response.success("Minigame retrieved successfully", mapper.toResponse(shell, miniGame));
    }

    private Set<Department> resolveOrganizers(List<Long> organizerIds) {
        if (organizerIds == null || organizerIds.isEmpty()) return new LinkedHashSet<>();
        var deps = departmentRepository.findAllById(organizerIds);
        var found = deps.stream().map(Department::getId).collect(Collectors.toSet());
        var missing = organizerIds.stream().filter(id -> !found.contains(id)).collect(Collectors.toList());
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException("Department ids not found: " + missing);
        }
        return new LinkedHashSet<>(deps);
    }

    /**
     * Create MiniGameQuiz + questions + options for a newly created MiniGame.
     */
    private void persistQuizQuestionsAndOptions(MiniGame miniGame, MinigameActivityCreateRequest.QuizConfigRequest quizReq) {
        if (quizReq == null || quizReq.getQuestions() == null || quizReq.getQuestions().isEmpty()) {
            return;
        }

        MiniGameQuiz quiz = new MiniGameQuiz();
        quiz.setMiniGame(miniGame);
        quiz = quizRepository.save(quiz);

        createQuestionsAndOptions(quiz, quizReq.getQuestions());
    }

    /**
     * Create MiniGameQuiz + questions + options for a newly created MiniGame (update path).
     */
    private void persistQuizQuestionsAndOptions(MiniGame miniGame, MinigameActivityUpdateRequest.QuizConfigRequest quizReq) {
        if (quizReq == null || quizReq.getQuestions() == null || quizReq.getQuestions().isEmpty()) {
            return;
        }

        MiniGameQuiz quiz = new MiniGameQuiz();
        quiz.setMiniGame(miniGame);
        quiz = quizRepository.save(quiz);

        createQuestionsAndOptions(quiz, quizReq.getQuestions());
    }

    /**
     * Rebuild quiz questions/options for an existing MiniGame (update path).
     * Deletes old answers, questions, options and recreates them.
     */
    private void rebuildQuizQuestionsAndOptions(MiniGame miniGame, MinigameActivityUpdateRequest.QuizConfigRequest quizReq) {
        Optional<MiniGameQuiz> quizOpt = quizRepository.findByMiniGameId(miniGame.getId());
        MiniGameQuiz quiz;

        if (quizOpt.isPresent()) {
            quiz = quizOpt.get();
            // Delete old answers linked to this quiz to avoid FK constraint errors
            answerRepository.deleteByQuizId(quiz.getId());
            // Delete old questions (cascade removes options)
            questionRepository.deleteAll(quiz.getQuestions());
            quiz.getQuestions().clear();
        } else {
            quiz = new MiniGameQuiz();
            quiz.setMiniGame(miniGame);
            quiz = quizRepository.save(quiz);
        }

        createQuestionsAndOptions(quiz, quizReq.getQuestions());
    }

    /**
     * Shared logic: create questions and options for a quiz.
     */
    private void createQuestionsAndOptions(MiniGameQuiz quiz, List<CreateMiniGameRequest.QuestionRequest> questions) {
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
}
