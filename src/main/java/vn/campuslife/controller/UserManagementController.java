package vn.campuslife.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.campuslife.model.CreateUserRequest;
import vn.campuslife.model.Response;
import vn.campuslife.model.UpdateUserRequest;
import vn.campuslife.service.UserManagementService;

@RestController
@RequestMapping("/api/admin/users")
public class UserManagementController {

    private final UserManagementService userManagementService;

    public UserManagementController(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    @PostMapping
    public ResponseEntity<Response> createUser(@RequestBody CreateUserRequest request) {
        try {
            Response response = userManagementService.createUser(request);
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

    @PutMapping("/{userId}")
    public ResponseEntity<Response> updateUser(
            @PathVariable Long userId,
            @RequestBody UpdateUserRequest request) {
        try {
            Response response = userManagementService.updateUser(userId, request);
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

    @DeleteMapping("/{userId}")
    public ResponseEntity<Response> deleteUser(@PathVariable Long userId) {
        try {
            Response response = userManagementService.deleteUser(userId);
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

    @GetMapping("/{userId}")
    public ResponseEntity<Response> getUserById(@PathVariable Long userId) {
        try {
            Response response = userManagementService.getUserById(userId);
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

    @GetMapping
    public ResponseEntity<Response> getAllUsers(
            @RequestParam(required = false) String role,
            @RequestParam(required = false, defaultValue = "false") boolean includeStudents) {
        try {
            Response response;
            if (role != null && !role.trim().isEmpty()) {
                response = userManagementService.getUsersByRole(role);
            } else if (includeStudents) {
                response = userManagementService.getAllUsersIncludingStudents();
            } else {
                response = userManagementService.getAllUsers();
            }
            
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

