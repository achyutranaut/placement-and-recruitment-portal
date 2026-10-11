package com.placement.portal.neo4j.repository;

import com.placement.portal.neo4j.model.DriveNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface Neo4jDriveRepository extends Neo4jRepository<DriveNode, String> {

    Optional<DriveNode> findByDriveId(String driveId);

    @Query("MATCH (d:Drive) RETURN d ORDER BY d.driveId ASC")
    List<DriveNode> findAllOrdered();

    @Query("MATCH (d:Drive {companyId: $companyId}) RETURN d ORDER BY d.driveDate DESC")
    List<DriveNode> findByCompanyId(String companyId);

    @Query("MATCH (d:Drive {driveId: $driveId})-[:ELIGIBLE_FOR]->(p:Program) RETURN p.programId")
    List<String> findEligibleProgramIds(String driveId);

    @Query("MATCH (a:Application {driveId: $driveId}) RETURN count(a)")
    long countApplicationsByDriveId(String driveId);
}
