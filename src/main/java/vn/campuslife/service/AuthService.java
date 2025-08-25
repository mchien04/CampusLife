package vn.campuslife.service;

import vn.campuslife.model.Response;
import vn.campuslife.model.LoginRequest;
import vn.campuslife.model.RegisterRequest;

public interface AuthService {
    Response register(RegisterRequest request);
    Response login(LoginRequest request);
    Response verifyAccount(String token);
}