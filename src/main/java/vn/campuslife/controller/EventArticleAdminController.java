package vn.campuslife.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.campuslife.model.EventArticleAdminResponse;
import vn.campuslife.model.EventArticleUpsertRequest;
import vn.campuslife.model.Response;
import vn.campuslife.service.EventArticleService;

@RestController
@RequestMapping("/api/admin/articles")
@RequiredArgsConstructor
public class EventArticleAdminController {

    private final EventArticleService eventArticleService;

    @PostMapping
    public ResponseEntity<Response> create(@RequestBody EventArticleUpsertRequest request) {
        EventArticleAdminResponse response = eventArticleService.createArticle(request);
        return ResponseEntity.status(201).body(Response.success("Article created successfully", response));
    }

    @PutMapping("/{articleId}")
    public ResponseEntity<Response> update(@PathVariable Long articleId, @RequestBody EventArticleUpsertRequest request) {
        EventArticleAdminResponse response = eventArticleService.updateArticle(articleId, request);
        return ResponseEntity.ok(Response.success("Article updated successfully", response));
    }

    @PutMapping("/{articleId}/publish")
    public ResponseEntity<Response> publish(@PathVariable Long articleId) {
        EventArticleAdminResponse response = eventArticleService.publishArticle(articleId);
        return ResponseEntity.ok(Response.success("Article published", response));
    }

    @PutMapping("/{articleId}/unpublish")
    public ResponseEntity<Response> unpublish(@PathVariable Long articleId) {
        EventArticleAdminResponse response = eventArticleService.unpublishArticle(articleId);
        return ResponseEntity.ok(Response.success("Article unpublished", response));
    }

    @GetMapping("/{articleId}")
    public ResponseEntity<Response> getById(@PathVariable Long articleId) {
        EventArticleAdminResponse response = eventArticleService.getArticleById(articleId);
        return ResponseEntity.ok(Response.success("Article retrieved successfully", response));
    }

    @GetMapping("/by-activity/{activityId}")
    public ResponseEntity<Response> getByActivityId(@PathVariable Long activityId) {
        EventArticleAdminResponse response = eventArticleService.getArticleByActivityId(activityId);
        return ResponseEntity.ok(Response.success("Article retrieved successfully", response));
    }
}

