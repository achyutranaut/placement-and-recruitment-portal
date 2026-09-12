package com.placement.portal.application;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApplicationAuditRepository extends JpaRepository<ApplicationAudit, Long> {
    List<ApplicationAudit> findByApplicationIdOrderByChangedAtDesc(String applicationId);
    List<ApplicationAudit> findAllByOrderByChangedAtDesc();
}
