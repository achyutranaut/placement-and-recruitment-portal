package com.placement.portal.program;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "ASSESSMENT")
public class Assessment {

    @Id
    @Column(name = "Assessment_Id", length = 20, nullable = false)
    private String assessmentId;

    @Column(name = "Program_Id", length = 20, nullable = false)
    private String programId;

    @Column(name = "Batch_No", length = 20, nullable = false)
    private String batchNo;

    @Column(name = "Title", length = 100, nullable = false)
    private String title;

    @Column(name = "Max_Marks", precision = 5, scale = 2, nullable = false)
    private BigDecimal maxMarks;

    @Column(name = "Beginner", length = 1, nullable = false)
    private String beginner = "N";

    @Column(name = "Intermediate", length = 1, nullable = false)
    private String intermediate = "N";

    @Column(name = "Advanced", length = 1, nullable = false)
    private String advanced = "N";

    @Column(name = "Eligibility_Criteria", length = 200)
    private String eligibilityCriteria;

    public Assessment() {}

    public Assessment(String assessmentId, String programId, String batchNo, String title, BigDecimal maxMarks, String beginner, String intermediate, String advanced, String eligibilityCriteria) {
        this.assessmentId = assessmentId;
        this.programId = programId;
        this.batchNo = batchNo;
        this.title = title;
        this.maxMarks = maxMarks;
        this.beginner = beginner;
        this.intermediate = intermediate;
        this.advanced = advanced;
        this.eligibilityCriteria = eligibilityCriteria;
    }

    public String getAssessmentId() {
        return assessmentId;
    }

    public void setAssessmentId(String assessmentId) {
        this.assessmentId = assessmentId;
    }

    public String getProgramId() {
        return programId;
    }

    public void setProgramId(String programId) {
        this.programId = programId;
    }

    public String getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(String batchNo) {
        this.batchNo = batchNo;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public BigDecimal getMaxMarks() {
        return maxMarks;
    }

    public void setMaxMarks(BigDecimal maxMarks) {
        this.maxMarks = maxMarks;
    }

    public String getBeginner() {
        return beginner;
    }

    public void setBeginner(String beginner) {
        this.beginner = beginner;
    }

    public String getIntermediate() {
        return intermediate;
    }

    public void setIntermediate(String intermediate) {
        this.intermediate = intermediate;
    }

    public String getAdvanced() {
        return advanced;
    }

    public void setAdvanced(String advanced) {
        this.advanced = advanced;
    }

    public String getEligibilityCriteria() {
        return eligibilityCriteria;
    }

    public void setEligibilityCriteria(String eligibilityCriteria) {
        this.eligibilityCriteria = eligibilityCriteria;
    }
}
