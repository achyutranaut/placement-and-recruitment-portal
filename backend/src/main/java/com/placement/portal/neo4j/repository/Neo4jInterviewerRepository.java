package com.placement.portal.neo4j.repository;

import com.placement.portal.neo4j.model.InterviewerNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface Neo4jInterviewerRepository extends Neo4jRepository<InterviewerNode, String> {

    Optional<InterviewerNode> findByInterviewerName(String interviewerName);
}
