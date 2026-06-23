package vn.campuslife.service.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import vn.campuslife.config.UploadProperties;
import vn.campuslife.entity.Activity;
import vn.campuslife.entity.ActivitySeries;
import vn.campuslife.entity.Department;
import vn.campuslife.model.activity.series.SeriesChildActivityCreateRequest;
import vn.campuslife.model.activity.series.SeriesChildActivityResponse;
import vn.campuslife.model.activity.series.SeriesChildActivityUpdateRequest;
import vn.campuslife.service.ActivityScoreRuleService;
import vn.campuslife.util.UrlUtils;

import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SeriesChildActivityMapper {

    private final UploadProperties uploadProperties;
    private final ActivityScoreRuleService activityScoreRuleService;

    public Activity toEntity(SeriesChildActivityCreateRequest req, ActivitySeries series) {
        if (req == null) return null;
        Activity entity = new Activity();
        entity.setName(req.getName());
        entity.setDescription(req.getDescription());
        entity.setStartDate(req.getStartDate());
        entity.setEndDate(req.getEndDate());
        entity.setLocation(req.getLocation());
        entity.setSeriesOrder(req.getOrder());
        entity.setBannerUrl(req.getBannerUrl());
        entity.setShareLink(req.getShareLink());
        entity.setBenefits(req.getBenefits());
        entity.setRequirements(req.getRequirements());
        entity.setContactInfo(req.getContactInfo());
        entity.setType(req.getType()); // Optional
        
        // Inherited from series
        if (series != null) {
            entity.setSeriesId(series.getId());
            entity.setRegistrationStartDate(series.getRegistrationStartDate());
            entity.setRegistrationDeadline(series.getRegistrationDeadline());
            entity.setRequiresApproval(series.isRequiresApproval());
            entity.setTicketQuantity(series.getTicketQuantity());
        }
        
        entity.setRequiresSubmission(false);
        entity.setImportant(false);
        entity.setMandatoryForFacultyStudents(false);
        entity.setDraft(false); // Series children are auto-published
        
        return entity;
    }

    public void applyUpdate(Activity entity, SeriesChildActivityUpdateRequest req) {
        if (req == null || entity == null) return;
        
        if (req.getName() != null) entity.setName(req.getName());
        if (req.getDescription() != null) entity.setDescription(req.getDescription());
        if (req.getStartDate() != null) entity.setStartDate(req.getStartDate());
        if (req.getEndDate() != null) entity.setEndDate(req.getEndDate());
        if (req.getLocation() != null) entity.setLocation(req.getLocation());
        if (req.getOrder() != null) entity.setSeriesOrder(req.getOrder());
        if (req.getBannerUrl() != null) entity.setBannerUrl(req.getBannerUrl());
        if (req.getShareLink() != null) entity.setShareLink(req.getShareLink());
        if (req.getBenefits() != null) entity.setBenefits(req.getBenefits());
        if (req.getRequirements() != null) entity.setRequirements(req.getRequirements());
        if (req.getContactInfo() != null) entity.setContactInfo(req.getContactInfo());
        if (req.getType() != null) entity.setType(req.getType());
    }

    public SeriesChildActivityResponse toResponse(Activity a, String seriesName) {
        if (a == null) return null;
        
        SeriesChildActivityResponse dto = new SeriesChildActivityResponse();
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
        dto.setImportant(a.isImportant());
        dto.setMandatoryForFacultyStudents(a.isMandatoryForFacultyStudents());
        dto.setDraft(a.isDraft());
        
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
        
        dto.setSeriesId(a.getSeriesId());
        dto.setSeriesOrder(a.getSeriesOrder());
        dto.setSeriesName(seriesName);
        
        dto.setCreatedAt(a.getCreatedAt());
        dto.setUpdatedAt(a.getUpdatedAt());
        dto.setCreatedBy(a.getCreatedBy());
        dto.setLastModifiedBy(a.getLastModifiedBy());
        
        return dto;
    }
}
