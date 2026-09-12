package com.placement.portal.auth;

import com.placement.portal.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Endpoints for user authentication and JWT generation")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "Login and obtain JWT token", description = "Authenticates user credentials and returns JWT bearer token with role claims")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok("Login successful", response));
    }

    @PostMapping("/register")
    @Operation(summary = "Register new portal user", description = "Creates a new user profile with encoded password")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(ApiResponse.ok("Registration successful", response));
    }

    @PostMapping("/student/register")
    @Operation(summary = "Register new student with VIT registration number validation", description = "Validates @vitstudent.ac.in and department registration format, creates student and user account")
    public ResponseEntity<ApiResponse<AuthResponse>> registerStudent(@Valid @RequestBody StudentRegisterRequest request) {
        AuthResponse response = authService.registerStudent(request);
        return ResponseEntity.ok(ApiResponse.ok("Student registration successful", response));
    }

    @PostMapping("/recruiter/register")
    @Operation(summary = "Register new recruiter account", description = "Registers a new corporate recruiter with company authorization. Validates company existence in Oracle.")
    public ResponseEntity<ApiResponse<RecruiterRegistrationResult>> registerRecruiter(@Valid @RequestBody RecruiterRegisterRequest request) {
        RecruiterRegistrationResult result = authService.registerRecruiter(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Recruiter registration successful", result));
    }
}
