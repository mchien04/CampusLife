package vn.campuslife.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.campuslife.model.EventArticleDetailResponse;
import vn.campuslife.model.Response;
import vn.campuslife.service.EventArticleService;

@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class EventArticleController {

    private final EventArticleService eventArticleService;

    @GetMapping("/{slug}")
    public ResponseEntity<Response> getArticleBySlug(@PathVariable String slug) {
        EventArticleDetailResponse response = eventArticleService.getPublishedArticleBySlug(slug);
        return ResponseEntity.ok(Response.success("Article retrieved successfully", response));
    }
}
