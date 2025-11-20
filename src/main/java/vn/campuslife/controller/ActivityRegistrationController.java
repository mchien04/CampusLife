package vn.campuslife.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.campuslife.model.*;
import vn.campuslife.service.ActivityRegistrationService;
import vn.campuslife.service.StudentService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/registrations")
@RequiredArgsConstructor
public class ActivityRegistrationController {

    private static final Logger logger = LoggerFactory.getLogger(ActivityRegistrationController.class);

    private final ActivityRegistrationService registrationService;
    private final StudentService studentService;

    /**
     * Đăng ký tham gia sự kiện
     */
    @PostMapping
    public ResponseEntity<Response> registerForActivity(@RequestBody @Valid ActivityRegistrationRequest request,
            Authentication authentication) {
        try {
            // Get student ID from authentication
            Long studentId = getStudentIdFromAuth(authentication);
            if (studentId == null) {
                return ResponseEntity.badRequest()
                        .body(new Response(false, "Student not found", null));
            }

            Response response = registrationService.registerForActivity(request, studentId);
            return ResponseEntity.status(response.isStatus() ? 201 : 400).body(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Failed to register: " + e.getMessage(), null));
        }
    }

    /**
     * Hủy đăng ký sự kiện
     */
    @DeleteMapping("/activity/{activityId}")
    public ResponseEntity<Response> cancelRegistration(@PathVariable Long activityId,
            Authentication authentication) {
        try {
            Long studentId = getStudentIdFromAuth(authentication);
            if (studentId == null) {
                return ResponseEntity.badRequest()
                        .body(new Response(false, "Student not found", null));
            }

            Response response = registrationService.cancelRegistration(activityId, studentId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Failed to cancel registration: " + e.getMessage(), null));
        }
    }

    /**
     * Lấy danh sách đăng ký của sinh viên
     */
    @GetMapping("/my")
    public ResponseEntity<Response> getMyRegistrations(Authentication authentication) {
        try {
            Long studentId = getStudentIdFromAuth(authentication);
            if (studentId == null) {
                return ResponseEntity.badRequest()
                        .body(new Response(false, "Student not found", null));
            }

            Response response = registrationService.getStudentRegistrations(studentId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Failed to get registrations: " + e.getMessage(), null));
        }
    }

    /**
     * Lấy danh sách đăng ký theo sự kiện (Admin/Manager)
     */
    @GetMapping("/activity/{activityId}")
    public ResponseEntity<Response> getActivityRegistrations(@PathVariable Long activityId) {
        Response response = registrationService.getActivityRegistrations(activityId);
        return ResponseEntity.ok(response);
    }

    /**
     * Cập nhật trạng thái đăng ký (Admin/Manager)
     */
    @PutMapping("/{registrationId}/status")
    public ResponseEntity<Response> updateRegistrationStatus(@PathVariable Long registrationId,
            @RequestParam String status) {
        Response response = registrationService.updateRegistrationStatus(registrationId, status);
        return ResponseEntity.ok(response);
    }

    /**
     * Lấy chi tiết đăng ký
     */
    @GetMapping("/{registrationId}")
    public ResponseEntity<Response> getRegistrationById(@PathVariable Long registrationId) {
        Response response = registrationService.getRegistrationById(registrationId);
        return ResponseEntity.ok(response);
    }

    /**
     * Kiểm tra trạng thái đăng ký
     */
    @GetMapping("/check/{activityId}")
    public ResponseEntity<Response> checkRegistrationStatus(@PathVariable Long activityId,
            Authentication authentication) {
        try {
            Long studentId = getStudentIdFromAuth(authentication);
            if (studentId == null) {
                return ResponseEntity.badRequest()
                        .body(new Response(false, "Student not found", null));
            }

            Response response = registrationService.checkRegistrationStatus(activityId, studentId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Failed to check status: " + e.getMessage(), null));
        }
    }

    /**
     * Debug endpoint để kiểm tra authentication và authorities
     */
    @GetMapping("/checkin/debug")
    public ResponseEntity<Response> debugCheckin(Authentication authentication) {
        try {
            Map<String, Object> debugInfo = new HashMap<>();
            if (authentication != null) {
                debugInfo.put("authenticated", true);
                debugInfo.put("username", authentication.getName());
                debugInfo.put("authorities", authentication.getAuthorities().stream()
                        .map(a -> a.getAuthority())
                        .collect(java.util.stream.Collectors.toList()));
                debugInfo.put("principal", authentication.getPrincipal().getClass().getName());
            } else {
                debugInfo.put("authenticated", false);
            }
            return ResponseEntity.ok(new Response(true, "Debug info", debugInfo));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Debug failed: " + e.getMessage(), null));
        }
    }

    /**
     * Validate/lookup ticketCode để preview thông tin trước khi check-in
     * Hỗ trợ cả quét QR code và nhập code thủ công
     */
    @GetMapping("/checkin/validate")
    public ResponseEntity<Response> validateTicketCode(@RequestParam String ticketCode) {
        try {
            Response response = registrationService.validateTicketCode(ticketCode);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Failed to validate ticket code: " + e.getMessage(), null));
        }
    }

    /**
     * Test endpoint để xác nhận checkin route hoạt động
     */
    @GetMapping("/checkin/test")
    public ResponseEntity<Response> testCheckin(Authentication authentication) {
        Map<String, Object> info = new HashMap<>();
        info.put("message", "Checkin endpoint is working");
        info.put("authenticated", authentication != null);
        if (authentication != null) {
            info.put("username", authentication.getName());
            info.put("authorities", authentication.getAuthorities().stream()
                    .map(a -> a.getAuthority())
                    .collect(java.util.stream.Collectors.toList()));
        }
        return ResponseEntity.ok(new Response(true, "Test successful", info));
    }

    /**
     * Ghi nhận tham gia sự kiện
     * Hỗ trợ cả quét QR code và nhập code thủ công
     */
    @PostMapping("/checkin")
    public ResponseEntity<Response> checkIn(@RequestBody @Valid ActivityParticipationRequest request,
            Authentication authentication) {
        try {
            // Enhanced logging for debugging
            logger.info("=== CHECK-IN REQUEST ===");
            logger.info("Request path: POST /api/registrations/checkin");
            logger.info("TicketCode: {}", request.getTicketCode());
            logger.info("StudentId: {}", request.getStudentId());
            logger.info("Authentication status: {}", authentication != null ? "AUTHENTICATED" : "NULL");
            if (authentication != null) {
                logger.info("Authenticated user: {}", authentication.getName());
                logger.info("User authorities: {}", authentication.getAuthorities());
                logger.info("Principal type: {}", authentication.getPrincipal().getClass().getName());
            } else {
                logger.warn("Authentication is NULL - this should not happen if SecurityConfig is correct");
            }
            logger.info("========================");

            Response response = registrationService.checkIn(request);
            logger.info("Check-in service response: status={}, message={}",
                    response.isStatus(), response.getMessage());
            return ResponseEntity.status(response.isStatus() ? 201 : 400).body(response);
        } catch (Exception e) {
            logger.error("Failed to check-in: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Failed to check-in: " + e.getMessage(), null));
        }
    }

    /**
     * Helper method to get student ID from authentication
     */
    private Long getStudentIdFromAuth(Authentication authentication) {
        try {
            String username = authentication.getName();
            return studentService.getStudentIdByUsername(username);
        } catch (Exception e) {
            return null;
        }
    }

    // Lấy báo cáo tham gia / chưa tham gia
    @GetMapping("/activities/{activityId}/report")
    public ResponseEntity<Response> getReport(
            @PathVariable Long activityId,
            Authentication authentication) {
        System.out.println("Authorities: " + authentication.getAuthorities());
        return ResponseEntity.ok(registrationService.getParticipationReport(activityId));
    }

    /**
     * Chấm điểm completion (đạt/không đạt)
     */
    @PutMapping("/participations/{participationId}/grade")
    public ResponseEntity<Response> gradeCompletion(
            @PathVariable Long participationId,
            @RequestParam boolean isCompleted,
            @RequestParam(required = false) String notes,
            Authentication authentication) {
        try {
            Response response = registrationService.gradeCompletion(participationId, isCompleted, notes);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Failed to grade completion: " + e.getMessage(), null));
        }
    }

    /**
     * Backfill: Tạo participation cho tất cả registration đã APPROVED nhưng chưa có
     * participation
     * Chỉ dành cho Admin/Manager
     */
    @PostMapping("/backfill/participations")
    public ResponseEntity<Response> backfillMissingParticipations() {
        try {
            Response response = registrationService.backfillMissingParticipations();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Failed to backfill participations: " + e.getMessage(), null));
        }
    }

    /**
     * Lấy danh sách participations theo activityId
     * Hiển thị trạng thái của tất cả participations để kiểm tra trước khi chấm điểm
     */
    @GetMapping("/activities/{activityId}/participations")
    public ResponseEntity<Response> getActivityParticipations(@PathVariable Long activityId) {
        try {
            Response response = registrationService.getActivityParticipations(activityId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Failed to get participations: " + e.getMessage(), null));
        }
    }

}
