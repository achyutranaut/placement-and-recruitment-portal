package com.placement.portal.neo4j.repository;

import com.placement.portal.neo4j.model.BatchNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface Neo4jBatchRepository extends Neo4jRepository<BatchNode, String> {

    @Query("MATCH (b:Batch {programId: $programId}) RETURN b ORDER BY b.batchNo ASC")
    List<BatchNode> findByProgramId(String programId);
}
