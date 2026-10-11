package com.placement.portal.company;

import java.util.List;

public interface ICompanyService {
    List<CompanyDto> getAllCompanies();
    CompanyDto getCompanyById(String companyId);
    List<JobDto> getJobsByCompany(String companyId);
    List<JobDto> getAllJobs();
    List<CompanyDto> getAuthorizedCompanies(String username);
    boolean isRecruiterAuthorizedForCompany(String username, String companyId);
    void assignCompanyToRecruiter(String userId, String companyId);
    void revokeCompanyFromRecruiter(String userId, String companyId);
    List<RecruiterAuthorizationDto> getAllRecruitersWithAuthorizations();
}
