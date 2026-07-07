package vn.campuslife.service;

import vn.campuslife.model.CreateUserRequest;
import vn.campuslife.model.UpdateUserRequest;
import vn.campuslife.model.Response;

public interface UserManagementService {
    Response createUser(CreateUserRequest request, String assignedByUsername);

    Response createUser(CreateUserRequest request);

    Response updateUser(Long userId, UpdateUserRequest request, String assignedByUsername);

    Response updateUser(Long userId, UpdateUserRequest request);

    Response deleteUser(Long userId);

    Response getUserById(Long userId);

    Response getAllUsers();

    Response getAllUsersIncludingStudents();

    Response getUsersByRole(String role);
}
