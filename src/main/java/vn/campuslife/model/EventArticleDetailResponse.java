package vn.campuslife.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.campuslife.enumeration.RegistrationCtaStatus;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventArticleDetailResponse {
    private Long id;
    private String title;
    private String slug;
    private String thumbnailUrl;
    private String content;
    private String seoTitle;
    private String seoDescription;
    private boolean isPublished;
    private LocalDateTime publishedAt;
    private RegistrationCtaStatus registrationStatus;
    private String registrationLink;
}
