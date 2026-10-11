package com.placement.portal.neo4j.model;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Node("Application")
public class ApplicationNode {

    @Id
    private String applicationId;

    private String studentId;
    private String driveId;
    private LocalDate applyDate;
    private String status = "APPLIED";

// student and drive mapped via IDs and traversal queries

    @Relationship(type = "HAS_INTERVIEW", direction = Relationship.Direction.OUTGOING)
    private List<InterviewNode> interviews = new ArrayList<>();

    @Relationship(type = "RECEIVED_OFFER", direction = Relationship.Direction.OUTGOING)
    private OfferNode offer;

    @Relationship(type = "HAS_AUDIT", direction = Relationship.Direction.OUTGOING)
    private List<AuditEventNode> auditEvents = new ArrayList<>();

    public ApplicationNode() {}

    public ApplicationNode(String applicationId, String studentId, String driveId, LocalDate applyDate, String status) {
        this.applicationId = applicationId;
        this.studentId = studentId;
        this.driveId = driveId;
        this.applyDate = applyDate;
        this.status = status;
    }

    public String getApplicationId() { return applicationId; }
    public void setApplicationId(String applicationId) { this.applicationId = applicationId; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getDriveId() { return driveId; }
    public void setDriveId(String driveId) { this.driveId = driveId; }

    public LocalDate getApplyDate() { return applyDate; }
    public void setApplyDate(LocalDate applyDate) { this.applyDate = applyDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }



    public List<InterviewNode> getInterviews() { return interviews; }
    public void setInterviews(List<InterviewNode> interviews) { this.interviews = interviews; }

    public OfferNode getOffer() { return offer; }
    public void setOffer(OfferNode offer) { this.offer = offer; }

    public List<AuditEventNode> getAuditEvents() { return auditEvents; }
    public void setAuditEvents(List<AuditEventNode> auditEvents) { this.auditEvents = auditEvents; }
}
