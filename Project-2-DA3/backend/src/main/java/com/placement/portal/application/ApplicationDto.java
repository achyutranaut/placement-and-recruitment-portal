package com.placement.portal.application;

import com.placement.portal.interview.InterviewDto;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ApplicationDto {
    private String applicationId;
    private String studentId;
    private String studentName;
    private String driveId;
    private String jobTitle;
    private String companyName;
    private LocalDate applyDate;
    private String status;
    private ApplicationScoresDto scores;
    private ScoreCompletionDto scoreCompletion;
    private List<InterviewDto> interviews = new ArrayList<>();

    public ApplicationDto() {}

    public ApplicationDto(String applicationId, String studentId, String studentName, String driveId, String jobTitle, String companyName, LocalDate applyDate, String status) {
        this.applicationId = applicationId;
        this.studentId = studentId;
        this.studentName = studentName;
        this.driveId = driveId;
        this.jobTitle = jobTitle;
        this.companyName = companyName;
        this.applyDate = applyDate;
        this.status = status;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getDriveId() {
        return driveId;
    }

    public void setDriveId(String driveId) {
        this.driveId = driveId;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public LocalDate getApplyDate() {
        return applyDate;
    }

    public void setApplyDate(LocalDate applyDate) {
        this.applyDate = applyDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public ApplicationScoresDto getScores() {
        return scores;
    }

    public void setScores(ApplicationScoresDto scores) {
        this.scores = scores;
    }

    public ScoreCompletionDto getScoreCompletion() {
        return scoreCompletion;
    }

    public void setScoreCompletion(ScoreCompletionDto scoreCompletion) {
        this.scoreCompletion = scoreCompletion;
    }

    public List<InterviewDto> getInterviews() {
        return interviews;
    }

    public void setInterviews(List<InterviewDto> interviews) {
        this.interviews = interviews;
    }

    public ApplicationScoresDto getInterviewPerformance() {
        return scores;
    }

    public void setInterviewPerformance(ApplicationScoresDto interviewPerformance) {
        this.scores = interviewPerformance;
    }

    public boolean isSelectionEligible() {
        return scoreCompletion != null && scoreCompletion.isSelectionEligible();
    }

    public String getReadinessReason() {
        return scoreCompletion != null ? scoreCompletion.getReadinessReason() : null;
    }
}

