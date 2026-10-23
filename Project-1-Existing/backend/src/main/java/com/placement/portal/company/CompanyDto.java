package com.placement.portal.company;

import java.util.List;

public class CompanyDto {
    private String companyId;
    private String email;
    private String companyName;
    private String industry;
    private List<String> phoneNumbers;

    public CompanyDto() {}

    public CompanyDto(String companyId, String email, String companyName, String industry, List<String> phoneNumbers) {
        this.companyId = companyId;
        this.email = email;
        this.companyName = companyName;
        this.industry = industry;
        this.phoneNumbers = phoneNumbers;
    }

    public String getCompanyId() {
        return companyId;
    }

    public void setCompanyId(String companyId) {
        this.companyId = companyId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getIndustry() {
        return industry;
    }

    public void setIndustry(String industry) {
        this.industry = industry;
    }

    public List<String> getPhoneNumbers() {
        return phoneNumbers;
    }

    public void setPhoneNumbers(List<String> phoneNumbers) {
        this.phoneNumbers = phoneNumbers;
    }
}
