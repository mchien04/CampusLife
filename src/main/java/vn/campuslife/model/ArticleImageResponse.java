package vn.campuslife.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArticleImageResponse {
    private Long id;
    private String imageUrl;
    private String caption;
    private int displayOrder;
    private boolean isCover;
    private LocalDateTime createdAt;
}
