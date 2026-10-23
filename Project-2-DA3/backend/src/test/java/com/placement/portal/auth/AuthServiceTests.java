package com.placement.portal.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("h2")
class AuthServiceTests {

    @Autowired
    private AuthService authService;

    @Test
    void testAdminLoginSuccess() {
        LoginRequest req = new LoginRequest("admin", "admin123");
        AuthResponse res = authService.login(req);

        assertNotNull(res);
        assertNotNull(res.getToken());
        assertEquals("admin", res.getUsername());
        assertEquals("ROLE_ADMIN", res.getRole());
    }

    @Test
    void testStudentLoginSuccess() {
        LoginRequest req = new LoginRequest("student1", "password123");
        AuthResponse res = authService.login(req);

        assertNotNull(res);
        assertNotNull(res.getToken());
        assertEquals("student1", res.getUsername());
        assertEquals("ROLE_STUDENT", res.getRole());
        assertEquals("STU001", res.getReferenceId());
    }

    @Test
    void testInvalidPasswordFailure() {
        LoginRequest req = new LoginRequest("student1", "wrongpassword");
        assertThrows(BadCredentialsException.class, () -> authService.login(req));
    }
}
