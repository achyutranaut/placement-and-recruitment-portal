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

    @Autowired
    private UserRepository userRepository;

    @Test
    void testStudentRegisterPopulatesFullNameAndEmail() {
        StudentRegisterRequest req = new StudentRegisterRequest(
                "Aarav Sharma",
                "aarav.sharma2025@vitstudent.ac.in",
                "25BCE9999",
                "B.Tech CSE",
                "BTECH-CSE",
                "StrongPassword123!",
                java.math.BigDecimal.valueOf(9.45),
                java.time.LocalDate.of(2003, 5, 15),
                "+91 9876543210",
                "Hostel Block D",
                "Vellore",
                "Tamil Nadu",
                java.util.List.of("Java", "Spring Boot")
        );

        AuthResponse res = authService.registerStudent(req);

        assertNotNull(res);
        assertNotNull(res.getToken());
        assertEquals("25bce9999", res.getUsername());
        assertEquals("ROLE_STUDENT", res.getRole());
        assertEquals("Aarav Sharma", res.getName());
        assertEquals("aarav.sharma2025@vitstudent.ac.in", res.getEmail());

        PortalUser user = userRepository.findByUsername("25bce9999").orElseThrow();
        assertEquals("Aarav Sharma", user.getFullName());
        assertEquals("aarav.sharma2025@vitstudent.ac.in", user.getEmail());
    }

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
