package com.placement.portal.neo4j.model;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

import java.time.LocalDateTime;

@Node("AuditEvent")
public class AuditEventNode {

    @Id
    private String auditId;

    private String applicationId;
    private String oldStatus;
    private String newStatus;
    private LocalDateTime changedAt = LocalDateTime.now();
    private String changedBy = "SYSTEM";
    private String triggerSource = "GRAPH_ENGINE";

    public AuditEventNode() {}

    public AuditEventNode(String auditId, String applicationId, String oldStatus, String newStatus, String changedBy) {
        this(auditId, applicationId, oldStatus, newStatus, changedBy, "GRAPH_ENGINE");
    }

    public AuditEventNode(String auditId, String applicationId, String oldStatus, String newStatus, String changedBy, String triggerSource) {
        this.auditId = auditId;
        this.applicationId = applicationId;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.changedAt = LocalDateTime.now();
        this.changedBy = changedBy != null ? changedBy : "SYSTEM";
        this.triggerSource = triggerSource != null ? triggerSource : "GRAPH_ENGINE";
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
