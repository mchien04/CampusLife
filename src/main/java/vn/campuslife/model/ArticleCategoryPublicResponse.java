package vn.campuslife.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArticleCategoryPublicResponse {
    private Long id;
    private String name;
    private String slug;
    private Long articleCount;
}
