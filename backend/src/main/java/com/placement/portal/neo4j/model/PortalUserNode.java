package com.placement.portal.neo4j.model;

import com.placement.portal.auth.Role;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Node("PortalUser")
public class PortalUserNode {

    @Id
    private String userId;

    private String username;
    private String passwordHash;
    private Role role;
    private String referenceId;
    private String fullName;
    private String email;
    private LocalDateTime createdAt = LocalDateTime.now();

// authorizedCompanies mapped via Neo4jCompanyRepository.findAuthorizedCompaniesForUser

    public PortalUserNode() {}

    public PortalUserNode(String userId, String username, String passwordHash, Role role, String referenceId) {
        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.referenceId = referenceId;
        this.createdAt = LocalDateTime.now();
    }

    public PortalUserNode(String userId, String username, String passwordHash, Role role, String referenceId, String fullName, String email) {
        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.referenceId = referenceId;
        this.fullName = fullName;
        this.email = email;
        this.createdAt = LocalDateTime.now();
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public String getReferenceId() { return referenceId; }
    public void setReferenceId(String referenceId) { this.referenceId = referenceId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }


}
