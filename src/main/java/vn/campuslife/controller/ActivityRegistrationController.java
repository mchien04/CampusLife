package vn.campuslife.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.campuslife.model.*;
import vn.campuslife.service.ActivityRegistrationService;
import vn.campuslife.service.StudentService;

@RestController
@RequestMapping("/api/registrations")
@RequiredArgsConstructor
public class ActivityRegistrationController {

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
     * Ghi nhận tham gia sự kiện
     */
    @PostMapping("/checkin")
    public ResponseEntity<Response> checkIn(@RequestBody @Valid ActivityParticipationRequest request) {
        try {
            Response response = registrationService.checkIn(request);
            return ResponseEntity.status(response.isStatus() ? 201 : 400).body(response);
        } catch (Exception e) {
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

}
