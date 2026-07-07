package vn.campuslife.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import vn.campuslife.exception.ForbiddenException;
import vn.campuslife.security.department.DepartmentRequestScope;
import vn.campuslife.security.department.DepartmentScope;
import vn.campuslife.security.department.DepartmentScopeResolver;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepartmentContextFilterTest {

    @Mock
    private DepartmentScopeResolver resolver;

    @Mock
    private FilterChain filterChain;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_AuthenticatedRequest_AttachesDepartmentScope() throws Exception {
        DepartmentContextFilter filter = new DepartmentContextFilter(resolver, new ObjectMapper());
        UsernamePasswordAuthenticationToken authentication = authentication("manager", "ROLE_MANAGER");
        DepartmentScope scope = DepartmentScope.manager(Set.of(1L, 2L));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        when(resolver.resolve(authentication)).thenReturn(scope);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/students");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertSame(scope, request.getAttribute(DepartmentRequestScope.ATTRIBUTE_NAME));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_UnassignedManager_WritesForbiddenResponse() throws Exception {
        DepartmentContextFilter filter = new DepartmentContextFilter(resolver, new ObjectMapper());
        UsernamePasswordAuthenticationToken authentication = authentication("manager", "ROLE_MANAGER");
        SecurityContextHolder.getContext().setAuthentication(authentication);
        when(resolver.resolve(authentication))
                .thenThrow(new ForbiddenException(DepartmentScopeResolver.MANAGER_UNASSIGNED_MESSAGE));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/students");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains(DepartmentScopeResolver.MANAGER_UNASSIGNED_MESSAGE));
        verify(filterChain, never()).doFilter(request, response);
    }

    private UsernamePasswordAuthenticationToken authentication(String username, String role) {
        return new UsernamePasswordAuthenticationToken(
                username,
                null,
                List.of(new SimpleGrantedAuthority(role)));
    }
}
