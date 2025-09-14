package vn.campuslife.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.campuslife.model.*;
import vn.campuslife.service.StudentProfileService;
import vn.campuslife.service.StudentService;

@RestController
@RequestMapping("/api/student/profile")
@RequiredArgsConstructor
public class StudentProfileController {

    private final StudentProfileService profileService;
    private final StudentService studentService;

    /**
     * Lấy thông tin profile của student hiện tại
     */
    @GetMapping
    public ResponseEntity<Response> getMyProfile(Authentication authentication) {
        try {
            Long studentId = getStudentIdFromAuth(authentication);
            if (studentId == null) {
                return ResponseEntity.badRequest()
                        .body(new Response(false, "Student not found", null));
            }

            Response response = profileService.getStudentProfile(studentId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Failed to get profile: " + e.getMessage(), null));
        }
    }

    /**
     * Cập nhật thông tin profile của student
     */
    @PutMapping
    public ResponseEntity<Response> updateMyProfile(@RequestBody @Valid StudentProfileUpdateRequest request,
            Authentication authentication) {
        try {
            Long studentId = getStudentIdFromAuth(authentication);
            if (studentId == null) {
                return ResponseEntity.badRequest()
                        .body(new Response(false, "Student not found", null));
            }

            Response response = profileService.updateStudentProfile(studentId, request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Failed to update profile: " + e.getMessage(), null));
        }
    }

    /**
     * Lấy thông tin profile theo username (Admin/Manager)
     */
    @GetMapping("/{username}")
    public ResponseEntity<Response> getStudentProfileByUsername(@PathVariable String username) {
        Response response = profileService.getStudentProfileByUsername(username);
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
