package vn.campuslife.controller;

import vn.campuslife.service.FileUploadService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/upload")
public class FileUploadController {

    private final FileUploadService fileUploadService;

    public FileUploadController(FileUploadService fileUploadService) {
        this.fileUploadService = fileUploadService;
    }

    @PostMapping("/image")
    public ResponseEntity<Map<String, Object>> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            // Validate file type
            if (file.isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("status", false);
                error.put("message", "Please select a file to upload");
                return ResponseEntity.badRequest().body(error);
            }

            // Validate file size (max 5MB)
            if (file.getSize() > 5 * 1024 * 1024) {
                Map<String, Object> error = new HashMap<>();
                error.put("status", false);
                error.put("message", "File size must be less than 5MB");
                return ResponseEntity.badRequest().body(error);
            }

            // Validate file type
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                Map<String, Object> error = new HashMap<>();
                error.put("status", false);
                error.put("message", "Only image files are allowed");
                return ResponseEntity.badRequest().body(error);
            }

            // Upload file
            String fileUrl = fileUploadService.uploadFile(file);

            Map<String, Object> success = new HashMap<>();
            success.put("status", true);
            success.put("message", "File uploaded successfully");
            success.put("data", fileUrl);

            return ResponseEntity.ok(success);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", false);
            error.put("message", "Failed to upload file: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @DeleteMapping("/image")
    public ResponseEntity<Map<String, Object>> deleteImage(@RequestParam("fileUrl") String fileUrl) {
        try {
            fileUploadService.deleteFile(fileUrl);

            Map<String, Object> success = new HashMap<>();
            success.put("status", true);
            success.put("message", "File deleted successfully");

            return ResponseEntity.ok(success);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", false);
            error.put("message", "Failed to delete file: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
}
