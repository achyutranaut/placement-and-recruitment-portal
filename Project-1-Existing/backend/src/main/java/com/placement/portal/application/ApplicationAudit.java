package com.placement.portal.application;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "APPLICATION_AUDIT")
public class ApplicationAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Audit_Id")
    private Long auditId;

    @Column(name = "Application_Id", length = 20, nullable = false)
    private String applicationId;

    @Column(name = "Old_Status", length = 30)
    private String oldStatus;

    @Column(name = "New_Status", length = 30, nullable = false)
    private String newStatus;

    @Column(name = "Changed_At", nullable = false)
    private LocalDateTime changedAt = LocalDateTime.now();

    @Column(name = "Changed_By", length = 50, nullable = false)
    private String changedBy = "SYSTEM";

    public ApplicationAudit() {}

    public ApplicationAudit(String applicationId, String oldStatus, String newStatus, String changedBy) {
        this.applicationId = applicationId;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.changedBy = changedBy;
        this.changedAt = LocalDateTime.now();
    }

    public Long getAuditId() {
        return auditId;
    }

    public void setAuditId(Long auditId) {
        this.auditId = auditId;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public String getOldStatus() {
        return oldStatus;
    }

    public void setOldStatus(String oldStatus) {
        this.oldStatus = oldStatus;
    }

    public String getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(String newStatus) {
        this.newStatus = newStatus;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(LocalDateTime changedAt) {
        this.changedAt = changedAt;
    }

    public String getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(String changedBy) {
        this.changedBy = changedBy;
    }
}
