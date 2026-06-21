package vn.campuslife.model.activity.series;

import lombok.Data;

@Data
public class AddActivityToSeriesRequest {
    private Long activityId;
    private Integer order;
}
