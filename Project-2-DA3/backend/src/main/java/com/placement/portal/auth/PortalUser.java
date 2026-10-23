package com.placement.portal.auth;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "PORTAL_USER")
public class PortalUser {

    @Id
    @Column(name = "User_Id", length = 50, nullable = false)
    private String userId;

    @Column(name = "Username", length = 50, nullable = false, unique = true)
    private String username;

    @Column(name = "Password_Hash", length = 255, nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "Role", length = 30, nullable = false)
    private Role role;

    @Column(name = "Reference_Id", length = 50)
    private String referenceId; // Student_Id or Company_Id or ADMIN

    @Column(name = "Full_Name", length = 100)
    private String fullName;

    @Column(name = "Email", length = 100)
    private String email;

    @Column(name = "Created_At", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public PortalUser() {}

    public PortalUser(String userId, String username, String passwordHash, Role role, String referenceId) {
        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.referenceId = referenceId;
        this.createdAt = LocalDateTime.now();
    }

    public PortalUser(String userId, String username, String passwordHash, Role role, String referenceId, String fullName, String email) {
        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.referenceId = referenceId;
        this.fullName = fullName;
        this.email = email;
        this.createdAt = LocalDateTime.now();
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
