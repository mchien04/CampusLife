package vn.campuslife.service;

import vn.campuslife.model.CreateUserRequest;
import vn.campuslife.model.UpdateUserRequest;
import vn.campuslife.model.Response;

public interface UserManagementService {
    Response createUser(CreateUserRequest request);
    Response updateUser(Long userId, UpdateUserRequest request);
    Response deleteUser(Long userId);
    Response getUserById(Long userId);
    Response getAllUsers();
    Response getAllUsersIncludingStudents();
    Response getUsersByRole(String role);
}

