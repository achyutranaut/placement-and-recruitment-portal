package com.placement.portal.recruitment;

import java.math.BigDecimal;
import java.time.LocalDate;

public class PlacementDriveDto {
    private String driveId;
    private String jobTitle;
    private BigDecimal minCgpa;
    private String companyId;
    private String companyName;
    private LocalDate driveDate;
    private LocalDate applicationDeadline;
    private Integer openings;
    private BigDecimal ctc;
    private BigDecimal packageLpa;
    private Long applicantCount;
    private String jobDescription;
    private String location;
    private String eligibleBranches;
    private String selectionProcess;

    public PlacementDriveDto() {}

    public PlacementDriveDto(String driveId, String jobTitle, BigDecimal minCgpa, String companyId, String companyName,
                             LocalDate driveDate, LocalDate applicationDeadline, Integer openings, BigDecimal ctc,
                             Long applicantCount, String jobDescription, String location, String eligibleBranches, String selectionProcess) {
        this.driveId = driveId;
        this.jobTitle = jobTitle;
        this.minCgpa = minCgpa;
        this.companyId = companyId;
        this.companyName = companyName;
        this.driveDate = driveDate;
        this.applicationDeadline = applicationDeadline;
        this.openings = openings;
        this.ctc = ctc;
        this.packageLpa = ctc;
        this.applicantCount = applicantCount;
        this.jobDescription = jobDescription;
        this.location = location;
        this.eligibleBranches = eligibleBranches;
        this.selectionProcess = selectionProcess;
    }

    public PlacementDriveDto(String driveId, String jobTitle, BigDecimal minCgpa, String companyId, String companyName,
                             LocalDate driveDate, LocalDate applicationDeadline, Integer openings, BigDecimal ctc, Long applicantCount) {
        this(driveId, jobTitle, minCgpa, companyId, companyName, driveDate, applicationDeadline, openings, ctc, applicantCount,
             "Leading campus recruitment opening for engineering graduates.", "Bengaluru / Hyderabad",
             "Computer Science & Engineering, Information Technology, ECE", "1. Online Assessment | 2. Technical Interview | 3. HR Interview");
    }

    public String getDriveId() { return driveId; }
    public void setDriveId(String driveId) { this.driveId = driveId; }

    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }

    public BigDecimal getMinCgpa() { return minCgpa; }
    public void setMinCgpa(BigDecimal minCgpa) { this.minCgpa = minCgpa; }

    public String getCompanyId() { return companyId; }
    public void setCompanyId(String companyId) { this.companyId = companyId; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public LocalDate getDriveDate() { return driveDate; }
    public void setDriveDate(LocalDate driveDate) { this.driveDate = driveDate; }

    public LocalDate getApplicationDeadline() { return applicationDeadline; }
    public void setApplicationDeadline(LocalDate applicationDeadline) { this.applicationDeadline = applicationDeadline; }

    public Integer getOpenings() { return openings; }
    public void setOpenings(Integer openings) { this.openings = openings; }

    public Integer getOpeningsCount() { return openings; }
    public void setOpeningsCount(Integer openingsCount) { this.openings = openingsCount; }

    public BigDecimal getCtc() { return ctc != null ? ctc : packageLpa; }
    public void setCtc(BigDecimal ctc) { 
        this.ctc = ctc; 
        this.packageLpa = ctc;
    }

    public BigDecimal getPackageLpa() { return ctc != null ? ctc : packageLpa; }
    public void setPackageLpa(BigDecimal packageLpa) { 
        this.packageLpa = packageLpa; 
        if (this.ctc == null) this.ctc = packageLpa;
    }

    public BigDecimal getStartingCtcLpa() { return getCtc(); }
    public void setStartingCtcLpa(BigDecimal startingCtcLpa) { setCtc(startingCtcLpa); }

    public Long getApplicantCount() { return applicantCount; }
    public void setApplicantCount(Long applicantCount) { this.applicantCount = applicantCount; }

    public String getJobDescription() { return jobDescription; }
    public void setJobDescription(String jobDescription) { this.jobDescription = jobDescription; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getEligibleBranches() { return eligibleBranches; }
    public void setEligibleBranches(String eligibleBranches) { this.eligibleBranches = eligibleBranches; }

    public String getSelectionProcess() { return selectionProcess; }
    public void setSelectionProcess(String selectionProcess) { this.selectionProcess = selectionProcess; }
}
