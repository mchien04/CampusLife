package vn.campuslife.service.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import vn.campuslife.config.UploadProperties;
import vn.campuslife.entity.Activity;
import vn.campuslife.entity.Department;
import vn.campuslife.entity.MiniGame;
import vn.campuslife.enumeration.ActivityType;
import vn.campuslife.enumeration.MiniGameType;
import vn.campuslife.model.activity.minigame.MinigameActivityCreateRequest;
import vn.campuslife.model.activity.minigame.MinigameActivityResponse;
import vn.campuslife.model.activity.minigame.MinigameActivityUpdateRequest;
import vn.campuslife.service.ActivityScoreRuleService;
import vn.campuslife.util.UrlUtils;

import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class MinigameActivityMapper {

    private final UploadProperties uploadProperties;
    private final ActivityScoreRuleService activityScoreRuleService;

    public Activity toShellEntity(MinigameActivityCreateRequest req) {
        if (req == null) return null;
        Activity entity = new Activity();
        entity.setName(req.getName());
        entity.setType(ActivityType.MINIGAME);
        entity.setDescription(req.getDescription());
        entity.setStartDate(req.getStartDate());
        entity.setEndDate(req.getEndDate());
        
        entity.setRequiresApproval(req.getRequiresApproval() != null && req.getRequiresApproval());
        entity.setTicketQuantity(req.getTicketQuantity());
        entity.setImportant(req.getIsImportant() != null && req.getIsImportant());
        entity.setMandatoryForFacultyStudents(req.getMandatoryForFacultyStudents() != null && req.getMandatoryForFacultyStudents());
        entity.setDraft(req.getIsDraft() != null && req.getIsDraft());
        entity.setRegistrationStartDate(req.getRegistrationStartDate());
        entity.setRegistrationDeadline(req.getRegistrationDeadline());
        
        entity.setBannerUrl(req.getBannerUrl());
        entity.setShareLink(req.getShareLink());
        entity.setPresetCode(req.getPresetCode());

        // Fixed defaults for minigame shell
        entity.setRequiresSubmission(false);
        entity.setLocation(null);
        
        return entity;
    }

    public void applyShellUpdate(Activity entity, MinigameActivityUpdateRequest req) {
        if (req == null || entity == null) return;
        
        if (req.getName() != null) entity.setName(req.getName());
        if (req.getDescription() != null) entity.setDescription(req.getDescription());
        if (req.getStartDate() != null) entity.setStartDate(req.getStartDate());
        if (req.getEndDate() != null) entity.setEndDate(req.getEndDate());
        
        if (req.getRequiresApproval() != null) entity.setRequiresApproval(req.getRequiresApproval());
        if (req.getTicketQuantity() != null) entity.setTicketQuantity(req.getTicketQuantity());
        if (req.getIsImportant() != null) entity.setImportant(req.getIsImportant());
        if (req.getMandatoryForFacultyStudents() != null) entity.setMandatoryForFacultyStudents(req.getMandatoryForFacultyStudents());
        if (req.getIsDraft() != null) entity.setDraft(req.getIsDraft());
        if (req.getRegistrationStartDate() != null) entity.setRegistrationStartDate(req.getRegistrationStartDate());
        if (req.getRegistrationDeadline() != null) entity.setRegistrationDeadline(req.getRegistrationDeadline());
        
        if (req.getBannerUrl() != null) entity.setBannerUrl(req.getBannerUrl());
        if (req.getShareLink() != null) entity.setShareLink(req.getShareLink());
    }

    public MiniGame toMiniGameEntity(MinigameActivityCreateRequest.QuizConfigRequest quizReq, Activity shell) {
        if (quizReq == null) return null;
        MiniGame miniGame = new MiniGame();
        miniGame.setActivity(shell);
        miniGame.setTitle(quizReq.getTitle());
        miniGame.setDescription(quizReq.getDescription());
        miniGame.setQuestionCount(quizReq.getQuestionCount());
        miniGame.setTimeLimit(quizReq.getTimeLimit());
        miniGame.setRequiredCorrectAnswers(quizReq.getRequiredCorrectAnswers());
        miniGame.setMaxAttempts(quizReq.getMaxAttempts());
        miniGame.setShowAnswers(quizReq.getShowAnswers() != null && quizReq.getShowAnswers());
        miniGame.setType(MiniGameType.QUIZ); // Defaulting to QUIZ
        miniGame.setActive(true);
        return miniGame;
    }

    public void applyMiniGameUpdate(MiniGame miniGame, MinigameActivityUpdateRequest.QuizConfigRequest quizReq) {
        if (quizReq == null || miniGame == null) return;
        if (quizReq.getTitle() != null) miniGame.setTitle(quizReq.getTitle());
        if (quizReq.getDescription() != null) miniGame.setDescription(quizReq.getDescription());
        if (quizReq.getQuestionCount() != null) miniGame.setQuestionCount(quizReq.getQuestionCount());
        if (quizReq.getTimeLimit() != null) miniGame.setTimeLimit(quizReq.getTimeLimit());
        if (quizReq.getRequiredCorrectAnswers() != null) miniGame.setRequiredCorrectAnswers(quizReq.getRequiredCorrectAnswers());
        if (quizReq.getMaxAttempts() != null) miniGame.setMaxAttempts(quizReq.getMaxAttempts());
        if (quizReq.getShowAnswers() != null) miniGame.setShowAnswers(quizReq.getShowAnswers());
    }

    public MinigameActivityResponse toResponse(Activity shell, MiniGame miniGame) {
        if (shell == null) return null;
        
        MinigameActivityResponse dto = new MinigameActivityResponse();
        dto.setId(shell.getId());
        dto.setName(shell.getName());
        dto.setType(ActivityType.MINIGAME);
        dto.setDescription(shell.getDescription());
        dto.setStartDate(shell.getStartDate());
        dto.setEndDate(shell.getEndDate());
        dto.setIsDraft(shell.isDraft());
        dto.setBannerUrl(UrlUtils.toFullUrl(shell.getBannerUrl(), uploadProperties.getPublicUrl()));
        dto.setShareLink(shell.getShareLink());
        dto.setIsImportant(shell.isImportant());
        dto.setCheckInCode(shell.getCheckInCode());
        dto.setPresetCode(shell.getPresetCode());
        dto.setCreatedAt(shell.getCreatedAt());
        dto.setUpdatedAt(shell.getUpdatedAt());
        
        if (shell.getId() != null) {
            dto.setScoreRules(activityScoreRuleService.getRuleResponses(shell.getId()));
        }
        
        if (miniGame != null) {
            MinigameActivityResponse.QuizConfigResponse quizResp = new MinigameActivityResponse.QuizConfigResponse();
            quizResp.setId(miniGame.getId());
            quizResp.setTitle(miniGame.getTitle());
            quizResp.setDescription(miniGame.getDescription());
            quizResp.setQuestionCount(miniGame.getQuestionCount());
            quizResp.setTimeLimit(miniGame.getTimeLimit());
            quizResp.setRequiredCorrectAnswers(miniGame.getRequiredCorrectAnswers());
            quizResp.setMaxAttempts(miniGame.getMaxAttempts());
            quizResp.setShowAnswers(miniGame.isShowAnswers());
            quizResp.setIsActive(miniGame.isActive());
            dto.setQuiz(quizResp);
        }
        
        return dto;
    }
}
