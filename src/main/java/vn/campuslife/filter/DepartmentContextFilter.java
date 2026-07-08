package vn.campuslife.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import vn.campuslife.config.DepartmentScopeProperties;
import vn.campuslife.exception.ForbiddenException;
import vn.campuslife.model.Response;
import vn.campuslife.security.department.DepartmentRequestScope;
import vn.campuslife.security.department.DepartmentScope;
import vn.campuslife.security.department.DepartmentScopeResolver;

import java.io.IOException;

@Component
public class DepartmentContextFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(DepartmentContextFilter.class);

    private final DepartmentScopeResolver departmentScopeResolver;
    private final DepartmentScopeProperties departmentScopeProperties;
    private final ObjectMapper objectMapper;

    public DepartmentContextFilter(
            DepartmentScopeResolver departmentScopeResolver,
            DepartmentScopeProperties departmentScopeProperties,
            ObjectMapper objectMapper) {
        this.departmentScopeResolver = departmentScopeResolver;
        this.departmentScopeProperties = departmentScopeProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!shouldResolveScope(authentication)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!departmentScopeProperties.isScopingActive()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            DepartmentScope scope = departmentScopeResolver.resolve(authentication);
            DepartmentRequestScope.set(request, scope);
            filterChain.doFilter(request, response);
        } catch (ForbiddenException ex) {
            writeForbidden(response, ex.getMessage());
        } catch (UsernameNotFoundException ex) {
            logger.debug("Skipping department scope resolution for unknown authenticated user: {}", ex.getMessage());
            filterChain.doFilter(request, response);
        }
    }

    private boolean shouldResolveScope(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)
                && authentication.getName() != null;
    }

    private void writeForbidden(HttpServletResponse response, String message) throws IOException {
        if (response.isCommitted()) {
            return;
        }
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(Response.error(message)));
    }
}
