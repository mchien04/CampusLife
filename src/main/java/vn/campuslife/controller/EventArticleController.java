package vn.campuslife.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.campuslife.model.*;
import vn.campuslife.service.EventArticleService;
import vn.campuslife.service.StudentService;

import java.util.List;

@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class EventArticleController {

    private final EventArticleService eventArticleService;
    private final StudentService studentService;

    @GetMapping
    public ResponseEntity<Page<ArticleListResponse>> getAllArticles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<ArticleListResponse> articles = eventArticleService.getAllPublishedArticles(page, size);
        return ResponseEntity.ok(articles);
    }

    @GetMapping("/featured")
    public ResponseEntity<List<ArticleListResponse>> getFeaturedArticles() {
        List<ArticleListResponse> articles = eventArticleService.getFeaturedArticles();
        return ResponseEntity.ok(articles);
    }

    @GetMapping("/category/{categorySlug}")
    public ResponseEntity<Page<ArticleListResponse>> getArticlesByCategory(
            @PathVariable String categorySlug,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<ArticleListResponse> articles = eventArticleService.getPublishedArticlesByCategory(categorySlug, page,
                size);
        return ResponseEntity.ok(articles);
    }

    @GetMapping("/search")
    public ResponseEntity<Page<ArticleListResponse>> searchArticles(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<ArticleListResponse> articles = eventArticleService.searchPublishedArticles(keyword, page, size);
        return ResponseEntity.ok(articles);
    }

    @GetMapping("/{slug}")
    public ResponseEntity<ArticleDetailResponse> getArticleBySlug(
            @PathVariable String slug,
            Authentication authentication) {
        Long studentId = null;
        if (authentication != null) {
            studentId = getStudentIdFromAuth(authentication);
        }
        ArticleDetailResponse response = eventArticleService.getArticleDetailBySlug(slug, studentId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{slug}/related")
    public ResponseEntity<List<ArticleListResponse>> getRelatedArticles(
            @PathVariable String slug,
            @RequestParam(defaultValue = "3") int limit) {
        List<ArticleListResponse> related = eventArticleService.getRelatedArticles(slug, limit);
        return ResponseEntity.ok(related);
    }

    @GetMapping("/{slug}/calendar")
    public ResponseEntity<byte[]> getCalendarFile(@PathVariable String slug) {
        byte[] icsData = eventArticleService.generateIcsFile(slug);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + slug + ".ics\"")
                .contentType(MediaType.parseMediaType("text/calendar"))
                .body(icsData);
    }

    @PostMapping("/{slug}/track-view")
    public ResponseEntity<Response> trackView(@PathVariable String slug) {
        eventArticleService.trackView(slug);
        return ResponseEntity.ok(Response.success("View tracked", null));
    }

    @PostMapping("/{slug}/waitlist")
    public ResponseEntity<Response> registerForWaitlist(@PathVariable String slug, Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).body(new Response(false, "Authentication required", null));
        }
        Response response = eventArticleService.registerForWaitlist(slug, authentication.getName());
        return ResponseEntity.status(response.isStatus() ? 201 : 400).body(response);
    }

    @PostMapping("/{slug}/wishlist")
    public ResponseEntity<Response> addToWishlist(@PathVariable String slug, Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).body(new Response(false, "Authentication required", null));
        }
        Response response = eventArticleService.addToWishlist(slug, authentication.getName());
        return ResponseEntity.status(response.isStatus() ? 201 : 400).body(response);
    }

    @DeleteMapping("/{slug}/wishlist")
    public ResponseEntity<Response> removeFromWishlist(@PathVariable String slug, Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).body(new Response(false, "Authentication required", null));
        }
        Response response = eventArticleService.removeFromWishlist(slug, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/wishlist")
    public ResponseEntity<Page<ArticleWishlistItemResponse>> getStudentWishlist(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        Page<ArticleWishlistItemResponse> wishlist = eventArticleService.getStudentWishlist(authentication.getName(),
                page, size);
        return ResponseEntity.ok(wishlist);
    }

    private Long getStudentIdFromAuth(Authentication authentication) {
        if (authentication == null)
            return null;
        try {
            return studentService.getStudentIdByUsername(authentication.getName());
        } catch (Exception e) {
            return null;
        }
    }
}
