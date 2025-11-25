package vn.campuslife.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.campuslife.model.Response;
import vn.campuslife.service.NotificationService;
import vn.campuslife.repository.UserRepository;
import vn.campuslife.entity.User;

import java.util.Optional;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    /**
     * Lấy danh sách thông báo của user hiện tại
     */
    @GetMapping
    public ResponseEntity<Response> getMyNotifications(Authentication authentication, Pageable pageable) {
        try {
            Long userId = getUserIdFromAuth(authentication);
            if (userId == null) {
                return ResponseEntity.badRequest()
                        .body(new Response(false, "User not found", null));
            }

            Response response = notificationService.getUserNotifications(userId, pageable);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Failed to get notifications: " + e.getMessage(), null));
        }
    }

    /**
     * Lấy thông báo chưa đọc
     */
    @GetMapping("/unread")
    public ResponseEntity<Response> getUnreadNotifications(Authentication authentication) {
        try {
            Long userId = getUserIdFromAuth(authentication);
            if (userId == null) {
                return ResponseEntity.badRequest()
                        .body(new Response(false, "User not found", null));
            }

            Response response = notificationService.getUnreadNotifications(userId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Failed to get unread notifications: " + e.getMessage(), null));
        }
    }

    /**
     * Đánh dấu thông báo đã đọc
     */
    @PutMapping("/{notificationId}/read")
    public ResponseEntity<Response> markAsRead(@PathVariable Long notificationId, Authentication authentication) {
        try {
            Long userId = getUserIdFromAuth(authentication);
            if (userId == null) {
                return ResponseEntity.badRequest()
                        .body(new Response(false, "User not found", null));
            }

            Response response = notificationService.markAsRead(notificationId, userId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Failed to mark as read: " + e.getMessage(), null));
        }
    }

    /**
     * Đánh dấu tất cả thông báo đã đọc
     */
    @PutMapping("/read-all")
    public ResponseEntity<Response> markAllAsRead(Authentication authentication) {
        try {
            Long userId = getUserIdFromAuth(authentication);
            if (userId == null) {
                return ResponseEntity.badRequest()
                        .body(new Response(false, "User not found", null));
            }

            Response response = notificationService.markAllAsRead(userId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Failed to mark all as read: " + e.getMessage(), null));
        }
    }

    /**
     * Lấy số lượng thông báo chưa đọc
     */
    @GetMapping("/unread-count")
    public ResponseEntity<Response> getUnreadCount(Authentication authentication) {
        try {
            Long userId = getUserIdFromAuth(authentication);
            if (userId == null) {
                return ResponseEntity.badRequest()
                        .body(new Response(false, "User not found", null));
            }

            Response response = notificationService.getUnreadCount(userId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Failed to get unread count: " + e.getMessage(), null));
        }
    }

    /**
     * Xóa thông báo
     */
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<Response> deleteNotification(@PathVariable Long notificationId,
            Authentication authentication) {
        try {
            Long userId = getUserIdFromAuth(authentication);
            if (userId == null) {
                return ResponseEntity.badRequest()
                        .body(new Response(false, "User not found", null));
            }

            Response response = notificationService.deleteNotification(notificationId, userId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Failed to delete notification: " + e.getMessage(), null));
        }
    }

    /**
     * Lưu trữ thông báo
     */
    @PutMapping("/{notificationId}/archive")
    public ResponseEntity<Response> archiveNotification(@PathVariable Long notificationId,
            Authentication authentication) {
        try {
            Long userId = getUserIdFromAuth(authentication);
            if (userId == null) {
                return ResponseEntity.badRequest()
                        .body(new Response(false, "User not found", null));
            }

            Response response = notificationService.archiveNotification(notificationId, userId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Failed to archive notification: " + e.getMessage(), null));
        }
    }

    /**
     * Helper method to get user ID from authentication
     */
    private Long getUserIdFromAuth(Authentication authentication) {
        try {
            if (authentication == null) {
                return null;
            }
            String username = authentication.getName();
            Optional<User> userOpt = userRepository.findByUsername(username);
            return userOpt.map(User::getId).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }
}
