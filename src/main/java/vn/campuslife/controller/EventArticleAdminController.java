package vn.campuslife.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.campuslife.model.*;
import vn.campuslife.service.EventArticleService;
import vn.campuslife.service.StudentService;

import java.util.List;

@RestController
@RequestMapping("/api/admin/articles")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class EventArticleAdminController {

    private final EventArticleService eventArticleService;
    private final StudentService studentService;

    @GetMapping
    public ResponseEntity<Page<ArticleListResponse>> getAllArticles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<ArticleListResponse> articles = eventArticleService.getAllArticlesForAdmin(page, size);
        return ResponseEntity.ok(articles);
    }

    @GetMapping("/statistics")
    public ResponseEntity<ArticleStatisticsResponse> getStatistics() {
        ArticleStatisticsResponse stats = eventArticleService.getArticleStatistics();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/{articleId}")
    public ResponseEntity<EventArticleAdminResponse> getArticleById(@PathVariable Long articleId) {
        EventArticleAdminResponse response = eventArticleService.getArticleById(articleId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/by-activity/{activityId}")
    public ResponseEntity<EventArticleAdminResponse> getArticleByActivityId(@PathVariable Long activityId) {
        EventArticleAdminResponse response = eventArticleService.getArticleByActivityId(activityId);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<EventArticleAdminResponse> createArticle(@RequestBody EventArticleUpsertRequest request) {
        EventArticleAdminResponse response = eventArticleService.createArticle(request);
        return ResponseEntity.status(201).body(response);
    }

    @PutMapping("/{articleId}")
    public ResponseEntity<EventArticleAdminResponse> updateArticle(
            @PathVariable Long articleId,
            @RequestBody EventArticleUpsertRequest request) {
        EventArticleAdminResponse response = eventArticleService.updateArticle(articleId, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{articleId}/publish")
    public ResponseEntity<EventArticleAdminResponse> publishArticle(@PathVariable Long articleId) {
        EventArticleAdminResponse response = eventArticleService.publishArticle(articleId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{articleId}/unpublish")
    public ResponseEntity<EventArticleAdminResponse> unpublishArticle(@PathVariable Long articleId) {
        EventArticleAdminResponse response = eventArticleService.unpublishArticle(articleId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{articleId}/images")
    public ResponseEntity<ArticleImageResponse> addImage(
            @PathVariable Long articleId,
            @RequestBody ArticleImageRequest request) {
        ArticleImageResponse response = eventArticleService.addImageToArticle(articleId, request);
        return ResponseEntity.status(201).body(response);
    }

    @DeleteMapping("/{articleId}/images/{imageId}")
    public ResponseEntity<Void> removeImage(
            @PathVariable Long articleId,
            @PathVariable Long imageId) {
        eventArticleService.removeImageFromArticle(articleId, imageId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/categories")
    public ResponseEntity<List<ArticleCategoryResponse>> getAllCategories() {
        List<ArticleCategoryResponse> categories = eventArticleService.getAllCategories();
        return ResponseEntity.ok(categories);
    }

    @PostMapping("/categories")
    public ResponseEntity<ArticleCategoryResponse> createCategory(@RequestBody ArticleCategoryRequest request) {
        ArticleCategoryResponse response = eventArticleService.createCategory(request);
        return ResponseEntity.status(201).body(response);
    }

    @PutMapping("/categories/{categoryId}")
    public ResponseEntity<ArticleCategoryResponse> updateCategory(
            @PathVariable Long categoryId,
            @RequestBody ArticleCategoryRequest request) {
        ArticleCategoryResponse response = eventArticleService.updateCategory(categoryId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/categories/{categoryId}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long categoryId) {
        eventArticleService.deleteCategory(categoryId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/tags")
    public ResponseEntity<List<ArticleTagResponse>> getAllTags() {
        List<ArticleTagResponse> tags = eventArticleService.getAllTags();
        return ResponseEntity.ok(tags);
    }

    @PostMapping("/tags")
    public ResponseEntity<ArticleTagResponse> createTag(@RequestBody ArticleTagRequest request) {
        ArticleTagResponse response = eventArticleService.createTag(request);
        return ResponseEntity.status(201).body(response);
    }

    @DeleteMapping("/tags/{tagId}")
    public ResponseEntity<Void> deleteTag(@PathVariable Long tagId) {
        eventArticleService.deleteTag(tagId);
        return ResponseEntity.noContent().build();
    }
}
