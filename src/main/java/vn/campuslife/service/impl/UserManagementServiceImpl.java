package vn.campuslife.service.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.campuslife.entity.Department;
import vn.campuslife.entity.User;
import vn.campuslife.entity.UserDepartment;
import vn.campuslife.enumeration.Role;
import vn.campuslife.model.CreateUserRequest;
import vn.campuslife.model.UpdateUserRequest;
import vn.campuslife.model.Response;
import vn.campuslife.model.UserResponse;
import vn.campuslife.repository.DepartmentRepository;
import vn.campuslife.repository.UserDepartmentRepository;
import vn.campuslife.repository.UserRepository;
import vn.campuslife.service.UserManagementService;
import vn.campuslife.service.UserUniquenessHelper;
import vn.campuslife.util.UserSoftDeleteSupport;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserManagementServiceImpl implements UserManagementService {

    private static final Logger logger = LoggerFactory.getLogger(UserManagementServiceImpl.class);

    private final UserRepository userRepository;
    private final UserDepartmentRepository userDepartmentRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public Response createUser(CreateUserRequest request) {
        return createUser(request, null);
    }

    @Override
    @Transactional
    public Response createUser(CreateUserRequest request, String assignedByUsername) {
        try {
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
            if (request.getRole() == Role.MANAGER && (request.getDepartmentIds() == null
                    || request.getDepartmentIds().isEmpty())) {
                return new Response(false, "departmentIds is required when creating a MANAGER account", null);
            }

            UserUniquenessHelper.reclaimDeletedIdentifiers(
                    userRepository, request.getUsername(), request.getEmail());

            if (userRepository.existsByUsernameAndIsDeletedFalse(request.getUsername())) {
                return new Response(false, "Username already exists", null);
            }
            if (userRepository.existsByEmailAndIsDeletedFalse(request.getEmail())) {
                return new Response(false, "Email already exists", null);
            }

            User user = new User();
            user.setUsername(request.getUsername());
            user.setEmail(request.getEmail());
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            user.setRole(request.getRole());
            user.setActivated(request.getIsActivated() != null ? request.getIsActivated() : true);
            user.setDeleted(false);

            User savedUser = userRepository.save(user);

            if (savedUser.getRole() == Role.MANAGER) {
                User assignedBy = resolveAssignedBy(assignedByUsername);
                replaceManagerDepartments(savedUser, request.getDepartmentIds(), assignedBy);
            }

            UserResponse response = toUserResponse(savedUser);
            logger.info("Created user: {} with role: {}", savedUser.getUsername(), savedUser.getRole());
            return new Response(true, "User created successfully", response);
        } catch (IllegalArgumentException e) {
            return new Response(false, e.getMessage(), null);
        } catch (Exception e) {
            logger.error("Failed to create user: {}", e.getMessage(), e);
            return new Response(false, "Failed to create user: " + e.getMessage(), null);
        }
    }

    @Override
    @Transactional
    public Response updateUser(Long userId, UpdateUserRequest request) {
        return updateUser(userId, request, null);
    }

    @Override
    @Transactional
    public Response updateUser(Long userId, UpdateUserRequest request, String assignedByUsername) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            if (user.isDeleted()) {
                return new Response(false, "User has been deleted", null);
            }

            if (request.getUsername() != null && !request.getUsername().trim().isEmpty()) {
                String username = request.getUsername().trim();
                UserUniquenessHelper.reclaimDeletedIdentifiers(userRepository, username, null);
                userRepository.findByUsernameAndIsDeletedFalse(username)
                        .ifPresent(existingUser -> {
                            if (!existingUser.getId().equals(userId)) {
                                throw new IllegalArgumentException("Username already exists");
                            }
                        });
                user.setUsername(username);
            }

            if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
                String email = request.getEmail().trim();
                UserUniquenessHelper.reclaimDeletedIdentifiers(userRepository, null, email);
                userRepository.findByEmailAndIsDeletedFalse(email)
                        .ifPresent(existingUser -> {
                            if (!existingUser.getId().equals(userId)) {
                                throw new IllegalArgumentException("Email already exists");
                            }
                        });
                user.setEmail(email);
            }

            if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
                if (request.getPassword().length() < 6) {
                    return new Response(false, "Password must be at least 6 characters long", null);
                }
                user.setPassword(passwordEncoder.encode(request.getPassword()));
            }

            if (request.getRole() != null) {
                if (request.getRole() != Role.ADMIN && request.getRole() != Role.MANAGER) {
                    return new Response(false, "Role must be ADMIN or MANAGER", null);
                }
                user.setRole(request.getRole());
            }

            if (request.getIsActivated() != null) {
                user.setActivated(request.getIsActivated());
            }

            User updatedUser = userRepository.save(user);

            if (updatedUser.getRole() == Role.MANAGER && request.getDepartmentIds() != null) {
                if (request.getDepartmentIds().isEmpty()) {
                    return new Response(false, "MANAGER must have at least one department", null);
                }
                User assignedBy = resolveAssignedBy(assignedByUsername);
                replaceManagerDepartments(updatedUser, request.getDepartmentIds(), assignedBy);
            }

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

            UserSoftDeleteSupport.softDelete(user);
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

            return new Response(true, "User retrieved successfully", toUserResponse(user));
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
            List<UserResponse> responses = userRepository.findAll().stream()
                    .filter(user -> !user.isDeleted())
                    .filter(user -> user.getRole() == Role.ADMIN || user.getRole() == Role.MANAGER)
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
    public Response getAllUsersIncludingStudents() {
        try {
            List<UserResponse> responses = userRepository.findAll().stream()
                    .filter(user -> !user.isDeleted())
                    .map(this::toUserResponse)
                    .collect(Collectors.toList());

            return new Response(true, "All users retrieved successfully", responses);
        } catch (Exception e) {
            logger.error("Failed to get all users including students: {}", e.getMessage(), e);
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

            List<UserResponse> responses = userRepository.findAll().stream()
                    .filter(user -> !user.isDeleted())
                    .filter(user -> user.getRole() == roleEnum)
                    .map(this::toUserResponse)
                    .collect(Collectors.toList());

            return new Response(true, "Users retrieved successfully", responses);
        } catch (Exception e) {
            logger.error("Failed to get users by role: {}", e.getMessage(), e);
            return new Response(false, "Failed to get users by role: " + e.getMessage(), null);
        }
    }

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
        if (user.getRole() == Role.MANAGER) {
            response.setDepartmentIds(new ArrayList<>(userDepartmentRepository.findActiveDepartmentIdsByUserId(user.getId())));
        }
        return response;
    }

    private void replaceManagerDepartments(User manager, List<Long> departmentIds, User assignedBy) {
        Set<Long> uniqueIds = new LinkedHashSet<>(departmentIds.stream().filter(id -> id != null).toList());
        if (uniqueIds.isEmpty()) {
            throw new IllegalArgumentException("departmentIds is required");
        }

        List<Department> departments = departmentRepository.findAllById(uniqueIds);
        Set<Long> foundIds = departments.stream().map(Department::getId).collect(Collectors.toSet());
        List<Long> missing = uniqueIds.stream().filter(id -> !foundIds.contains(id)).toList();
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException("Department ids not found: " + missing);
        }

        userDepartmentRepository.deleteByUser_Id(manager.getId());
        for (Department department : departments) {
            userDepartmentRepository.save(new UserDepartment(manager, department, assignedBy));
        }
    }

    private User resolveAssignedBy(String assignedByUsername) {
        if (assignedByUsername == null || assignedByUsername.isBlank()) {
            return null;
        }
        return userRepository.findByUsernameAndIsDeletedFalse(assignedByUsername).orElse(null);
    }
}
