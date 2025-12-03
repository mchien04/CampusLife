package vn.campuslife.service.impl;

import vn.campuslife.entity.ActivationToken;
import vn.campuslife.entity.PasswordResetToken;
import vn.campuslife.entity.User;
import vn.campuslife.entity.Student;
import vn.campuslife.enumeration.Role;
import vn.campuslife.model.AuthResponse;
import vn.campuslife.model.LoginRequest;
import vn.campuslife.model.RegisterRequest;
import vn.campuslife.model.ForgotPasswordRequest;
import vn.campuslife.model.ResetPasswordRequest;
import vn.campuslife.model.ChangePasswordRequest;
import vn.campuslife.model.Response;
import vn.campuslife.repository.ActivationTokenRepository;
import vn.campuslife.repository.PasswordResetTokenRepository;
import vn.campuslife.repository.UserRepository;
import vn.campuslife.repository.StudentRepository;
import vn.campuslife.service.StudentScoreInitService;
import vn.campuslife.util.JwtUtil;
import vn.campuslife.util.EmailUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;

@Service
public class AuthServiceImpl implements vn.campuslife.service.AuthService {

    private final UserRepository userRepository;
    private final ActivationTokenRepository activationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final StudentRepository studentRepository;
    private final StudentScoreInitService studentScoreInitService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EmailUtil emailUtil;

    public AuthServiceImpl(UserRepository userRepository, ActivationTokenRepository activationTokenRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            StudentRepository studentRepository, StudentScoreInitService studentScoreInitService,
            PasswordEncoder passwordEncoder, JwtUtil jwtUtil, EmailUtil emailUtil) {
        this.userRepository = userRepository;
        this.activationTokenRepository = activationTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.studentRepository = studentRepository;
        this.studentScoreInitService = studentScoreInitService;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.emailUtil = emailUtil;
    }

    @Override
    @Transactional
    public Response login(LoginRequest request) {
        try {
            // Validate request
            if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
                return new Response(false, "Username is required", null);
            }
            if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
                return new Response(false, "Password is required", null);
            }

            // Find user
            User user = userRepository.findByUsernameAndIsDeletedFalse(request.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found with username: " + request.getUsername()));

            // Check if user is activated
            if (!user.isActivated()) {
                return new Response(false, "Account is not activated. Please check your email.", null);
            }

            // Verify password
            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                return new Response(false, "Invalid password", null);
            }

