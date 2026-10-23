package com.placement.portal.company;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmailCompanyRepository extends JpaRepository<EmailCompany, String> {
}
