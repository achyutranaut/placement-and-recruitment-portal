package com.placement.portal.interview;

import java.math.BigDecimal;
import java.time.LocalDate;

public class InterviewDto {
    private String applicationId;
    private String interviewerName;
    private Integer roundNo;
    private String roundName;
    private BigDecimal score;
    private String result;
    private String mode; // Online / Offline
    private LocalDate interviewDate;
    private BigDecimal oa;
    private BigDecimal gd;
    private BigDecimal hr;
    private BigDecimal oaScore;
    private BigDecimal gdScore;
    private BigDecimal hrScore;
    private String online;
    private String offline;

    public InterviewDto() {}

    public InterviewDto(String applicationId, String interviewerName, Integer roundNo, BigDecimal oa, BigDecimal gd, BigDecimal hr, String result, String online, String offline) {
        this.applicationId = applicationId;
        this.interviewerName = interviewerName;
        this.roundNo = roundNo;
        this.oa = oa;
        this.gd = gd;
        this.hr = hr;
        this.result = result;
        this.online = online;
        this.offline = offline;

        // Determine specific round name and single round score
        if (roundNo != null && roundNo == 1) {
            this.roundName = "OA (Online Assessment)";
            this.score = oa;
        } else if (roundNo != null && roundNo == 2) {
            this.roundName = "GD (Group Discussion)";
            this.score = gd;
        } else if (roundNo != null && roundNo == 3) {
            this.roundName = "HR (Leadership & Culture)";
            this.score = hr;
        } else {
            this.roundName = "Interview Round " + roundNo;
            this.score = oa != null ? oa : (gd != null ? gd : hr);
        }

        this.oaScore = oa != null ? oa : (roundNo != null && roundNo == 1 ? this.score : null);
        this.gdScore = gd != null ? gd : (roundNo != null && roundNo == 2 ? this.score : null);
        this.hrScore = hr != null ? hr : (roundNo != null && roundNo == 3 ? this.score : null);

        this.mode = "Y".equalsIgnoreCase(online) ? "Online" : "Offline";
        this.interviewDate = LocalDate.now();
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

    public Integer getRoundNo() {
        return roundNo;
    }

    public void setRoundNo(Integer roundNo) {
        this.roundNo = roundNo;
    }

    public String getRoundName() {
        return roundName;
    }

    public void setRoundName(String roundName) {
        this.roundName = roundName;
    }

    public BigDecimal getScore() {
        return score;
    }

    public void setScore(BigDecimal score) {
        this.score = score;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public LocalDate getInterviewDate() {
        return interviewDate;
    }

    public void setInterviewDate(LocalDate interviewDate) {
        this.interviewDate = interviewDate;
    }

    public BigDecimal getOa() {
        return oa;
    }

    public void setOa(BigDecimal oa) {
        this.oa = oa;
    }

    public BigDecimal getGd() {
        return gd;
    }

    public void setGd(BigDecimal gd) {
        this.gd = gd;
    }

    public BigDecimal getHr() {
        return hr;
    }

    public void setHr(BigDecimal hr) {
        this.hr = hr;
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

    public BigDecimal getOaScore() {
        return oaScore != null ? oaScore : oa;
    }

    public void setOaScore(BigDecimal oaScore) {
        this.oaScore = oaScore;
    }

    public BigDecimal getGdScore() {
        return gdScore != null ? gdScore : gd;
    }

    public void setGdScore(BigDecimal gdScore) {
        this.gdScore = gdScore;
    }

    public BigDecimal getHrScore() {
        return hrScore != null ? hrScore : hr;
    }

    public void setHrScore(BigDecimal hrScore) {
        this.hrScore = hrScore;
    }
}

