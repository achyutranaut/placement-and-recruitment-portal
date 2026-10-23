package com.placement.portal.company;

import java.util.List;

public class RecruiterAuthorizationDto {
    private String userId;
    private String username;
    private String fullName;
    private String email;
    private List<CompanyDto> authorizedCompanies;

    public RecruiterAuthorizationDto() {}

    public RecruiterAuthorizationDto(String userId, String username, String fullName, String email, List<CompanyDto> authorizedCompanies) {
        this.userId = userId;
        this.username = username;
        this.fullName = fullName;
        this.email = email;
        this.authorizedCompanies = authorizedCompanies;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public List<CompanyDto> getAuthorizedCompanies() {
        return authorizedCompanies;
    }

    public void setAuthorizedCompanies(List<CompanyDto> authorizedCompanies) {
        this.authorizedCompanies = authorizedCompanies;
    }
}
