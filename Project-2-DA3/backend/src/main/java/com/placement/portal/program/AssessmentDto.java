package com.placement.portal.program;

import java.math.BigDecimal;

public class AssessmentDto {
    private String assessmentId;
    private String programId;
    private String batchNo;
    private String title;
    private BigDecimal maxMarks;
    private String level;
    private String eligibilityCriteria;

    public AssessmentDto() {}

    public AssessmentDto(String assessmentId, String programId, String batchNo, String title, BigDecimal maxMarks, String level, String eligibilityCriteria) {
        this.assessmentId = assessmentId;
        this.programId = programId;
        this.batchNo = batchNo;
        this.title = title;
        this.maxMarks = maxMarks;
        this.level = level;
        this.eligibilityCriteria = eligibilityCriteria;
    }

    public static AssessmentDto fromEntity(Assessment a) {
        if (a == null) return null;
        String level = "Beginner";
        if ("Y".equalsIgnoreCase(a.getAdvanced())) level = "Advanced";
        else if ("Y".equalsIgnoreCase(a.getIntermediate())) level = "Intermediate";

        return new AssessmentDto(a.getAssessmentId(), a.getProgramId(), a.getBatchNo(), a.getTitle(), a.getMaxMarks(), level, a.getEligibilityCriteria());
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

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getEligibilityCriteria() {
        return eligibilityCriteria;
    }

    public void setEligibilityCriteria(String eligibilityCriteria) {
        this.eligibilityCriteria = eligibilityCriteria;
    }
}
