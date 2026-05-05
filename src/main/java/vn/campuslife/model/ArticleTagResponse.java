package vn.campuslife.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArticleTagResponse {
    private Long id;
    private String name;
    private String slug;
    private boolean isActive;
    private LocalDateTime createdAt;
}
