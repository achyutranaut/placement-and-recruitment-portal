package com.placement.portal.auth;

public class AuthResponse {
    private String token;
    private String username;
    private String role;
    private String referenceId;
    private String name;
    private String email;

    public AuthResponse() {}

    public AuthResponse(String token, String username, String role, String referenceId) {
        this.token = token;
        this.username = username;
        this.role = role;
        this.referenceId = referenceId;
    }

    public AuthResponse(String token, String username, String role, String referenceId, String name, String email) {
        this.token = token;
        this.username = username;
        this.role = role;
        this.referenceId = referenceId;
        this.name = name;
        this.email = email;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
