package vn.campuslife.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.campuslife.entity.Activity;
import vn.campuslife.entity.EventArticle;
import vn.campuslife.enumeration.RegistrationCtaStatus;
import vn.campuslife.enumeration.RegistrationStatus;
import vn.campuslife.exception.BadRequestException;
import vn.campuslife.exception.ResourceNotFoundException;
import vn.campuslife.model.EventArticleAdminResponse;
import vn.campuslife.model.EventArticleDetailResponse;
import vn.campuslife.model.EventArticleUpsertRequest;
import vn.campuslife.repository.ActivityRepository;
import vn.campuslife.repository.ActivityRegistrationRepository;
import vn.campuslife.repository.EventArticleRepository;
import vn.campuslife.service.EventArticleService;
import vn.campuslife.util.UrlUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class EventArticleServiceImpl implements EventArticleService {

    private final EventArticleRepository eventArticleRepository;
    private final ActivityRegistrationRepository activityRegistrationRepository;
    private final ActivityRepository activityRepository;

    @org.springframework.beans.factory.annotation.Value("${app.upload.public-url:http://localhost:8080}")
    private String publicUrl;

    @Override
    @Transactional(readOnly = true)
    public EventArticleDetailResponse getPublishedArticleBySlug(String slug) {
        EventArticle article = eventArticleRepository.findBySlugAndIsPublishedTrue(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Article not found with slug: " + slug));

        Activity activity = article.getActivity();
        RegistrationCtaStatus registrationStatus = resolveRegistrationStatus(activity, LocalDateTime.now());

        EventArticleDetailResponse response = new EventArticleDetailResponse();
        response.setId(article.getId());
        response.setTitle(article.getTitle());
        response.setSlug(article.getSlug());
        response.setThumbnailUrl(UrlUtils.toFullUrl(article.getThumbnailUrl(), publicUrl));
        response.setContent(article.getContent());
        response.setSeoTitle(article.getSeoTitle());
        response.setSeoDescription(article.getSeoDescription());
        response.setPublished(article.isPublished());
        response.setPublishedAt(article.getPublishedAt());
        response.setRegistrationStatus(registrationStatus);
        response.setRegistrationLink(resolveRegistrationLink(activity));
        return response;
    }

    @Override
    @Transactional
    public EventArticleAdminResponse createArticle(EventArticleUpsertRequest request) {
        validateUpsertRequest(request, true);
        Long activityId = request.getActivityId();

        Activity activity = activityRepository.findByIdAndIsDeletedFalse(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Activity not found: " + activityId));

        if (eventArticleRepository.findByActivityId(activityId).isPresent()) {
            throw new BadRequestException("This activity already has an article");
        }

        String slug = normalizeSlug(request.getSlug());
        if (eventArticleRepository.existsBySlug(slug)) {
            throw new BadRequestException("Slug already exists");
        }

        EventArticle article = new EventArticle();
        article.setActivity(activity);
        article.setTitle(request.getTitle().trim());
        article.setSlug(slug);
        article.setThumbnailUrl(trimToNull(request.getThumbnailUrl()));
        article.setContent(request.getContent());
        article.setSeoTitle(trimToNull(request.getSeoTitle()));
        article.setSeoDescription(trimToNull(request.getSeoDescription()));
        article.setPublished(false);
        article.setPublishedAt(null);

        EventArticle saved = eventArticleRepository.save(article);
        return toAdminResponse(saved);
    }

    @Override
    @Transactional
    public EventArticleAdminResponse updateArticle(Long articleId, EventArticleUpsertRequest request) {
        validateUpsertRequest(request, false);

        EventArticle article = eventArticleRepository.findById(articleId)
                .orElseThrow(() -> new ResourceNotFoundException("Article not found: " + articleId));

        if (request.getActivityId() != null && !request.getActivityId().equals(article.getActivity().getId())) {
            throw new BadRequestException("Cannot change activityId of an existing article");
        }

        String slug = normalizeSlug(request.getSlug());
        if (eventArticleRepository.existsBySlugAndIdNot(slug, articleId)) {
            throw new BadRequestException("Slug already exists");
        }

        article.setTitle(request.getTitle().trim());
        article.setSlug(slug);
        article.setThumbnailUrl(trimToNull(request.getThumbnailUrl()));
        article.setContent(request.getContent());
        article.setSeoTitle(trimToNull(request.getSeoTitle()));
        article.setSeoDescription(trimToNull(request.getSeoDescription()));

        EventArticle saved = eventArticleRepository.save(article);
        return toAdminResponse(saved);
    }

    @Override
    @Transactional
    public EventArticleAdminResponse publishArticle(Long articleId) {
        EventArticle article = eventArticleRepository.findById(articleId)
                .orElseThrow(() -> new ResourceNotFoundException("Article not found: " + articleId));

        if (!article.isPublished()) {
            article.setPublished(true);
            article.setPublishedAt(LocalDateTime.now());
        }

        EventArticle saved = eventArticleRepository.save(article);
        return toAdminResponse(saved);
    }

    @Override
    @Transactional
    public EventArticleAdminResponse unpublishArticle(Long articleId) {
        EventArticle article = eventArticleRepository.findById(articleId)
                .orElseThrow(() -> new ResourceNotFoundException("Article not found: " + articleId));

        if (article.isPublished()) {
            article.setPublished(false);
            article.setPublishedAt(null);
        }

        EventArticle saved = eventArticleRepository.save(article);
        return toAdminResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public EventArticleAdminResponse getArticleById(Long articleId) {
        EventArticle article = eventArticleRepository.findById(articleId)
                .orElseThrow(() -> new ResourceNotFoundException("Article not found: " + articleId));
        return toAdminResponse(article);
    }

    @Override
    @Transactional(readOnly = true)
    public EventArticleAdminResponse getArticleByActivityId(Long activityId) {
        EventArticle article = eventArticleRepository.findByActivityId(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Article not found for activity: " + activityId));
        return toAdminResponse(article);
    }

    private RegistrationCtaStatus resolveRegistrationStatus(Activity activity, LocalDateTime now) {
        LocalDateTime startDate = activity.getRegistrationStartDate();
        LocalDateTime deadline = activity.getRegistrationDeadline();

        if (startDate != null && now.isBefore(startDate)) {
            return RegistrationCtaStatus.UPCOMING;
        }

        if (deadline != null && now.isAfter(deadline)) {
            return RegistrationCtaStatus.CLOSED;
        }

        Long approvedCount = activityRegistrationRepository
                .countByActivityIdAndStatus(activity.getId(), RegistrationStatus.APPROVED);
        if (activity.getTicketQuantity() != null && approvedCount >= activity.getTicketQuantity()) {
            return RegistrationCtaStatus.FULL;
        }

        return RegistrationCtaStatus.OPEN;
    }

    private String resolveRegistrationLink(Activity activity) {
        if (activity.getShareLink() != null && !activity.getShareLink().isBlank()) {
            return activity.getShareLink();
        }
        return "/activities/" + activity.getId();
    }

    private void validateUpsertRequest(EventArticleUpsertRequest request, boolean requireActivityId) {
        if (request == null) {
            throw new BadRequestException("Request body is required");
        }
        if (requireActivityId && request.getActivityId() == null) {
            throw new BadRequestException("activityId is required");
        }
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new BadRequestException("title is required");
        }
        if (request.getSlug() == null || request.getSlug().isBlank()) {
            throw new BadRequestException("slug is required");
        }
        if (request.getContent() == null || request.getContent().isBlank()) {
            throw new BadRequestException("content is required");
        }
    }

    private String normalizeSlug(String slug) {
        return slug.trim();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String v = value.trim();
        return v.isBlank() ? null : v;
    }

    private EventArticleAdminResponse toAdminResponse(EventArticle article) {
        EventArticleAdminResponse res = new EventArticleAdminResponse();
        res.setId(article.getId());
        res.setActivityId(article.getActivity().getId());
        res.setTitle(article.getTitle());
        res.setSlug(article.getSlug());
        res.setThumbnailUrl(UrlUtils.toFullUrl(article.getThumbnailUrl(), publicUrl));
        res.setContent(article.getContent());
        res.setSeoTitle(article.getSeoTitle());
        res.setSeoDescription(article.getSeoDescription());
        res.setPublished(article.isPublished());
        res.setPublishedAt(article.getPublishedAt());
        res.setCreatedAt(article.getCreatedAt());
        res.setUpdatedAt(article.getUpdatedAt());
        return res;
    }
}
