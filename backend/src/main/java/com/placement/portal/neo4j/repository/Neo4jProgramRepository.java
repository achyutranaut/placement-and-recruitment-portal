package com.placement.portal.neo4j.repository;

import com.placement.portal.neo4j.model.ProgramNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface Neo4jProgramRepository extends Neo4jRepository<ProgramNode, String> {

    Optional<ProgramNode> findByProgramId(String programId);

    @Query("MATCH (p:Program) RETURN p ORDER BY p.programName ASC")
    List<ProgramNode> findAllOrdered();
}
