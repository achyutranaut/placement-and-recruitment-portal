package com.placement.portal.neo4j.model;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.ArrayList;
import java.util.List;

@Node("Company")
public class CompanyNode {

    @Id
    private String companyId;

    private String companyName;
    private String email;
    private String industry;
    private List<String> phoneNumbers = new ArrayList<>();

    @Relationship(type = "HOSTS_DRIVE", direction = Relationship.Direction.OUTGOING)
    private List<DriveNode> drives = new ArrayList<>();

    public CompanyNode() {}

    public CompanyNode(String companyId, String companyName, String email, String industry) {
        this.companyId = companyId;
        this.companyName = companyName;
        this.email = email;
        this.industry = industry;
    }

    public String getCompanyId() { return companyId; }
    public void setCompanyId(String companyId) { this.companyId = companyId; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getIndustry() { return industry; }
    public void setIndustry(String industry) { this.industry = industry; }

    public List<String> getPhoneNumbers() { return phoneNumbers; }
    public void setPhoneNumbers(List<String> phoneNumbers) { this.phoneNumbers = phoneNumbers; }

    public List<DriveNode> getDrives() { return drives; }
    public void setDrives(List<DriveNode> drives) { this.drives = drives; }
}
