package com.placement.portal.company;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Junction entity mapping recruiters (PORTAL_USER) to authorized companies.
 * A recruiter may be authorized for multiple companies.
 */
@Entity
@Table(name = "RECRUITER_COMPANY")
@IdClass(RecruiterCompanyId.class)
public class RecruiterCompany {

    @Id
    @Column(name = "User_Id", length = 50, nullable = false)
    private String userId;

    @Id
    @Column(name = "Company_Id", length = 20, nullable = false)
    private String companyId;

    @Column(name = "Authorized_At", nullable = false)
    private LocalDateTime authorizedAt = LocalDateTime.now();

    public RecruiterCompany() {}

    public RecruiterCompany(String userId, String companyId) {
        this.userId = userId;
        this.companyId = companyId;
        this.authorizedAt = LocalDateTime.now();
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getCompanyId() { return companyId; }
    public void setCompanyId(String companyId) { this.companyId = companyId; }

    public LocalDateTime getAuthorizedAt() { return authorizedAt; }
    public void setAuthorizedAt(LocalDateTime authorizedAt) { this.authorizedAt = authorizedAt; }
}
