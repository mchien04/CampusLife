package vn.campuslife.service.impl;

import vn.campuslife.entity.ActivationToken;
import vn.campuslife.entity.User;
import vn.campuslife.entity.Student;
import vn.campuslife.enumeration.Role;
import vn.campuslife.model.AuthResponse;
import vn.campuslife.model.LoginRequest;
import vn.campuslife.model.RegisterRequest;
import vn.campuslife.model.Response;
import vn.campuslife.repository.ActivationTokenRepository;
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
    private final StudentRepository studentRepository;
    private final StudentScoreInitService studentScoreInitService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EmailUtil emailUtil;

    public AuthServiceImpl(UserRepository userRepository, ActivationTokenRepository activationTokenRepository,
            StudentRepository studentRepository, StudentScoreInitService studentScoreInitService,
            PasswordEncoder passwordEncoder, JwtUtil jwtUtil, EmailUtil emailUtil) {
        this.userRepository = userRepository;
        this.activationTokenRepository = activationTokenRepository;
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
}