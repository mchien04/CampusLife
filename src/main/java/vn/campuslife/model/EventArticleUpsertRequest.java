package vn.campuslife.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.campuslife.enumeration.ArticleType;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventArticleUpsertRequest {
    private Long ownerDepartmentId;
    private Long activityId;
    private ArticleType articleType;
    @JsonProperty("isPrimary")
    private boolean isPrimary;
    private String title;
    private String slug;
    private String thumbnailUrl;
    private String content;
    private String seoTitle;
    private String seoDescription;
    private Long categoryId;
    private List<Long> tagIds;
    @JsonProperty("isFeatured")
    private boolean isFeatured;
    @JsonProperty("isPinned")
    private boolean isPinned;
    private int priority;
}
