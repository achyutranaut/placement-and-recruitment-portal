package com.placement.portal.company;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobCompanyRepository extends JpaRepository<JobCompany, String> {
    List<JobCompany> findByCompanyId(String companyId);
}
