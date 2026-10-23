package com.placement.portal.company;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecruiterCompanyRepository extends JpaRepository<RecruiterCompany, RecruiterCompanyId> {
    List<RecruiterCompany> findByUserId(String userId);
    List<RecruiterCompany> findByCompanyId(String companyId);
    boolean existsByUserIdAndCompanyId(String userId, String companyId);
    void deleteByUserIdAndCompanyId(String userId, String companyId);
}
