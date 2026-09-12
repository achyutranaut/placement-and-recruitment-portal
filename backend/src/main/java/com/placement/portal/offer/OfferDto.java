package com.placement.portal.offer;

import java.math.BigDecimal;
import java.time.LocalDate;

public class OfferDto {
    private String offerId;
    private String applicationId;
    private String studentId;
    private String studentName;
    private String jobTitle;
    private String companyName;
    private LocalDate offerDate;
    private BigDecimal ctcLpa;
    private String status = "OFFERED";
    private java.time.LocalDateTime acceptedAt;
    private String driveId;

    public OfferDto() {}

    public OfferDto(String offerId, String applicationId, String studentId, String studentName, String jobTitle, String companyName, LocalDate offerDate, BigDecimal ctcLpa) {
        this.offerId = offerId;
        this.applicationId = applicationId;
        this.studentId = studentId;
        this.studentName = studentName;
        this.jobTitle = jobTitle;
        this.companyName = companyName;
        this.offerDate = offerDate;
        this.ctcLpa = ctcLpa;
        this.status = "OFFERED";
    }

    public OfferDto(String offerId, String applicationId, String studentId, String studentName, String jobTitle, String companyName, LocalDate offerDate, BigDecimal ctcLpa, String status, java.time.LocalDateTime acceptedAt, String driveId) {
        this.offerId = offerId;
        this.applicationId = applicationId;
        this.studentId = studentId;
        this.studentName = studentName;
        this.jobTitle = jobTitle;
        this.companyName = companyName;
        this.offerDate = offerDate;
        this.ctcLpa = ctcLpa;
        this.status = status != null ? status : "OFFERED";
        this.acceptedAt = acceptedAt;
        this.driveId = driveId;
    }

    public String getOfferId() {
        return offerId;
    }

    public void setOfferId(String offerId) {
        this.offerId = offerId;
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

    public LocalDate getOfferDate() {
        return offerDate;
    }

    public void setOfferDate(LocalDate offerDate) {
        this.offerDate = offerDate;
    }

    public BigDecimal getCtcLpa() {
        return ctcLpa;
    }

    public void setCtcLpa(BigDecimal ctcLpa) {
        this.ctcLpa = ctcLpa;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public java.time.LocalDateTime getAcceptedAt() {
        return acceptedAt;
    }

    public void setAcceptedAt(java.time.LocalDateTime acceptedAt) {
        this.acceptedAt = acceptedAt;
    }

    public String getDriveId() {
        return driveId;
    }

    public void setDriveId(String driveId) {
        this.driveId = driveId;
    }
}
