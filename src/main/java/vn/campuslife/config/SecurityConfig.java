package vn.campuslife.config;

import org.springframework.http.HttpMethod;
import vn.campuslife.filter.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy; // Added Lazy import
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserDetailsService userDetailsService;
    private final CorsConfigurationSource corsConfigurationSource;

    public SecurityConfig(@Lazy JwtAuthenticationFilter jwtAuthenticationFilter,
            @Lazy UserDetailsService userDetailsService,
            CorsConfigurationSource corsConfigurationSource) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.userDetailsService = userDetailsService;
        this.corsConfigurationSource = corsConfigurationSource;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
            CorsConfigurationSource corsConfigurationSource) throws Exception {
        http
                .cors(c -> c.configurationSource(corsConfigurationSource))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Preflight
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Public
                        .requestMatchers("/api/auth/register", "/api/auth/login", "/api/auth/verify",
                                "/api/auth/forgot-password", "/api/auth/reset-password")
                        .permitAll()
                        .requestMatchers("/api/test/**").permitAll()
                        .requestMatchers("/api/upload/**").permitAll()
                        .requestMatchers("/uploads/**").permitAll()
                        .requestMatchers("/api/departments/**").permitAll()

                        // Check-in endpoints - place early to avoid pattern conflicts
                        .requestMatchers(HttpMethod.POST, "/api/registrations/checkin").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/registrations/checkin/test").authenticated()

                        // Admin-only endpoints
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        // Student Account Management - Admin only
                        .requestMatchers("/api/admin/students/**").hasRole("ADMIN")

                        // Activities
                        .requestMatchers(HttpMethod.GET, "/api/activities/upcoming")
                        .hasAnyRole("STUDENT")
                        .requestMatchers(HttpMethod.GET, "/api/activities/my").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/activities/debug/user-info").authenticated()
                        // Activity Photos - GET allows all authenticated users (STUDENT/MANAGER/ADMIN)
                        .requestMatchers(HttpMethod.GET, "/api/activities/*/photos")
                        .hasAnyRole("STUDENT", "ADMIN", "MANAGER")
                        // Activity Photos - POST/DELETE/PUT require MANAGER/ADMIN
                        .requestMatchers(HttpMethod.POST, "/api/activities/*/photos")
                        .hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.DELETE, "/api/activities/*/photos/*")
                        .hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.PUT, "/api/activities/*/photos/*/order")
                        .hasAnyRole("ADMIN", "MANAGER")

                        // Activity Series
                        // - GET: STUDENT/MANAGER/ADMIN (list, detail, activities, my progress, my
                        // registration)
                        // - POST register: STUDENT
                        // - Other POST/PUT/DELETE: MANAGER/ADMIN
                        .requestMatchers(HttpMethod.GET,
                                "/api/series",
                                "/api/series/*",
                                "/api/series/*/activities",
                                "/api/series/*/progress/my",
                                "/api/series/*/registration/my")
                        .hasAnyRole("STUDENT", "ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.POST, "/api/series/*/register")
                        .hasRole("STUDENT")
                        .requestMatchers("/api/series/**")
                        .hasAnyRole("ADMIN", "MANAGER")

                        // MiniGame - GET allows STUDENT/MANAGER/ADMIN, POST/PUT/DELETE require
                        // MANAGER/ADMIN
                        .requestMatchers(HttpMethod.GET, "/api/minigames/activity/*", "/api/minigames/*/questions",
                                "/api/minigames/*/attempts/my", "/api/minigames/attempts/*")
                        .hasAnyRole("STUDENT", "ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.GET, "/api/minigames")
                        .hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.POST, "/api/minigames/*/start")
                        .hasRole("STUDENT")
                        .requestMatchers(HttpMethod.POST, "/api/minigames/attempts/*/submit")
                        .hasRole("STUDENT")
                        .requestMatchers("/api/minigames/**")
                        .hasAnyRole("ADMIN", "MANAGER")

                        .requestMatchers(HttpMethod.GET, "/api/activities/**").permitAll()
                        .requestMatchers("/api/activities/**").hasAnyRole("ADMIN", "MANAGER")
                        // Hien thi participations
                        .requestMatchers(HttpMethod.GET, "/api/participations").permitAll()
                        // Tasks and Assignments
                        .requestMatchers(HttpMethod.GET, "/api/tasks/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/assignments/student/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/assignments/*/status").hasRole("STUDENT")
                        .requestMatchers("/api/tasks/**").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers("/api/assignments/**").hasAnyRole("ADMIN", "MANAGER")

                        // Activity Registrations
                        // Check-in endpoints (already defined above, but keep validate/debug here)
                        .requestMatchers(HttpMethod.GET, "/api/registrations/checkin/debug").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/registrations/checkin/validate").authenticated()
                        // Other specific routes
                        .requestMatchers(HttpMethod.GET, "/api/registrations/my", "/api/registrations/my/**")
                        .hasRole("STUDENT")
                        .requestMatchers(HttpMethod.POST, "/api/registrations").hasRole("STUDENT")
                        .requestMatchers(HttpMethod.DELETE, "/api/registrations/activity/*").hasRole("STUDENT")
                        .requestMatchers(HttpMethod.GET, "/api/registrations/check/*").hasRole("STUDENT")
                        .requestMatchers(HttpMethod.GET, "/api/registrations/activities/*/report").hasRole("MANAGER")
                        .requestMatchers(HttpMethod.POST, "/api/registrations/backfill/**")
                        .hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.GET, "/api/registrations/activity/*").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.PUT, "/api/registrations/*/status").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.GET, "/api/registrations/*")
                        .hasAnyRole("ADMIN", "MANAGER", "STUDENT")

                        // Student Profile
                        .requestMatchers(HttpMethod.GET, "/api/student/profile").hasRole("STUDENT")
                        .requestMatchers(HttpMethod.PUT, "/api/student/profile").hasRole("STUDENT")
                        .requestMatchers(HttpMethod.GET, "/api/student/profile/*").hasAnyRole("ADMIN", "MANAGER")

                        // Notifications
                        .requestMatchers(HttpMethod.GET, "/api/notifications/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/notifications/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/notifications/**").authenticated()

                        // Public catalogs for students
                        .requestMatchers(HttpMethod.GET, "/api/criteria/**").hasAnyRole("STUDENT", "ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.GET, "/api/academic/**").hasAnyRole("STUDENT", "ADMIN", "MANAGER")

                        // Score Management
                        .requestMatchers(HttpMethod.GET, "/api/scores/student/*/semester/*")
                        .hasAnyRole("STUDENT", "ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.GET, "/api/scores/student/*/semester/*/total")
                        .hasAnyRole("STUDENT", "ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.GET, "/api/scores/ranking")
                        .hasAnyRole("STUDENT", "ADMIN", "MANAGER")
                        // Score history - Student (own), Admin/Manager (all)
                        .requestMatchers(HttpMethod.GET, "/api/scores/history/student/*")
                        .hasAnyRole("STUDENT", "ADMIN", "MANAGER")
                        // Recalculate score endpoints - Admin/Manager only
                        .requestMatchers(HttpMethod.POST, "/api/scores/recalculate/**")
                        .hasAnyRole("ADMIN", "MANAGER")
                        // removed: /api/scores/training/calculate

                        // Task Submissions
                        .requestMatchers(HttpMethod.GET, "/api/submissions/task/*/my").hasRole("STUDENT")
                        .requestMatchers(HttpMethod.POST, "/api/submissions/task/*").hasRole("STUDENT")
                        .requestMatchers(HttpMethod.PUT, "/api/submissions/*").hasRole("STUDENT")
                        .requestMatchers(HttpMethod.DELETE, "/api/submissions/*").hasRole("STUDENT")
                        .requestMatchers(HttpMethod.GET, "/api/submissions/task/*").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.PUT, "/api/submissions/*/grade").hasAnyRole("ADMIN", "MANAGER")

                        // Participation Grading
                        .requestMatchers(HttpMethod.PUT, "/api/registrations/participations/*/grade")
                        .hasAnyRole("ADMIN", "MANAGER")

                        // Address Management
                        .requestMatchers(HttpMethod.GET, "/api/addresses/provinces").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/addresses/provinces/*/wards").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/addresses/my").hasRole("STUDENT")
                        .requestMatchers(HttpMethod.PUT, "/api/addresses/my").hasRole("STUDENT")
                        .requestMatchers(HttpMethod.POST, "/api/addresses/my").hasRole("STUDENT")
                        .requestMatchers(HttpMethod.DELETE, "/api/addresses/my").hasRole("STUDENT")
                        .requestMatchers(HttpMethod.GET, "/api/addresses/search").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/addresses/load-data").hasAnyRole("ADMIN", "MANAGER")

                        // Student Class Management
                        .requestMatchers(HttpMethod.GET, "/api/classes").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/classes/department/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/classes/*").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/classes").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.PUT, "/api/classes/*").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.DELETE, "/api/classes/*").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.GET, "/api/classes/*/students").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.POST, "/api/classes/*/students/*").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.DELETE, "/api/classes/*/students/*").hasAnyRole("ADMIN", "MANAGER")

                        // Student Management
                        .requestMatchers(HttpMethod.GET, "/api/students").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.GET, "/api/students/search").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.GET, "/api/students/without-class").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.GET, "/api/students/department/*").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.GET, "/api/students/*").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.GET, "/api/students/username/*").hasAnyRole("ADMIN", "MANAGER")

                        // Statistics endpoints
                        // Dashboard - all authenticated users (role-based filtering in controller)
                        .requestMatchers(HttpMethod.GET, "/api/statistics/dashboard")
                        .hasAnyRole("STUDENT", "ADMIN", "MANAGER")
                        // Activity statistics - ADMIN, MANAGER only
                        .requestMatchers(HttpMethod.GET, "/api/statistics/activities")
                        .hasAnyRole("ADMIN", "MANAGER")
                        // Student statistics - ADMIN, MANAGER only
                        .requestMatchers(HttpMethod.GET, "/api/statistics/students")
                        .hasAnyRole("ADMIN", "MANAGER")
                        // Score statistics - all authenticated (role-based filtering in controller)
                        .requestMatchers(HttpMethod.GET, "/api/statistics/scores")
                        .hasAnyRole("STUDENT", "ADMIN", "MANAGER")
                        // Series statistics - all authenticated
                        .requestMatchers(HttpMethod.GET, "/api/statistics/series")
                        .hasAnyRole("STUDENT", "ADMIN", "MANAGER")
                        // MiniGame statistics - all authenticated
                        .requestMatchers(HttpMethod.GET, "/api/statistics/minigames")
                        .hasAnyRole("STUDENT", "ADMIN", "MANAGER")
                        // Time-based statistics - ADMIN, MANAGER only
                        .requestMatchers(HttpMethod.GET, "/api/statistics/timeline")
                        .hasAnyRole("ADMIN", "MANAGER")

                        // Default
                        .anyRequest().authenticated())
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
