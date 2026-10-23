package com.placement.portal.company;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "EMAIL_COMPANY")
public class EmailCompany {

    @Id
    @Column(name = "Email", length = 100, nullable = false)
    private String email;

    @Column(name = "Company_Name", length = 100, nullable = false)
    private String companyName;

    public EmailCompany() {}

    public EmailCompany(String email, String companyName) {
        this.email = email;
        this.companyName = companyName;
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
}
