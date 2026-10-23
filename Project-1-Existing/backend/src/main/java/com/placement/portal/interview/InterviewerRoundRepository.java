package com.placement.portal.interview;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InterviewerRoundRepository extends JpaRepository<InterviewerRound, String> {
    List<InterviewerRound> findByInterviewRoundNo(Integer roundNo);
}
