package vn.campuslife.service.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import vn.campuslife.config.UploadProperties;
import vn.campuslife.entity.Activity;
import vn.campuslife.model.activity.ActivitySummaryResponse;
import vn.campuslife.util.UrlUtils;

@Component
@RequiredArgsConstructor
public class ActivitySummaryMapper {

    private final UploadProperties uploadProperties;

    public ActivitySummaryResponse toSummary(Activity entity) {
        if (entity == null) {
            return null;
        }
        
        ActivitySummaryResponse response = new ActivitySummaryResponse();
        response.setId(entity.getId());
        response.setName(entity.getName());
        response.setType(entity.getType());
        response.setStartDate(entity.getStartDate());
        response.setEndDate(entity.getEndDate());
        response.setBannerUrl(UrlUtils.toFullUrl(entity.getBannerUrl(), uploadProperties.getPublicUrl()));
        response.setIsDraft(entity.isDraft());
        response.setIsImportant(entity.isImportant());
        response.setLocation(entity.getLocation());
        response.setSeriesId(entity.getSeriesId());
        
        if (entity.getSeriesId() != null) {
            response.setVariantTag("SERIES_CHILD");
        } else if (vn.campuslife.enumeration.ActivityType.MINIGAME.equals(entity.getType())) {
            response.setVariantTag("MINIGAME");
        } else {
            response.setVariantTag("STANDARD");
        }
        
        return response;
    }
}
