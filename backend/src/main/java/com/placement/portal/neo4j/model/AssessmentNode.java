package com.placement.portal.neo4j.model;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

import java.math.BigDecimal;

@Node("Assessment")
public class AssessmentNode {

    @Id
    private String assessmentId;

    private String programId;
    private String batchNo;
    private String title;
    private BigDecimal maxMarks;
    private String eligibilityCriteria;
    private String beginner = "N";
    private String intermediate = "N";
    private String advanced = "N";

    public AssessmentNode() {}

    public AssessmentNode(String assessmentId, String programId, String batchNo, String title,
                          BigDecimal maxMarks, String eligibilityCriteria, String beginner, String intermediate, String advanced) {
        this.assessmentId = assessmentId;
        this.programId = programId;
        this.batchNo = batchNo;
        this.title = title;
        this.maxMarks = maxMarks;
        this.eligibilityCriteria = eligibilityCriteria;
        this.beginner = beginner;
        this.intermediate = intermediate;
        this.advanced = advanced;
    }

    public String getAssessmentId() { return assessmentId; }
    public void setAssessmentId(String assessmentId) { this.assessmentId = assessmentId; }

    public String getProgramId() { return programId; }
    public void setProgramId(String programId) { this.programId = programId; }

    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public BigDecimal getMaxMarks() { return maxMarks; }
    public void setMaxMarks(BigDecimal maxMarks) { this.maxMarks = maxMarks; }

    public String getEligibilityCriteria() { return eligibilityCriteria; }
    public void setEligibilityCriteria(String eligibilityCriteria) { this.eligibilityCriteria = eligibilityCriteria; }

    public String getBeginner() { return beginner; }
    public void setBeginner(String beginner) { this.beginner = beginner; }

    public String getIntermediate() { return intermediate; }
    public void setIntermediate(String intermediate) { this.intermediate = intermediate; }

    public String getAdvanced() { return advanced; }
    public void setAdvanced(String advanced) { this.advanced = advanced; }
}
