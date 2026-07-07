package vn.campuslife.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArticleCategoryRequest {
    private String name;
    private String description;
    private String slug;
    private int displayOrder;
    @JsonProperty("isActive")
    private boolean isActive;
}
