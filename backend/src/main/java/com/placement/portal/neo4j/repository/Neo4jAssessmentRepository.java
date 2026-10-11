package com.placement.portal.neo4j.repository;

import com.placement.portal.neo4j.model.AssessmentNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface Neo4jAssessmentRepository extends Neo4jRepository<AssessmentNode, String> {

    @Query("MATCH (a:Assessment {batchNo: $batchNo}) RETURN a ORDER BY a.title ASC")
    List<AssessmentNode> findByBatchNo(String batchNo);
}
