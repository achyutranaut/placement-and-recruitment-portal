package com.placement.portal.interview;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InterviewRepository extends JpaRepository<Interview, Interview.InterviewId> {
    List<Interview> findByApplicationId(String applicationId);
    List<Interview> findByApplicationIdIn(List<String> applicationIds);
    List<Interview> findByInterviewerName(String interviewerName);
}
