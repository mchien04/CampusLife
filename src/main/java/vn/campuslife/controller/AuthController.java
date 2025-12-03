package vn.campuslife.controller;

import vn.campuslife.model.Response;
import vn.campuslife.model.LoginRequest;
import vn.campuslife.model.RegisterRequest;
import vn.campuslife.model.ForgotPasswordRequest;
import vn.campuslife.model.ResetPasswordRequest;
import vn.campuslife.model.ChangePasswordRequest;
import vn.campuslife.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<Response> register(@RequestBody RegisterRequest request) {
        try {
            Response response = authService.register(request);
            if (response.isStatus()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            System.err.println("Controller exception: " + e.getMessage());
            e.printStackTrace();
            Response errorResponse = new Response(false, "Server error occurred", null);
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<Response> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/verify")
    public ResponseEntity<Response> verifyAccount(@RequestParam("token") String token) {
        return ResponseEntity.ok(authService.verifyAccount(token));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Response> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        Response response = authService.forgotPassword(request);
        if (response.isStatus()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Response> resetPassword(@RequestBody ResetPasswordRequest request) {
        Response response = authService.resetPassword(request);
        if (response.isStatus()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/change-password")
    public ResponseEntity<Response> changePassword(
            @RequestBody ChangePasswordRequest request,
            Authentication authentication) {
        try {
            if (authentication == null || authentication.getName() == null) {
                return ResponseEntity.badRequest()
                        .body(new Response(false, "Authentication required", null));
            }

            String username = authentication.getName();
            Response response = authService.changePassword(username, request);
            if (response.isStatus()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            System.err.println("Controller exception: " + e.getMessage());
            e.printStackTrace();
            Response errorResponse = new Response(false, "Server error occurred", null);
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

}
