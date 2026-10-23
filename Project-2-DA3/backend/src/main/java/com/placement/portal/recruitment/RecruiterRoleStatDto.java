package com.placement.portal.recruitment;

import java.math.BigDecimal;
import java.time.LocalDate;

public class RecruiterRoleStatDto {
    private String driveId;
    private String jobTitle;
    private String companyId;
    private String companyName;
    private BigDecimal packageLpa;
    private BigDecimal minCgpa;
    private Integer openings;
    private LocalDate driveDate;
    private LocalDate applicationDeadline;
    private Long totalApplications;
    private Long shortlisted;
    private Long interviewing;
    private Long selected;
    private Long offered;
    private Long rejected;

    public RecruiterRoleStatDto() {}

    public RecruiterRoleStatDto(String driveId, String jobTitle, String companyId, String companyName,
                                BigDecimal packageLpa, BigDecimal minCgpa, Integer openings,
                                LocalDate driveDate, LocalDate applicationDeadline,
                                Long totalApplications, Long shortlisted, Long interviewing,
                                Long selected, Long offered, Long rejected) {
        this.driveId = driveId;
        this.jobTitle = jobTitle;
        this.companyId = companyId;
        this.companyName = companyName;
        this.packageLpa = packageLpa;
        this.minCgpa = minCgpa;
        this.openings = openings;
        this.driveDate = driveDate;
        this.applicationDeadline = applicationDeadline;
        this.totalApplications = totalApplications;
        this.shortlisted = shortlisted;
        this.interviewing = interviewing;
        this.selected = selected;
        this.offered = offered;
        this.rejected = rejected;
    }

    public String getDriveId() { return driveId; }
    public String getJobTitle() { return jobTitle; }
    public String getCompanyId() { return companyId; }
    public String getCompanyName() { return companyName; }
    public BigDecimal getPackageLpa() { return packageLpa; }
    public BigDecimal getCtc() { return packageLpa; }
    public BigDecimal getStartingCtcLpa() { return packageLpa; }
    public BigDecimal getMinCgpa() { return minCgpa; }
    public Integer getOpenings() { return openings; }
    public LocalDate getDriveDate() { return driveDate; }
    public LocalDate getApplicationDeadline() { return applicationDeadline; }
    public Long getTotalApplications() { return totalApplications; }
    public Long getShortlisted() { return shortlisted; }
    public Long getInterviewing() { return interviewing; }
    public Long getSelected() { return selected; }
    public Long getOffered() { return offered; }
    public Long getRejected() { return rejected; }
}
