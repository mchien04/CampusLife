package vn.campuslife.service.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import vn.campuslife.config.UploadProperties;
import vn.campuslife.entity.Activity;
import vn.campuslife.entity.Department;
import vn.campuslife.model.activity.StandardActivityCreateRequest;
import vn.campuslife.model.activity.StandardActivityResponse;
import vn.campuslife.model.activity.StandardActivityUpdateRequest;
import vn.campuslife.service.ActivityScoreRuleService;
import vn.campuslife.util.UrlUtils;

import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class StandardActivityMapper {

    private final UploadProperties uploadProperties;
    private final ActivityScoreRuleService activityScoreRuleService;

    public Activity toEntity(StandardActivityCreateRequest req) {
        if (req == null) return null;
        Activity entity = new Activity();
        entity.setName(req.getName());
        entity.setType(req.getType());
        entity.setDescription(req.getDescription());
        entity.setStartDate(req.getStartDate());
        entity.setEndDate(req.getEndDate());
        entity.setLocation(req.getLocation());
        
        entity.setRegistrationStartDate(req.getRegistrationStartDate());
        entity.setRegistrationDeadline(req.getRegistrationDeadline());
        
        entity.setRequiresSubmission(req.getRequiresSubmission() != null && req.getRequiresSubmission());
        entity.setRequiresApproval(req.getRequiresApproval() != null && req.getRequiresApproval());
        entity.setTicketQuantity(req.getTicketQuantity());
        entity.setImportant(req.getIsImportant() != null && req.getIsImportant());
        entity.setMandatoryForFacultyStudents(req.getMandatoryForFacultyStudents() != null && req.getMandatoryForFacultyStudents());
        entity.setDraft(req.getIsDraft() != null && req.getIsDraft());
        
        entity.setBannerUrl(req.getBannerUrl());
        entity.setShareLink(req.getShareLink());
        entity.setBenefits(req.getBenefits());
        entity.setRequirements(req.getRequirements());
        entity.setContactInfo(req.getContactInfo());
        
        // Organizers and ScoreRules are typically mapped and linked in the service layer
        // because they require database lookups (DepartmentRepository)
        entity.setPresetCode(req.getPresetCode());
        return entity;
    }

    public void applyUpdate(Activity entity, StandardActivityUpdateRequest req) {
        if (req == null || entity == null) return;
        
        if (req.getName() != null) entity.setName(req.getName());
        if (req.getType() != null) entity.setType(req.getType());
        if (req.getDescription() != null) entity.setDescription(req.getDescription());
        if (req.getStartDate() != null) entity.setStartDate(req.getStartDate());
        if (req.getEndDate() != null) entity.setEndDate(req.getEndDate());
        if (req.getLocation() != null) entity.setLocation(req.getLocation());
        if (req.getRegistrationStartDate() != null) entity.setRegistrationStartDate(req.getRegistrationStartDate());
        if (req.getRegistrationDeadline() != null) entity.setRegistrationDeadline(req.getRegistrationDeadline());
        if (req.getRequiresSubmission() != null) entity.setRequiresSubmission(req.getRequiresSubmission());
        if (req.getRequiresApproval() != null) entity.setRequiresApproval(req.getRequiresApproval());
        if (req.getTicketQuantity() != null) entity.setTicketQuantity(req.getTicketQuantity() == 0 ? null : req.getTicketQuantity());
        if (req.getIsImportant() != null) entity.setImportant(req.getIsImportant());
        if (req.getMandatoryForFacultyStudents() != null) entity.setMandatoryForFacultyStudents(req.getMandatoryForFacultyStudents());
        if (req.getIsDraft() != null) entity.setDraft(req.getIsDraft());
        if (req.getBannerUrl() != null) entity.setBannerUrl(req.getBannerUrl());
        if (req.getShareLink() != null) entity.setShareLink(req.getShareLink());
        if (req.getBenefits() != null) entity.setBenefits(req.getBenefits());
        if (req.getRequirements() != null) entity.setRequirements(req.getRequirements());
        if (req.getContactInfo() != null) entity.setContactInfo(req.getContactInfo());
        if (req.getPresetCode() != null) entity.setPresetCode(req.getPresetCode());
    }

    public StandardActivityResponse toResponse(Activity a) {
        if (a == null) return null;
        
        StandardActivityResponse dto = new StandardActivityResponse();
        dto.setId(a.getId());
        dto.setName(a.getName());
        dto.setType(a.getType());
        dto.setDescription(a.getDescription());
        dto.setStartDate(a.getStartDate());
        dto.setEndDate(a.getEndDate());
        dto.setLocation(a.getLocation());
        
        dto.setRegistrationStartDate(a.getRegistrationStartDate());
        dto.setRegistrationDeadline(a.getRegistrationDeadline());
        
        dto.setHasPreparation(a.isHasPreparation());
        dto.setRequiresSubmission(a.isRequiresSubmission());
        dto.setRequiresApproval(a.isRequiresApproval());
        dto.setTicketQuantity(a.getTicketQuantity());

        dto.setMandatoryForFacultyStudents(a.isMandatoryForFacultyStudents());

        
        dto.setBannerUrl(UrlUtils.toFullUrl(a.getBannerUrl(), uploadProperties.getPublicUrl()));
        dto.setShareLink(a.getShareLink());
        dto.setBenefits(a.getBenefits());
        dto.setRequirements(a.getRequirements());
        dto.setContactInfo(a.getContactInfo());
        dto.setCheckInCode(a.getCheckInCode());
        
        if (a.getOrganizers() != null) {
            dto.setOrganizerIds(a.getOrganizers().stream().map(Department::getId).collect(Collectors.toList()));
        }
        
        if (a.getId() != null) {
            dto.setScoreRules(activityScoreRuleService.getRuleResponses(a.getId()));
        }
        
        dto.setCreatedAt(a.getCreatedAt());
        dto.setUpdatedAt(a.getUpdatedAt());
        dto.setCreatedBy(a.getCreatedBy());
        dto.setLastModifiedBy(a.getLastModifiedBy());

        dto.setPresetCode(a.getPresetCode());
        dto.setPresetConfig(null);
        dto.setActiveScoreEntryCount(activityScoreRuleService.countActiveEntries(a.getId()));

        return dto;
    }
}
