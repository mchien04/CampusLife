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
    @PostMapping("/participate")
    public ResponseEntity<Response> recordParticipation(@RequestBody @Valid ActivityParticipationRequest request,
            Authentication authentication) {
        try {
            Long studentId = getStudentIdFromAuth(authentication);
            if (studentId == null) {
                return ResponseEntity.badRequest()
                        .body(new Response(false, "Student not found", null));
            }

            Response response = registrationService.recordParticipation(request, studentId);
            return ResponseEntity.status(response.isStatus() ? 201 : 400).body(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Failed to record participation: " + e.getMessage(), null));
        }
    }

    /**
     * Lấy danh sách tham gia của sinh viên
     */
    @GetMapping("/my/participations")
    public ResponseEntity<Response> getMyParticipations(Authentication authentication) {
        try {
            Long studentId = getStudentIdFromAuth(authentication);
            if (studentId == null) {
                return ResponseEntity.badRequest()
                        .body(new Response(false, "Student not found", null));
            }

            Response response = registrationService.getStudentParticipations(studentId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Failed to get participations: " + e.getMessage(), null));
        }
    }

    /**
     * Lấy danh sách tham gia theo sự kiện (Admin/Manager)
     */
    @GetMapping("/activity/{activityId}/participations")
    public ResponseEntity<Response> getActivityParticipations(@PathVariable Long activityId) {
        Response response = registrationService.getActivityParticipations(activityId);
        return ResponseEntity.ok(response);
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
}
