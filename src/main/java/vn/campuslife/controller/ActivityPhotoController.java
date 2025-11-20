package vn.campuslife.controller;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.campuslife.model.Response;
import vn.campuslife.service.ActivityPhotoService;

import java.util.List;

@RestController
@RequestMapping("/api/activities/{activityId}/photos")
@RequiredArgsConstructor
public class ActivityPhotoController {

    private static final Logger logger = LoggerFactory.getLogger(ActivityPhotoController.class);

    private final ActivityPhotoService photoService;

    /**
     * Upload photos for an activity (only after activity ends)
     * Manager/Admin only
     */
    @PostMapping
    public ResponseEntity<Response> uploadPhotos(
            @PathVariable Long activityId,
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(value = "captions", required = false) List<String> captions,
            Authentication authentication) {
        try {
            if (files == null || files.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new Response(false, "No files provided", null));
            }

            String username = authentication != null ? authentication.getName() : "unknown";
            Response response = photoService.uploadPhotos(activityId, files, captions, username);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to upload photos: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Failed to upload photos: " + e.getMessage(), null));
        }
    }

    /**
     * Get all photos for an activity
     * Student/Manager/Admin can view
     */
    @GetMapping
    public ResponseEntity<Response> getActivityPhotos(@PathVariable Long activityId) {
        try {
            Response response = photoService.getActivityPhotos(activityId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to get photos: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Failed to get photos: " + e.getMessage(), null));
        }
    }

    /**
     * Delete a photo (soft delete)
     * Manager/Admin only
     */
    @DeleteMapping("/{photoId}")
    public ResponseEntity<Response> deletePhoto(
            @PathVariable Long activityId,
            @PathVariable Long photoId,
            Authentication authentication) {
        try {
            String username = authentication != null ? authentication.getName() : "unknown";
            Response response = photoService.deletePhoto(photoId, username);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to delete photo: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Failed to delete photo: " + e.getMessage(), null));
        }
    }

    /**
     * Update display order of a photo
     * Manager/Admin only
     */
    @PutMapping("/{photoId}/order")
    public ResponseEntity<Response> updatePhotoOrder(
            @PathVariable Long activityId,
            @PathVariable Long photoId,
            @RequestParam Integer order,
            Authentication authentication) {
        try {
            String username = authentication != null ? authentication.getName() : "unknown";
            Response response = photoService.updatePhotoOrder(photoId, order, username);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to update photo order: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Failed to update photo order: " + e.getMessage(), null));
        }
    }
}

