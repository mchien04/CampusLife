package vn.campuslife.filter;

import vn.campuslife.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, UserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        final String authorizationHeader = request.getHeader("Authorization");
        String requestPath = request.getRequestURI();
        String requestMethod = request.getMethod();

        logger.debug("Processing request: {} {}", requestMethod, requestPath);

        String username = null;
        String jwt = null;

        // Extract token from Authorization header
        try {
            if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                jwt = authorizationHeader.substring(7);
                logger.debug("JWT token extracted from Authorization header");
                
                try {
                    username = jwtUtil.extractUsername(jwt);
                    logger.debug("Username extracted from token: {}", username);
                } catch (Exception e) {
                    logger.warn("Failed to extract username from token: {}", e.getMessage());
                    // Continue filter chain even if token extraction fails
                }
            } else {
                logger.debug("No Authorization header found or header doesn't start with 'Bearer '");
            }
        } catch (Exception e) {
            logger.error("Error extracting token from Authorization header: {}", e.getMessage(), e);
            // Continue filter chain even on error
        }

        // Set authentication if token is valid
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                logger.debug("Loading user details for username: {}", username);
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
                logger.debug("User details loaded. User: {}, Authorities: {}", 
                    userDetails.getUsername(), userDetails.getAuthorities());

                if (jwtUtil.validateToken(jwt, userDetails)) {
                    logger.debug("Token validation successful for user: {}", username);
                    UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    usernamePasswordAuthenticationToken
                            .setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
                    logger.info("Authentication set successfully for user: {} on {} {}", 
                        username, requestMethod, requestPath);
                } else {
                    logger.warn("Token validation failed for user: {}", username);
                }
            } catch (org.springframework.security.core.userdetails.UsernameNotFoundException e) {
                logger.warn("User not found: {}", username);
                // Continue filter chain even if user not found
            } catch (Exception e) {
                logger.error("Error loading user details or validating token for user {}: {}", 
                    username, e.getMessage(), e);
                // Continue filter chain even on error
            }
        } else {
            if (username == null) {
                logger.debug("No username extracted, skipping authentication setup");
            } else {
                logger.debug("Authentication already exists in SecurityContext, skipping");
            }
        }

        filterChain.doFilter(request, response);
    }
}
