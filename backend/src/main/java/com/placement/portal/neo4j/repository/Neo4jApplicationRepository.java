package com.placement.portal.neo4j.repository;

import com.placement.portal.neo4j.model.ApplicationNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface Neo4jApplicationRepository extends Neo4jRepository<ApplicationNode, String> {

    Optional<ApplicationNode> findByApplicationId(String applicationId);

    @Query("MATCH (a:Application {studentId: $studentId}) RETURN a ORDER BY a.applyDate DESC")
    List<ApplicationNode> findByStudentId(String studentId);

    @Query("MATCH (a:Application {driveId: $driveId}) RETURN a ORDER BY a.applyDate DESC")
    List<ApplicationNode> findByDriveId(String driveId);

    @Query("MATCH (a:Application {studentId: $studentId, applyDate: $applyDate}) RETURN count(a)")
    long countStudentApplicationsOnDate(String studentId, LocalDate applyDate);

    @Query("MATCH (a:Application) RETURN a.status AS status, count(a) AS count")
    List<java.util.Map<String, Object>> countApplicationsByStatus();
}