            // Update last login timestamp
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);

            // Generate JWT token
            String token = jwtUtil.generateToken(new org.springframework.security.core.userdetails.User(
                    user.getUsername(), user.getPassword(),
                    Collections.singletonList(new org.springframework.security.core.authority.SimpleGrantedAuthority(
                            "ROLE_" + user.getRole().name()))));

            return new Response(true, "Login successful", new AuthResponse(token));
        } catch (Exception e) {
            return new Response(false, "Login failed: " + e.getMessage(), null);
        }
    }

    @Override
    @Transactional
    public Response register(RegisterRequest request) {
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

            // Check if username or email already exists
            if (userRepository.findByUsername(request.getUsername()).isPresent()) {
                return new Response(false, "Username already exists", null);
            }
            if (userRepository.findByEmail(request.getEmail()).isPresent()) {
                return new Response(false, "Email already exists", null);
            }

            // Create new user
            User user = new User();
            user.setUsername(request.getUsername());
            user.setEmail(request.getEmail());
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            user.setRole(Role.STUDENT);
            user.setActivated(false);

            // Save user
            User savedUser = userRepository.save(user);

            // Auto-create student profile if role is STUDENT
            if (savedUser.getRole() == Role.STUDENT) {
                Student student = new Student();
                student.setUser(savedUser);
                // All other fields remain null - student will fill them later
                Student savedStudent = studentRepository.save(student);

                // Initialize 3 score records (REN_LUYEN, CONG_TAC_XA_HOI, CHUYEN_DE) for
                // current semester
                try {
                    studentScoreInitService.initializeStudentScoresForCurrentSemester(savedStudent);
                } catch (Exception e) {
                    // Log error but don't fail registration
                    System.err.println("Failed to initialize student scores: " + e.getMessage());
                }
            }

            // Generate and save activation token
            String token = UUID.randomUUID().toString();
            ActivationToken activationToken = new ActivationToken();
            activationToken.setUser(savedUser);
            activationToken.setToken(token);
            activationToken.setExpiryDate(LocalDateTime.now().plusDays(1)); // Token valid for 1 day
            activationToken.setUsed(false);
            activationTokenRepository.save(activationToken);

            // GỬI EMAIL KÍCH HOẠT
            boolean emailSent = emailUtil.sendActivationEmail(savedUser.getEmail(), token);
            if (!emailSent) {
                // Log error nhưng vẫn trả về success để user có thể request gửi lại email
                System.err.println("Failed to send activation email to: " + savedUser.getEmail());
            }

            return new Response(true, "Registration successful. Please check your email to activate your account.",
                    null);
        } catch (Exception e) {
            return new Response(false, "Registration failed: " + e.getMessage(), null);
        }
    }

    @Override
    @Transactional
    public Response verifyAccount(String token) {
        try {
            // Find activation token
            ActivationToken activationToken = activationTokenRepository.findByTokenAndUsedFalse(token)
                    .orElseThrow(() -> new RuntimeException("Invalid or used token"));

            // Check if token is expired
            if (activationToken.getExpiryDate().isBefore(LocalDateTime.now())) {
                return new Response(false, "Token has expired", null);
            }

            // Activate user
            User user = activationToken.getUser();
            user.setActivated(true);
            userRepository.save(user);

            // Mark token as used
            activationToken.setUsed(true);
            activationTokenRepository.save(activationToken);

            return new Response(true, "Account activated successfully", null);
        } catch (Exception e) {
            return new Response(false, "Account activation failed: " + e.getMessage(), null);
        }
    }

    @Override
    @Transactional
    public Response forgotPassword(ForgotPasswordRequest request) {
        try {
            // Validate request
            if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                return new Response(false, "Email is required", null);
            }

            // Find user by email
            User user = userRepository.findByEmail(request.getEmail())
                    .orElse(null);

            // Always return success message to prevent email enumeration attacks
            // If user exists, send reset email; if not, just return success
            if (user != null && !user.isDeleted()) {
                // Invalidate any existing unused reset tokens for this user
                passwordResetTokenRepository.findByUserIdAndUsedFalse(user.getId())
                        .ifPresent(existingToken -> {
                            existingToken.setUsed(true);
                            passwordResetTokenRepository.save(existingToken);
                        });

                // Generate and save reset token
                String token = UUID.randomUUID().toString();
                PasswordResetToken resetToken = new PasswordResetToken();
                resetToken.setUser(user);
                resetToken.setToken(token);
                resetToken.setExpiryDate(LocalDateTime.now().plusHours(1)); // Token valid for 1 hour
                resetToken.setUsed(false);
                resetToken.setCreatedAt(LocalDateTime.now());
                passwordResetTokenRepository.save(resetToken);

                // Send reset email
                boolean emailSent = emailUtil.sendPasswordResetEmail(user.getEmail(), token);
                if (!emailSent) {
                    System.err.println("Failed to send password reset email to: " + user.getEmail());
                }
            }

            // Always return success to prevent email enumeration
            return new Response(true, 
                    "If an account with that email exists, a password reset link has been sent.", null);
        } catch (Exception e) {
            return new Response(false, "Failed to process password reset request: " + e.getMessage(), null);
        }
    }

    @Override
    @Transactional
    public Response resetPassword(ResetPasswordRequest request) {
        try {
            // Validate request
            if (request.getToken() == null || request.getToken().trim().isEmpty()) {
                return new Response(false, "Token is required", null);
            }
            if (request.getNewPassword() == null || request.getNewPassword().trim().isEmpty()) {
                return new Response(false, "New password is required", null);
            }
            if (request.getNewPassword().length() < 6) {
                return new Response(false, "Password must be at least 6 characters long", null);
            }

            // Find reset token
            PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenAndUsedFalse(request.getToken())
                    .orElseThrow(() -> new RuntimeException("Invalid or used token"));

            // Check if token is expired
            if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
                return new Response(false, "Token has expired. Please request a new password reset.", null);
            }

            // Update user password
            User user = resetToken.getUser();
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
            userRepository.save(user);

            // Mark token as used
            resetToken.setUsed(true);
            passwordResetTokenRepository.save(resetToken);

            return new Response(true, "Password reset successfully. You can now login with your new password.", null);
        } catch (RuntimeException e) {
            return new Response(false, e.getMessage(), null);
        } catch (Exception e) {
            return new Response(false, "Failed to reset password: " + e.getMessage(), null);
        }
    }

    @Override
    @Transactional
    public Response changePassword(String username, ChangePasswordRequest request) {
        try {
            // Validate request
            if (request.getOldPassword() == null || request.getOldPassword().trim().isEmpty()) {
                return new Response(false, "Old password is required", null);
            }
            if (request.getNewPassword() == null || request.getNewPassword().trim().isEmpty()) {
                return new Response(false, "New password is required", null);
            }
            if (request.getConfirmPassword() == null || request.getConfirmPassword().trim().isEmpty()) {
                return new Response(false, "Confirm password is required", null);
            }

            // Validate password length
            if (request.getNewPassword().length() < 6) {
                return new Response(false, "New password must be at least 6 characters long", null);
            }

            // Validate new password and confirm password match
            if (!request.getNewPassword().equals(request.getConfirmPassword())) {
                return new Response(false, "New password and confirm password do not match", null);
            }

            // Validate new password is different from old password
            if (request.getOldPassword().equals(request.getNewPassword())) {
                return new Response(false, "New password must be different from old password", null);
            }

            // Find user
            User user = userRepository.findByUsernameAndIsDeletedFalse(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Verify old password
            if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
                return new Response(false, "Old password is incorrect", null);
            }

            // Update password
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
            userRepository.save(user);

            return new Response(true, "Password changed successfully", null);
        } catch (RuntimeException e) {
            return new Response(false, e.getMessage(), null);
        } catch (Exception e) {
            return new Response(false, "Failed to change password: " + e.getMessage(), null);
        }
    }
}