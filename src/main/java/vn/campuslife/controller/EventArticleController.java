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
import vn.campuslife.service.ArticleCommentService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class EventArticleController {

    private final EventArticleService eventArticleService;
    private final StudentService studentService;
    private final ArticleCommentService articleCommentService;

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

    @GetMapping("/series/{seriesId}")
    public ResponseEntity<List<ArticleListResponse>> getArticlesBySeriesId(@PathVariable Long seriesId) {
        List<ArticleListResponse> articles = eventArticleService.getArticlesBySeriesId(seriesId);
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

    @GetMapping("/trending")
    public ResponseEntity<List<ArticleListResponse>> getTrendingArticles(
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(defaultValue = "5") int limit) {
        List<ArticleListResponse> articles = eventArticleService.getTrendingArticles(days, limit);
        return ResponseEntity.ok(articles);
    }

    @PostMapping("/{slug}/comments")
    public ResponseEntity<ArticleCommentResponse> addComment(
            @PathVariable String slug,
            @RequestBody ArticleCommentRequest request,
            Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        ArticleCommentResponse response = articleCommentService.addComment(slug, authentication.getName(), request);
        return ResponseEntity.status(201).body(response);
    }

    @GetMapping("/{slug}/comments")
    public ResponseEntity<Page<ArticleCommentResponse>> getArticleComments(
            @PathVariable String slug,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<ArticleCommentResponse> comments = articleCommentService.getArticleComments(slug, false, page, size);
        return ResponseEntity.ok(comments);
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long commentId,
            Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        articleCommentService.deleteComment(commentId, authentication.getName(), false);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{slug}/reaction")
    public ResponseEntity<Response> addReaction(
            @PathVariable String slug,
            @RequestParam vn.campuslife.enumeration.ReactionType type,
            Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).body(new Response(false, "Authentication required", null));
        }
        Response response = eventArticleService.addReaction(slug, authentication.getName(), type);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{slug}/reaction")
    public ResponseEntity<Response> removeReaction(
            @PathVariable String slug,
            Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).body(new Response(false, "Authentication required", null));
        }
        Response response = eventArticleService.removeReaction(slug, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{slug}/reactions")
    public ResponseEntity<Map<String, Long>> getReactionCounts(@PathVariable String slug) {
        Map<String, Long> counts = eventArticleService.getReactionCounts(slug);
        return ResponseEntity.ok(counts);
    }

    @PostMapping("/{slug}/track-share")
    public ResponseEntity<Response> trackShare(@PathVariable String slug) {
        Response response = eventArticleService.trackShare(slug);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history")
    public ResponseEntity<Page<ArticleHistoryResponse>> getReadingHistory(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        Page<ArticleHistoryResponse> history = eventArticleService.getReadingHistory(authentication.getName(), page, size);
        return ResponseEntity.ok(history);
    }

    @DeleteMapping("/history/{historyId}")
    public ResponseEntity<Void> deleteReadingHistory(
            @PathVariable Long historyId,
            Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        eventArticleService.deleteReadingHistory(authentication.getName(), historyId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/history")
    public ResponseEntity<Void> clearAllReadingHistory(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        eventArticleService.clearAllReadingHistory(authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
