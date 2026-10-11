package com.placement.portal.neo4j.repository;

import com.placement.portal.neo4j.model.StudentNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface Neo4jStudentRepository extends Neo4jRepository<StudentNode, String> {

    @Query("MATCH (s:Student {studentId: $studentId}) RETURN s")
    Optional<StudentNode> findByStudentId(String studentId);

    @Query("MATCH (s:Student {email: $email}) RETURN s")
    Optional<StudentNode> findByEmail(String email);

    @Query("MATCH (s:Student {registrationNo: $registrationNo}) RETURN s")
    Optional<StudentNode> findByRegistrationNo(String registrationNo);

    @Query("MATCH (s:Student) RETURN s ORDER BY s.studentId ASC")
    List<StudentNode> findAllOrdered();

    @Query("MATCH (s:Student {placementStatus: $status}) RETURN s")
    List<StudentNode> findByPlacementStatus(String status);
}
