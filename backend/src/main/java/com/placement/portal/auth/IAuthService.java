package com.placement.portal.auth;

public interface IAuthService {
    AuthResponse login(LoginRequest request);
    AuthResponse register(RegisterRequest request);
    AuthResponse registerStudent(StudentRegisterRequest request);
    RecruiterRegistrationResult registerRecruiter(RecruiterRegisterRequest request);
}
