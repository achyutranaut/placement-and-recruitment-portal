package com.placement.portal.company;

public class JobDto {
    private String jobTitle;
    private String companyId;

    public JobDto() {}

    public JobDto(String jobTitle, String companyId) {
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
