package com.placement.portal.neo4j.repository;

import com.placement.portal.neo4j.model.ResumeEvaluationNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface Neo4jResumeEvaluationRepository extends Neo4jRepository<ResumeEvaluationNode, String> {

    Optional<ResumeEvaluationNode> findByApplicationId(String applicationId);

    @Query("MATCH (e:ResumeEvaluation {applicationId: $applicationId}) RETURN e ORDER BY e.evaluatedAt DESC")
    List<ResumeEvaluationNode> findByApplicationIdOrderByEvaluatedAtDesc(String applicationId);

    @Query("MATCH (e:ResumeEvaluation {resumeId: $resumeId}) DETACH DELETE e")
    void deleteByResumeId(String resumeId);
}
