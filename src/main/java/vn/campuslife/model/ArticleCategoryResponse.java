package vn.campuslife.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArticleCategoryResponse {
    private Long id;
    private String name;
    private String description;
    private String slug;
    private int displayOrder;
    private boolean isActive;
    private LocalDateTime createdAt;
}
