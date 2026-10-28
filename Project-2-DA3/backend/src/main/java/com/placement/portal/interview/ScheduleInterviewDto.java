package com.placement.portal.interview;

import com.fasterxml.jackson.annotation.JsonSetter;
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
        setOnline(online);
        setOffline(offline);
    }

    public static String normalizeFlag(Object val, String defaultVal) {
        if (val == null) return defaultVal;
        String s = val.toString().trim();
        if (s.equalsIgnoreCase("true") || s.equalsIgnoreCase("y") || s.equalsIgnoreCase("yes") || s.equalsIgnoreCase("online") || s.equals("1")) {
            return "Y";
        }
        if (s.equalsIgnoreCase("false") || s.equalsIgnoreCase("n") || s.equalsIgnoreCase("no") || s.equalsIgnoreCase("offline") || s.equals("0")) {
            return "N";
        }
        return defaultVal;
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

    @JsonSetter
    public void setOnline(Object online) {
        this.online = normalizeFlag(online, "Y");
    }

    public String getOffline() {
        return offline;
    }

    @JsonSetter
    public void setOffline(Object offline) {
        this.offline = normalizeFlag(offline, "N");
    }
}
