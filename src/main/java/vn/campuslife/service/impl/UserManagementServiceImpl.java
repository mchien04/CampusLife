package vn.campuslife.service.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.campuslife.entity.User;
import vn.campuslife.enumeration.Role;
import vn.campuslife.model.CreateUserRequest;
import vn.campuslife.model.UpdateUserRequest;
import vn.campuslife.model.Response;
import vn.campuslife.model.UserResponse;
import vn.campuslife.repository.UserRepository;
import vn.campuslife.service.UserManagementService;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserManagementServiceImpl implements UserManagementService {

    private static final Logger logger = LoggerFactory.getLogger(UserManagementServiceImpl.class);
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private UserResponse toUserResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole());
        response.setIsActivated(user.isActivated());
        response.setLastLogin(user.getLastLogin());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());
        response.setIsDeleted(user.isDeleted());
        return response;
    }

    @Override
    @Transactional
    public Response createUser(CreateUserRequest request) {
        try {
            // Validate request
            if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
                return new Response(false, "Username is required", null);
            }
            if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                return new Response(false, "Email is required", null);
            }
            if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
                return new Response(false, "Password is required", null);
            }
            if (request.getPassword().length() < 6) {
                return new Response(false, "Password must be at least 6 characters long", null);
            }
            if (request.getRole() == null) {
                return new Response(false, "Role is required", null);
            }
            if (request.getRole() != Role.ADMIN && request.getRole() != Role.MANAGER) {
                return new Response(false, "Role must be ADMIN or MANAGER", null);
            }

            // Check if username already exists
            if (userRepository.findByUsername(request.getUsername()).isPresent()) {
                return new Response(false, "Username already exists", null);
            }

            // Check if email already exists
            if (userRepository.findByEmail(request.getEmail()).isPresent()) {
                return new Response(false, "Email already exists", null);
            }

            // Create new user
            User user = new User();
            user.setUsername(request.getUsername());
            user.setEmail(request.getEmail());
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            user.setRole(request.getRole());
            // Mặc định activated = true khi admin tạo tài khoản (không cần xác nhận email)
            user.setActivated(request.getIsActivated() != null ? request.getIsActivated() : true);
            user.setDeleted(false);

            User savedUser = userRepository.save(user);
            UserResponse response = toUserResponse(savedUser);

            logger.info("Created user: {} with role: {}", savedUser.getUsername(), savedUser.getRole());
            return new Response(true, "User created successfully", response);
        } catch (Exception e) {
            logger.error("Failed to create user: {}", e.getMessage(), e);
            return new Response(false, "Failed to create user: " + e.getMessage(), null);
        }
    }

    @Override
    @Transactional
    public Response updateUser(Long userId, UpdateUserRequest request) {
        try {
            // Find user
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            // Check if user is deleted
            if (user.isDeleted()) {
                return new Response(false, "User has been deleted", null);
            }

            // Update username if provided
            if (request.getUsername() != null && !request.getUsername().trim().isEmpty()) {
                // Check if new username already exists (excluding current user)
                userRepository.findByUsername(request.getUsername())
                        .ifPresent(existingUser -> {
                            if (!existingUser.getId().equals(userId)) {
                                throw new IllegalArgumentException("Username already exists");
                            }
                        });
                user.setUsername(request.getUsername());
            }

            // Update email if provided
            if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
                // Check if new email already exists (excluding current user)
                userRepository.findByEmail(request.getEmail())
                        .ifPresent(existingUser -> {
                            if (!existingUser.getId().equals(userId)) {
                                throw new IllegalArgumentException("Email already exists");
                            }
                        });
                user.setEmail(request.getEmail());
            }

            // Update password if provided
            if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
                if (request.getPassword().length() < 6) {
                    return new Response(false, "Password must be at least 6 characters long", null);
                }
                user.setPassword(passwordEncoder.encode(request.getPassword()));
            }

            // Update role if provided
            if (request.getRole() != null) {
                if (request.getRole() != Role.ADMIN && request.getRole() != Role.MANAGER) {
                    return new Response(false, "Role must be ADMIN or MANAGER", null);
                }
                user.setRole(request.getRole());
            }

            // Update activation status if provided
            if (request.getIsActivated() != null) {
                user.setActivated(request.getIsActivated());
            }

            User updatedUser = userRepository.save(user);
            UserResponse response = toUserResponse(updatedUser);

            logger.info("Updated user: {}", updatedUser.getUsername());
            return new Response(true, "User updated successfully", response);
        } catch (IllegalArgumentException e) {
            return new Response(false, e.getMessage(), null);
        } catch (Exception e) {
            logger.error("Failed to update user: {}", e.getMessage(), e);
            return new Response(false, "Failed to update user: " + e.getMessage(), null);
        }
    }

    @Override
    @Transactional
    public Response deleteUser(Long userId) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            if (user.isDeleted()) {
                return new Response(false, "User has already been deleted", null);
            }

            // Soft delete
            user.setDeleted(true);
            userRepository.save(user);

            logger.info("Deleted user: {}", user.getUsername());
            return new Response(true, "User deleted successfully", null);
        } catch (IllegalArgumentException e) {
            return new Response(false, e.getMessage(), null);
        } catch (Exception e) {
            logger.error("Failed to delete user: {}", e.getMessage(), e);
            return new Response(false, "Failed to delete user: " + e.getMessage(), null);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Response getUserById(Long userId) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            if (user.isDeleted()) {
                return new Response(false, "User has been deleted", null);
            }

            UserResponse response = toUserResponse(user);
            return new Response(true, "User retrieved successfully", response);
        } catch (IllegalArgumentException e) {
            return new Response(false, e.getMessage(), null);
        } catch (Exception e) {
            logger.error("Failed to get user: {}", e.getMessage(), e);
            return new Response(false, "Failed to get user: " + e.getMessage(), null);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Response getAllUsers() {
        try {
            List<User> users = userRepository.findAll()
                    .stream()
                    .filter(user -> !user.isDeleted())
                    .filter(user -> user.getRole() == Role.ADMIN || user.getRole() == Role.MANAGER)
                    .collect(Collectors.toList());

            List<UserResponse> responses = users.stream()
                    .map(this::toUserResponse)
                    .collect(Collectors.toList());

            return new Response(true, "Users retrieved successfully", responses);
        } catch (Exception e) {
            logger.error("Failed to get all users: {}", e.getMessage(), e);
            return new Response(false, "Failed to get all users: " + e.getMessage(), null);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Response getUsersByRole(String role) {
        try {
            Role roleEnum;
            try {
                roleEnum = Role.valueOf(role.toUpperCase());
            } catch (IllegalArgumentException e) {
                return new Response(false, "Invalid role. Must be ADMIN or MANAGER", null);
            }

            if (roleEnum != Role.ADMIN && roleEnum != Role.MANAGER) {
                return new Response(false, "Role must be ADMIN or MANAGER", null);
            }

            List<User> users = userRepository.findAll()
                    .stream()
                    .filter(user -> !user.isDeleted())
                    .filter(user -> user.getRole() == roleEnum)
                    .collect(Collectors.toList());

            List<UserResponse> responses = users.stream()
                    .map(this::toUserResponse)
                    .collect(Collectors.toList());

            return new Response(true, "Users retrieved successfully", responses);
        } catch (Exception e) {
            logger.error("Failed to get users by role: {}", e.getMessage(), e);
            return new Response(false, "Failed to get users by role: " + e.getMessage(), null);
        }
    }
}

