package com.placement.portal.admin;

import java.time.LocalDateTime;

public class AuditLogDto {
    private String auditId;
    private String applicationId;
    private String oldStatus;
    private String newStatus;
    private LocalDateTime changedAt;
    private String changedBy;
    private String triggerSource = "DATABASE_TRIGGER";

    public AuditLogDto() {}

    public AuditLogDto(Long auditId, String applicationId, String oldStatus, String newStatus, LocalDateTime changedAt, String changedBy) {
        this.auditId = auditId != null ? String.valueOf(auditId) : null;
        this.applicationId = applicationId;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.changedAt = changedAt;
        this.changedBy = changedBy;
        this.triggerSource = "DATABASE_TRIGGER";
    }

    public AuditLogDto(String auditId, String applicationId, String oldStatus, String newStatus, LocalDateTime changedAt, String changedBy, String triggerSource) {
        this.auditId = auditId;
        this.applicationId = applicationId;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.changedAt = changedAt;
        this.changedBy = changedBy;
        this.triggerSource = triggerSource != null ? triggerSource : "DATABASE_TRIGGER";
    }

    public String getAuditId() { return auditId; }
    public void setAuditId(String auditId) { this.auditId = auditId; }
    public String getApplicationId() { return applicationId; }
    public void setApplicationId(String applicationId) { this.applicationId = applicationId; }
    public String getOldStatus() { return oldStatus; }
    public void setOldStatus(String oldStatus) { this.oldStatus = oldStatus; }
    public String getNewStatus() { return newStatus; }
    public void setNewStatus(String newStatus) { this.newStatus = newStatus; }
    public LocalDateTime getChangedAt() { return changedAt; }
    public void setChangedAt(LocalDateTime changedAt) { this.changedAt = changedAt; }
    public String getChangedBy() { return changedBy; }
    public void setChangedBy(String changedBy) { this.changedBy = changedBy; }
    public String getTriggerSource() { return triggerSource; }
    public void setTriggerSource(String triggerSource) { this.triggerSource = triggerSource; }
}
