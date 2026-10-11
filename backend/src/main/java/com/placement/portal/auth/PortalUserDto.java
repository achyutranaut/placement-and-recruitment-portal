package com.placement.portal.auth;

public class PortalUserDto {
    private String userId;
    private String username;
    private Role role;
    private String referenceId;
    private String fullName;
    private String email;

    public PortalUserDto() {}

    public PortalUserDto(String userId, String username, Role role, String referenceId, String fullName, String email) {
        this.userId = userId;
        this.username = username;
        this.role = role;
        this.referenceId = referenceId;
        this.fullName = fullName;
        this.email = email;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public String getReferenceId() { return referenceId; }
    public void setReferenceId(String referenceId) { this.referenceId = referenceId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
