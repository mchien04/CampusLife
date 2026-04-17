package vn.campuslife.service;

import vn.campuslife.model.EventArticleAdminResponse;
import vn.campuslife.model.EventArticleDetailResponse;
import vn.campuslife.model.EventArticleUpsertRequest;

public interface EventArticleService {
    EventArticleDetailResponse getPublishedArticleBySlug(String slug);

    EventArticleAdminResponse createArticle(EventArticleUpsertRequest request);

    EventArticleAdminResponse updateArticle(Long articleId, EventArticleUpsertRequest request);

    EventArticleAdminResponse publishArticle(Long articleId);

    EventArticleAdminResponse unpublishArticle(Long articleId);

    EventArticleAdminResponse getArticleById(Long articleId);

    EventArticleAdminResponse getArticleByActivityId(Long activityId);
}
