package vn.campuslife.model;

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
    private boolean isActive;
}
