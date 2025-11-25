package vn.campuslife.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivityPhotoResponse {
    private Long id;
    private Long activityId;
    private String imageUrl;
    private String caption;
    private Integer displayOrder;
    private String uploadedBy;
    private LocalDateTime createdAt;
}

