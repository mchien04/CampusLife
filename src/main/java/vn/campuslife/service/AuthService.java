package vn.campuslife.service;

import vn.campuslife.model.Response;
import vn.campuslife.model.LoginRequest;
import vn.campuslife.model.RegisterRequest;
import vn.campuslife.model.ForgotPasswordRequest;
import vn.campuslife.model.ResetPasswordRequest;

public interface AuthService {
    Response register(RegisterRequest request);
    Response login(LoginRequest request);
    Response verifyAccount(String token);
    Response forgotPassword(ForgotPasswordRequest request);
    Response resetPassword(ResetPasswordRequest request);
}