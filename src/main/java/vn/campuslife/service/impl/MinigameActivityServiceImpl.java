package vn.campuslife.service.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.campuslife.entity.Activity;
import vn.campuslife.entity.Department;
import vn.campuslife.entity.MiniGame;
import vn.campuslife.model.Response;
import vn.campuslife.model.activity.minigame.MinigameActivityCreateRequest;
import vn.campuslife.model.activity.minigame.MinigameActivityUpdateRequest;
import vn.campuslife.repository.ActivityRepository;
import vn.campuslife.repository.DepartmentRepository;
import vn.campuslife.repository.MiniGameRepository;
import vn.campuslife.service.ActivityScoreRuleService;
import vn.campuslife.service.MinigameActivityService;
import vn.campuslife.service.mapper.MinigameActivityMapper;
import vn.campuslife.service.validator.MinigameActivityValidator;

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
    private final ActivityScoreRuleService activityScoreRuleService;
    private final MinigameActivityValidator validator;
    private final MinigameActivityMapper mapper;

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

            MiniGame miniGame = mapper.toMiniGameEntity(request.getQuiz(), savedShell);
            miniGame = miniGameRepository.save(miniGame);

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

            MiniGame miniGame = null;
            if (request.getQuiz() != null) {
                Optional<MiniGame> miniGameOpt = miniGameRepository.findByActivityId(shell.getId());
                if (miniGameOpt.isPresent()) {
                    miniGame = miniGameOpt.get();
                    mapper.applyMiniGameUpdate(miniGame, request.getQuiz());
                    miniGame = miniGameRepository.save(miniGame);
                } else {
                    // It shouldn't happen for a valid MINIGAME type but just in case
                    miniGame = mapper.toMiniGameEntity(new MinigameActivityCreateRequest.QuizConfigRequest(
                            request.getQuiz().getTitle(), request.getQuiz().getQuestionCount(),
                            request.getQuiz().getTimeLimit(), request.getQuiz().getRequiredCorrectAnswers(),
                            request.getQuiz().getMaxAttempts(), request.getQuiz().getShowAnswers(),
                            request.getQuiz().getQuestions()), savedShell);
                    miniGame = miniGameRepository.save(miniGame);
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
}
