package com.placement.portal.auth;

public class RecruiterRegistrationResult {
    private String recruiterId;
    private String username;
    private String fullName;
    private String email;
    private String companyId;
    private String companyName;
    private String token;
    private String role;

    public RecruiterRegistrationResult() {}

    public RecruiterRegistrationResult(String recruiterId, String username, String fullName, String email, String companyId, String companyName, String token, String role) {
        this.recruiterId = recruiterId;
        this.username = username;
        this.fullName = fullName;
        this.email = email;
        this.companyId = companyId;
        this.companyName = companyName;
        this.token = token;
        this.role = role;
    }

    public String getRecruiterId() {
        return recruiterId;
    }

    public void setRecruiterId(String recruiterId) {
        this.recruiterId = recruiterId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCompanyId() {
        return companyId;
    }

    public void setCompanyId(String companyId) {
        this.companyId = companyId;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
