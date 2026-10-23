package com.placement.portal.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.placement.portal.company.AdminRecruiterController;
import com.placement.portal.company.CompanyService;
import com.placement.portal.config.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
public class SecurityPhase1RegressionTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private LoginAttemptService loginAttemptService;

    @Autowired
    private CompanyService companyService;

    @BeforeEach
    void setUp() {
        loginAttemptService.reset();
    }

    @Test
    @DisplayName("S-01: Public registration requesting ROLE_ADMIN is rejected")
    void testPublicRegistrationAsAdminBlocked() throws Exception {
        RegisterRequest req = new RegisterRequest("forged_admin", "Password123!", Role.ROLE_ADMIN, "ADMIN01");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());

        assertFalse(userRepository.existsByUsername("forged_admin"), "Forged admin user must not be persisted in database");
    }

    @Test
    @DisplayName("S-01: Public registration attempting to claim arbitrary referenceId is rejected")
    void testPublicRegistrationArbitraryReferenceIdBlocked() throws Exception {
        // Attempt to hijack student STU001 which is already associated with student1
        RegisterRequest req = new RegisterRequest("impersonator", "Password123!", Role.ROLE_STUDENT, "STU001");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());

        assertFalse(userRepository.existsByUsername("impersonator"), "Impersonator user must not be persisted");
    }

    @Test
    @DisplayName("S-01: Public registration with non-existent referenceId is rejected")
    void testPublicRegistrationNonExistentReferenceIdBlocked() throws Exception {
        RegisterRequest req = new RegisterRequest("ghost_student", "Password123!", Role.ROLE_STUDENT, "STU999_NON_EXISTENT");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("S-01: Valid student registration succeeds")
    void testValidStudentRegistrationAllowed() {
        RegisterRequest req = new RegisterRequest("legit_student_user", "Password123!", Role.ROLE_STUDENT, null);
        AuthResponse resp = authService.register(req);

        assertNotNull(resp);
        assertEquals("legit_student_user", resp.getUsername());
        assertEquals("ROLE_STUDENT", resp.getRole());
        assertNotNull(resp.getToken());
    }

    @Test
    @DisplayName("S-04: Recruiter self-registration results in PENDING_APPROVAL and blocks company drive access")
    void testRecruiterRegistrationPendingApproval() throws Exception {
        RecruiterRegisterRequest req = new RecruiterRegisterRequest(
                "pending_recruiter",
                "Password123!",
                "Pending Recruiter Name",
                "pending@techcorp.com",
                "COM001"
        );

        RecruiterRegistrationResult regResult = authService.registerRecruiter(req);
        assertNotNull(regResult);
        assertEquals("pending_recruiter", regResult.getUsername());
        assertNotNull(regResult.getToken());

        // Attempting to access candidate applications for COM001 (DRV001/DRV002/DRV003) must be 403 Forbidden
        mockMvc.perform(get("/api/v1/applications/drive/DRV001")
                        .header("Authorization", "Bearer " + regResult.getToken()))
                .andExpect(status().isForbidden());

        // Verify that recruiter is not authorized for COM001 until admin approves
        assertFalse(companyService.isRecruiterAuthorizedForCompany("pending_recruiter", "COM001"));
    }

    @Test
    @DisplayName("S-06: JWT authentication via URL query parameter '?token=' is rejected")
    void testQueryParamTokenRejected() throws Exception {
        String token = tokenProvider.generateToken("student1", "ROLE_STUDENT", "STU001");

        // Request with query parameter instead of Authorization header must be 403 Forbidden
        mockMvc.perform(get("/api/v1/resumes/student/STU001").param("token", token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("S-06: JWT authentication via Authorization Bearer header succeeds")
    void testBearerHeaderTokenAccepted() throws Exception {
        String token = tokenProvider.generateToken("student1", "ROLE_STUDENT", "STU001");

        mockMvc.perform(get("/api/v1/resumes/student/STU001")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("S-08: Five consecutive failed logins trigger temporary lockout")
    void testLoginRateLimitingLockout() {
        LoginRequest badReq = new LoginRequest("student1", "wrong_password_attempt");

        for (int i = 0; i < 5; i++) {
            assertThrows(BadCredentialsException.class, () -> authService.login(badReq));
        }

        // 6th attempt must be blocked by rate limiter
        BadCredentialsException ex = assertThrows(BadCredentialsException.class, () -> authService.login(badReq));
        assertTrue(ex.getMessage().contains("Too many failed login attempts") || ex.getMessage().contains("locked"));
    }

    @Test
    @DisplayName("S-05: JwtTokenProvider rejects missing or short keys")
    void testJwtSecretValidation() {
        assertThrows(IllegalStateException.class, () -> new JwtTokenProvider("", 86400000L));
        assertThrows(IllegalStateException.class, () -> new JwtTokenProvider(null, 86400000L));
        assertThrows(IllegalStateException.class, () -> new JwtTokenProvider("short_secret_under_32_bytes", 86400000L));
    }
}
