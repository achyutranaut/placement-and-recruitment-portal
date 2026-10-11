package com.placement.portal.neo4j.model;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Node("Drive")
public class DriveNode {

    @Id
    private String driveId;

    private String jobTitle;
    private String companyId;
    private String companyName;
    private BigDecimal minCgpa;
    private LocalDate driveDate;
    private LocalDate applicationDeadline;
    private Integer openings;
    private BigDecimal ctc;
    private String jobDescription;
    private String location;
    private String eligibleBranches;
    private String selectionProcess;

    @Relationship(type = "HOSTS_DRIVE", direction = Relationship.Direction.INCOMING)
    private CompanyNode company;

    @Relationship(type = "ELIGIBLE_FOR", direction = Relationship.Direction.OUTGOING)
    private List<ProgramNode> eligiblePrograms = new ArrayList<>();

// applications via ApplicationNode.drive

    public DriveNode() {}

    public DriveNode(String driveId, String jobTitle, String companyId, String companyName, BigDecimal minCgpa,
                     LocalDate driveDate, LocalDate applicationDeadline, Integer openings, BigDecimal ctc,
                     String jobDescription, String location, String eligibleBranches, String selectionProcess) {
        this.driveId = driveId;
        this.jobTitle = jobTitle;
        this.companyId = companyId;
        this.companyName = companyName;
        this.minCgpa = minCgpa;
        this.driveDate = driveDate;
        this.applicationDeadline = applicationDeadline;
        this.openings = openings;
        this.ctc = ctc;
        this.jobDescription = jobDescription;
        this.location = location;
        this.eligibleBranches = eligibleBranches;
        this.selectionProcess = selectionProcess;
    }

    public String getDriveId() { return driveId; }
    public void setDriveId(String driveId) { this.driveId = driveId; }

    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }

    public String getCompanyId() { return companyId; }
    public void setCompanyId(String companyId) { this.companyId = companyId; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

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

    public String getJobDescription() { return jobDescription; }
    public void setJobDescription(String jobDescription) { this.jobDescription = jobDescription; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getEligibleBranches() { return eligibleBranches; }
    public void setEligibleBranches(String eligibleBranches) { this.eligibleBranches = eligibleBranches; }

    public String getSelectionProcess() { return selectionProcess; }
    public void setSelectionProcess(String selectionProcess) { this.selectionProcess = selectionProcess; }

    public CompanyNode getCompany() { return company; }
    public void setCompany(CompanyNode company) { this.company = company; }

    public List<ProgramNode> getEligiblePrograms() { return eligiblePrograms; }
    public void setEligiblePrograms(List<ProgramNode> eligiblePrograms) { this.eligiblePrograms = eligiblePrograms; }


}
