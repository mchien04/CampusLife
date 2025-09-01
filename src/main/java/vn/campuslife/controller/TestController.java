package vn.campuslife.controller;

import vn.campuslife.util.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class TestController {

    private final JwtUtil jwtUtil;

    public TestController(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @GetMapping("/token-info")
    public ResponseEntity<Map<String, Object>> getTokenInfo(@RequestHeader("Authorization") String authHeader) {
        try {
            // Extract token from "Bearer <token>"
            String token = authHeader.substring(7);

            Map<String, Object> tokenInfo = new HashMap<>();
            tokenInfo.put("username", jwtUtil.extractUsername(token));
            tokenInfo.put("role", jwtUtil.extractRole(token));
            tokenInfo.put("expiration", jwtUtil.extractExpiration(token));

            return ResponseEntity.ok(tokenInfo);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Invalid token: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/log-request")
    public ResponseEntity<Map<String, Object>> logRequest(@RequestBody Map<String, Object> requestData) {
        try {
            System.out.println("=== INCOMING REQUEST DATA ===");
            System.out.println("Request body: " + requestData);
            System.out.println("bannerUrl: " + requestData.get("bannerUrl"));
            System.out.println("bannerUrl type: "
                    + (requestData.get("bannerUrl") != null ? requestData.get("bannerUrl").getClass().getName()
                            : "null"));
            System.out.println("=============================");

            Map<String, Object> response = new HashMap<>();
            response.put("status", true);
            response.put("message", "Request logged successfully");
            response.put("receivedData", requestData);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", false);
            error.put("error", "Failed to log request: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
}
