package com.placement.portal.recruitment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "PLACEMENT_DRIVE")
public class PlacementDrive {

    @Id
    @Column(name = "Drive_Id", length = 20, nullable = false)
    private String driveId;

    @Column(name = "Job_Title", length = 100, nullable = false)
    private String jobTitle;

    @Column(name = "Min_CGPA", precision = 4, scale = 2, nullable = false)
    private BigDecimal minCgpa;

    @Column(name = "Drive_Date")
    private LocalDate driveDate;

    @Column(name = "Application_Deadline")
    private LocalDate applicationDeadline;

    @Column(name = "Openings")
    private Integer openings = 10;

    @Column(name = "CTC", precision = 10, scale = 2, nullable = false)
    private BigDecimal ctc = BigDecimal.valueOf(12.00);

    @Column(name = "Job_Description", length = 1000)
    private String jobDescription;

    @Column(name = "Location", length = 100)
    private String location;

    @Column(name = "Eligible_Branches", length = 200)
    private String eligibleBranches;

    @Column(name = "Selection_Process", length = 500)
    private String selectionProcess;

    public PlacementDrive() {}

    public PlacementDrive(String driveId, String jobTitle, BigDecimal minCgpa) {
        this.driveId = driveId;
        this.jobTitle = jobTitle;
        this.minCgpa = minCgpa;
    }

    public PlacementDrive(String driveId, String jobTitle, BigDecimal minCgpa, LocalDate driveDate,
                          LocalDate applicationDeadline, Integer openings, BigDecimal ctc) {
        this.driveId = driveId;
        this.jobTitle = jobTitle;
        this.minCgpa = minCgpa;
        this.driveDate = driveDate;
        this.applicationDeadline = applicationDeadline;
        this.openings = openings;
        this.ctc = ctc != null ? ctc : BigDecimal.valueOf(12.00);
    }

    public PlacementDrive(String driveId, String jobTitle, BigDecimal minCgpa, LocalDate driveDate,
                          LocalDate applicationDeadline, Integer openings, BigDecimal ctc,
                          String jobDescription, String location, String eligibleBranches, String selectionProcess) {
        this.driveId = driveId;
        this.jobTitle = jobTitle;
        this.minCgpa = minCgpa;
        this.driveDate = driveDate;
        this.applicationDeadline = applicationDeadline;
        this.openings = openings;
        this.ctc = ctc != null ? ctc : BigDecimal.valueOf(12.00);
        this.jobDescription = jobDescription;
        this.location = location;
        this.eligibleBranches = eligibleBranches;
        this.selectionProcess = selectionProcess;
    }

    public String getDriveId() { return driveId; }
    public void setDriveId(String driveId) { this.driveId = driveId; }

    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }

    public BigDecimal getMinCgpa() { return minCgpa; }
    public void setMinCgpa(BigDecimal minCgpa) { this.minCgpa = minCgpa; }

    public LocalDate getDriveDate() { return driveDate; }
    public void setDriveDate(LocalDate driveDate) { this.driveDate = driveDate; }

    public LocalDate getApplicationDeadline() { return applicationDeadline; }
    public void setApplicationDeadline(LocalDate applicationDeadline) { this.applicationDeadline = applicationDeadline; }

    public Integer getOpenings() { return openings; }
    public void setOpenings(Integer openings) { this.openings = openings; }

    public BigDecimal getCtc() { return ctc; }
    public void setCtc(BigDecimal ctc) { this.ctc = ctc; }

    public BigDecimal getPackageLpa() { return ctc; }
    public void setPackageLpa(BigDecimal packageLpa) { this.ctc = packageLpa; }

    public String getJobDescription() { return jobDescription; }
    public void setJobDescription(String jobDescription) { this.jobDescription = jobDescription; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getEligibleBranches() { return eligibleBranches; }
    public void setEligibleBranches(String eligibleBranches) { this.eligibleBranches = eligibleBranches; }

    public String getSelectionProcess() { return selectionProcess; }
    public void setSelectionProcess(String selectionProcess) { this.selectionProcess = selectionProcess; }
}
