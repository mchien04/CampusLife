package vn.campuslife.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "12345678901234567890123456789012");
        ReflectionTestUtils.setField(jwtUtil, "expiration", 86400000L);
    }

    @Test
    void generateToken_IncludesIdentityAndRoleButNoDepartmentScope() {
        User userDetails = new User(
                "manager_a",
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_MANAGER")));

        String token = jwtUtil.generateToken(userDetails);

        assertEquals("manager_a", jwtUtil.extractUsername(token));
        assertEquals("MANAGER", jwtUtil.extractRole(token));
        assertNull(jwtUtil.extractClaim(token, claims -> claims.get("dept_ids")));
    }
}
