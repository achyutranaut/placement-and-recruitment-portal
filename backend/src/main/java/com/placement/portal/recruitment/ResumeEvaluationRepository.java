package com.placement.portal.recruitment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResumeEvaluationRepository extends JpaRepository<ResumeEvaluation, String> {
    List<ResumeEvaluation> findByApplicationIdOrderByEvaluatedAtDesc(String applicationId);
    Optional<ResumeEvaluation> findTopByApplicationIdOrderByEvaluatedAtDesc(String applicationId);
    List<ResumeEvaluation> findByResumeId(String resumeId);
}
