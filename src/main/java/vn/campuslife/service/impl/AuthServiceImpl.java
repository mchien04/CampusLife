package vn.campuslife.service.impl;

import vn.campuslife.entity.ActivationToken;
import vn.campuslife.entity.User;
import vn.campuslife.enumeration.Role;
import vn.campuslife.model.AuthResponse;
import vn.campuslife.model.Response;
import vn.campuslife.model.LoginRequest;
import vn.campuslife.model.RegisterRequest;
import vn.campuslife.repository.ActivationTokenRepository;
import vn.campuslife.repository.UserRepository;
import vn.campuslife.service.AuthService;
import vn.campuslife.util.EmailUtil;
import vn.campuslife.util.JwtUtil;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final ActivationTokenRepository activationTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final EmailUtil emailUtil;
    private final UserDetailsService userDetailsService;

    public AuthServiceImpl(UserRepository userRepository, ActivationTokenRepository activationTokenRepository,
                           PasswordEncoder passwordEncoder, JwtUtil jwtUtil, AuthenticationManager authenticationManager,
                           EmailUtil emailUtil, UserDetailsService userDetailsService) {
        this.userRepository = userRepository;
        this.activationTokenRepository = activationTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
        this.emailUtil = emailUtil;
        this.userDetailsService = userDetailsService;
    }

    @Override
    public Response register(RegisterRequest request) {
        try {
            if (userRepository.findByUsername(request.getUsername()).isPresent()) {
                return new Response(false, "Username already exists", null);
            }
            if (userRepository.findByEmail(request.getEmail()).isPresent()) {
                return new Response(false, "Email already exists", null);
            }

            User user = new User();
            user.setUsername(request.getUsername());
            user.setEmail(request.getEmail());
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            user.setRole(Role.STUDENT);
            user.setActivated(false);
            userRepository.save(user);

            String token = UUID.randomUUID().toString();
            ActivationToken activationToken = new ActivationToken();
            activationToken.setUser(user);
            activationToken.setToken(token);
            activationToken.setExpiryDate(LocalDateTime.now().plusHours(24));
            activationToken.setUsed(false);
            activationTokenRepository.save(activationToken);

            boolean emailSent = emailUtil.sendActivationEmail(user.getEmail(), token);
            if (emailSent) {
                return new Response(true, "Registration successful. Please check your email to activate your account.", null);
            } else {
                return new Response(true, "Registration successful, but failed to send activation email. Please contact support.", null);
            }
        } catch (Exception e) {
            System.err.println("Registration failed with exception: " + e.getMessage());
            e.printStackTrace();
            return new Response(false, "Registration failed due to server error.", null);
        }
    }

    @Override
    public Response login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        if (authentication.isAuthenticated()) {
            User user = userRepository.findByUsername(request.getUsername())
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));
            if (!user.isActivated()) {
                return new Response(false, "Account not activated. Please check your email.", null);
            }
            UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername());
            String jwtToken = jwtUtil.generateToken(userDetails);
            return new Response(true, "Login successful", new AuthResponse(jwtToken));
        } else {
            return new Response(false, "Invalid credentials", null);
        }
    }

    @Override
    public Response verifyAccount(String token) {
        ActivationToken activationToken = activationTokenRepository.findByToken(token)
                .orElseGet(() -> null);

        if (activationToken == null) {
            return new Response(false, "Invalid token", null);
        }
        if (activationToken.isUsed()) {
            return new Response(false, "Token already used", null);
        }
        if (activationToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            return new Response(false, "Token expired", null);
        }

        User user = activationToken.getUser();
        user.setActivated(true);
        userRepository.save(user);

        activationToken.setUsed(true);
        activationTokenRepository.save(activationToken);

        return new Response(true, "Account activated successfully", null);
    }
}
