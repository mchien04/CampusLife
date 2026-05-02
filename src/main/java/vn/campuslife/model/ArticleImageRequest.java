package vn.campuslife.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArticleImageRequest {
    private String imageUrl;
    private String caption;
    private int displayOrder;
    private boolean isCover;
}
