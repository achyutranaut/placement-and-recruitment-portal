package com.placement.portal.company;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "COMPANY")
public class Company {

    @Id
    @Column(name = "Company_Id", length = 20, nullable = false)
    private String companyId;

    @Column(name = "Email", length = 100, nullable = false)
    private String email;

    @Column(name = "Industry", length = 100, nullable = false)
    private String industry;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "COMPANY_PHONE", joinColumns = @JoinColumn(name = "Company_Id"))
    @Column(name = "Phone_No")
    private List<String> phoneNumbers = new ArrayList<>();

    public Company() {}

    public Company(String companyId, String email, String industry) {
        this.companyId = companyId;
        this.email = email;
        this.industry = industry;
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
