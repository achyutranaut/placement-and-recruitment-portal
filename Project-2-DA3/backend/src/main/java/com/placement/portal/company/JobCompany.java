package com.placement.portal.company;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "JOB_COMPANY")
public class JobCompany {

    @Id
    @Column(name = "Job_Title", length = 100, nullable = false)
    private String jobTitle;

    @Column(name = "Company_Id", length = 20, nullable = false)
    private String companyId;

    public JobCompany() {}

    public JobCompany(String jobTitle, String companyId) {
        this.jobTitle = jobTitle;
        this.companyId = companyId;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public String getCompanyId() {
        return companyId;
    }

    public void setCompanyId(String companyId) {
        this.companyId = companyId;
    }
}
