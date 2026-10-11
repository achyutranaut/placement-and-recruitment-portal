package com.placement.portal.neo4j.repository;

import com.placement.portal.neo4j.model.InterviewNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface Neo4jInterviewRepository extends Neo4jRepository<InterviewNode, String> {

    @Query("MATCH (iv:Interview {applicationId: $applicationId}) RETURN iv ORDER BY iv.roundNo ASC")
    List<InterviewNode> findByApplicationId(String applicationId);
}
