package com.placement.portal.neo4j.model;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.time.LocalDateTime;

@Node("ResumeEvaluation")
public class ResumeEvaluationNode {

    @Id
    private String evaluationId;

    private String resumeId;
    private String applicationId;
    private String recruiterId;
    private Integer technicalScore;
    private Integer educationScore;
    private Integer projectScore;
    private Integer experienceScore;
    private Integer overallScore;
    private String comments;
    private LocalDateTime evaluatedAt = LocalDateTime.now();

// application mapped via applicationId

    public ResumeEvaluationNode() {}

    public ResumeEvaluationNode(String evaluationId, String resumeId, String applicationId, String recruiterId,
                                Integer technicalScore, Integer educationScore, Integer projectScore,
                                Integer experienceScore, Integer overallScore, String comments) {
        this.evaluationId = evaluationId;
        this.resumeId = resumeId;
        this.applicationId = applicationId;
        this.recruiterId = recruiterId;
        this.technicalScore = technicalScore;
        this.educationScore = educationScore;
        this.projectScore = projectScore;
        this.experienceScore = experienceScore;
        this.overallScore = overallScore;
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
