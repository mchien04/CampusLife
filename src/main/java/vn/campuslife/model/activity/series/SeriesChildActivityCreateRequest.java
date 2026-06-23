package vn.campuslife.model.activity.series;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.campuslife.enumeration.ActivityType;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeriesChildActivityCreateRequest {
    private String name;
    private String description;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String location;
    private Integer order; // Maps to seriesOrder
    private String bannerUrl;
    private String shareLink;
    private String benefits;
    private String requirements;
    private String contactInfo;
    private List<Long> organizerIds;
    private ActivityType type;
}
