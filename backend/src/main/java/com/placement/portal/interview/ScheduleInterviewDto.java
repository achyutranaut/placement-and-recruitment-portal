package com.placement.portal.interview;

import jakarta.validation.constraints.NotBlank;

public class ScheduleInterviewDto {
    @NotBlank(message = "Application ID is required")
    private String applicationId;

    @NotBlank(message = "Interviewer Name is required")
    private String interviewerName;

    private String online = "Y";
    private String offline = "N";

    public ScheduleInterviewDto() {}

    public ScheduleInterviewDto(String applicationId, String interviewerName, String online, String offline) {
        this.applicationId = applicationId;
        this.interviewerName = interviewerName;
        this.online = online;
        this.offline = offline;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public String getInterviewerName() {
        return interviewerName;
    }

    public void setInterviewerName(String interviewerName) {
        this.interviewerName = interviewerName;
    }

    public String getOnline() {
        return online;
    }

    public void setOnline(String online) {
        this.online = online;
    }

    public String getOffline() {
        return offline;
    }

    public void setOffline(String offline) {
        this.offline = offline;
    }
}
