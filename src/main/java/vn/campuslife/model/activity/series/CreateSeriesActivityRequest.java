package vn.campuslife.model.activity.series;

import lombok.Data;
import vn.campuslife.enumeration.ActivityType;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CreateSeriesActivityRequest {
    private String name;
    private String description;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String location;
    private Integer order;
    private String shareLink;
    private String bannerUrl;
    private String benefits;
    private String requirements;
    private String contactInfo;
    private List<Long> organizerIds;
    private ActivityType type;
}
