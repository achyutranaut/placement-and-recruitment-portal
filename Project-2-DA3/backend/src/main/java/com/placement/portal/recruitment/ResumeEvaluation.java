package com.placement.portal.recruitment;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "RESUME_EVALUATION")
public class ResumeEvaluation {

    @Id
    @Column(name = "Evaluation_Id", length = 50, nullable = false)
    private String evaluationId;

    @Column(name = "Resume_Id", length = 50, nullable = false)
    private String resumeId;

    @Column(name = "Application_Id", length = 20, nullable = false)
    private String applicationId;

    @Column(name = "Recruiter_Id", length = 50, nullable = false)
    private String recruiterId;

    @Column(name = "Technical_Score", nullable = false)
    private Integer technicalScore = 0;

    @Column(name = "Education_Score", nullable = false)
    private Integer educationScore = 0;

    @Column(name = "Project_Score", nullable = false)
    private Integer projectScore = 0;

    @Column(name = "Experience_Score", nullable = false)
    private Integer experienceScore = 0;

    @Column(name = "Overall_Score", nullable = false)
    private Integer overallScore = 0;

    @Column(name = "Comments", length = 1000)
    private String comments;

    @Column(name = "Evaluated_At", nullable = false)
    private LocalDateTime evaluatedAt = LocalDateTime.now();

    public ResumeEvaluation() {}

    public ResumeEvaluation(String evaluationId, String resumeId, String applicationId, String recruiterId,
                            Integer technicalScore, Integer educationScore, Integer projectScore,
                            Integer experienceScore, Integer overallScore, String comments) {
        this.evaluationId = evaluationId;
        this.resumeId = resumeId;
        this.applicationId = applicationId;
        this.recruiterId = recruiterId;
        this.technicalScore = technicalScore != null ? technicalScore : 0;
        this.educationScore = educationScore != null ? educationScore : 0;
        this.projectScore = projectScore != null ? projectScore : 0;
        this.experienceScore = experienceScore != null ? experienceScore : 0;
        this.overallScore = overallScore != null ? overallScore : 0;
        this.comments = comments;
        this.evaluatedAt = LocalDateTime.now();
    }

    public String getEvaluationId() { return evaluationId; }
    public void setEvaluationId(String evaluationId) { this.evaluationId = evaluationId; }

    public String getResumeId() { return resumeId; }
    public void setResumeId(String resumeId) { this.resumeId = resumeId; }

    public String getApplicationId() { return applicationId; }
    public void setApplicationId(String applicationId) { this.applicationId = applicationId; }

    public String getRecruiterId() { return recruiterId; }
    public void setRecruiterId(String recruiterId) { this.recruiterId = recruiterId; }

    public Integer getTechnicalScore() { return technicalScore; }
    public void setTechnicalScore(Integer technicalScore) { this.technicalScore = technicalScore; }

    public Integer getEducationScore() { return educationScore; }
    public void setEducationScore(Integer educationScore) { this.educationScore = educationScore; }

    public Integer getProjectScore() { return projectScore; }
    public void setProjectScore(Integer projectScore) { this.projectScore = projectScore; }

    public Integer getExperienceScore() { return experienceScore; }
    public void setExperienceScore(Integer experienceScore) { this.experienceScore = experienceScore; }

    public Integer getOverallScore() { return overallScore; }
    public void setOverallScore(Integer overallScore) { this.overallScore = overallScore; }

    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }

    public LocalDateTime getEvaluatedAt() { return evaluatedAt; }
    public void setEvaluatedAt(LocalDateTime evaluatedAt) { this.evaluatedAt = evaluatedAt; }
}
