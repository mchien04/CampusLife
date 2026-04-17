package vn.campuslife.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventArticleUpsertRequest {
    private Long activityId;
    private String title;
    private String slug;
    private String thumbnailUrl;
    private String content;
    private String seoTitle;
    private String seoDescription;
}

