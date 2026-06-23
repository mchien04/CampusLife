package vn.campuslife.model.activity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.campuslife.enumeration.ActivityType;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivitySummaryResponse {
    private Long id;
    private String name;
    private ActivityType type;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String bannerUrl;
    private Boolean isDraft;
    private Boolean isImportant;
    private String location;
    
    // "STANDARD", "MINIGAME", or "SERIES_CHILD"
    private String variantTag;
    
    private Long seriesId;
}
