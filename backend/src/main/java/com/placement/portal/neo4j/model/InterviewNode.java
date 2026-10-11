package com.placement.portal.neo4j.model;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.math.BigDecimal;

@Node("Interview")
public class InterviewNode {

    @Id
    private String interviewId; // applicationId + "_" + interviewerName

    private String applicationId;
    private String interviewerName;
    private Integer roundNo;
    private BigDecimal oa;
    private BigDecimal gd;
    private BigDecimal hr;
    private String result = "PENDING"; // PENDING, CLEARED, PASSED, FAILED, REJECTED
    private String online = "Y";
    private String offline = "N";

    @Relationship(type = "CONDUCTED_BY", direction = Relationship.Direction.OUTGOING)
    private InterviewerNode interviewer;

    public InterviewNode() {}

    public InterviewNode(String applicationId, String interviewerName, Integer roundNo,
                         BigDecimal oa, BigDecimal gd, BigDecimal hr, String result, String online, String offline) {
        this.interviewId = applicationId + "_" + interviewerName;
        this.applicationId = applicationId;
        this.interviewerName = interviewerName;
        this.roundNo = roundNo;
        this.oa = oa;
        this.gd = gd;
        this.hr = hr;
        this.result = result;
        this.online = online;
        this.offline = offline;
    }

    public String getInterviewId() { return interviewId; }
    public void setInterviewId(String interviewId) { this.interviewId = interviewId; }

    public String getApplicationId() { return applicationId; }
    public void setApplicationId(String applicationId) { this.applicationId = applicationId; }

    public String getInterviewerName() { return interviewerName; }
    public void setInterviewerName(String interviewerName) { this.interviewerName = interviewerName; }

    public Integer getRoundNo() { return roundNo; }
    public void setRoundNo(Integer roundNo) { this.roundNo = roundNo; }

    public BigDecimal getOa() { return oa; }
    public void setOa(BigDecimal oa) { this.oa = oa; }

    public BigDecimal getGd() { return gd; }
    public void setGd(BigDecimal gd) { this.gd = gd; }

    public BigDecimal getHr() { return hr; }
    public void setHr(BigDecimal hr) { this.hr = hr; }

    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }

    public String getOnline() { return online; }
    public void setOnline(String online) { this.online = online; }

    public String getOffline() { return offline; }
    public void setOffline(String offline) { this.offline = offline; }

    public InterviewerNode getInterviewer() { return interviewer; }
    public void setInterviewer(InterviewerNode interviewer) { this.interviewer = interviewer; }
}
