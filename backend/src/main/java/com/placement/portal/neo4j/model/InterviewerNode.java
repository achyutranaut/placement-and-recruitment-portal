package com.placement.portal.neo4j.model;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

@Node("Interviewer")
public class InterviewerNode {

    @Id
    private String interviewerName;

    private Integer interviewRoundNo;

    public InterviewerNode() {}

    public InterviewerNode(String interviewerName, Integer interviewRoundNo) {
        this.interviewerName = interviewerName;
        this.interviewRoundNo = interviewRoundNo;
    }

    public String getInterviewerName() { return interviewerName; }
    public void setInterviewerName(String interviewerName) { this.interviewerName = interviewerName; }

    public Integer getInterviewRoundNo() { return interviewRoundNo; }
    public void setInterviewRoundNo(Integer interviewRoundNo) { this.interviewRoundNo = interviewRoundNo; }
}
