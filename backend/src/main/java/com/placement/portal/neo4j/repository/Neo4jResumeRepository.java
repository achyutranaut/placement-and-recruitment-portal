package com.placement.portal.neo4j.repository;

import com.placement.portal.neo4j.model.ResumeNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface Neo4jResumeRepository extends Neo4jRepository<ResumeNode, String> {

    Optional<ResumeNode> findByResumeId(String resumeId);

    @Query("MATCH (r:Resume {studentId: $studentId}) RETURN r ORDER BY r.versionNo DESC")
    List<ResumeNode> findByStudentIdOrderByVersionNoDesc(String studentId);

    @Query("MATCH (r:Resume {studentId: $studentId, isCurrent: 'Y'}) RETURN r ORDER BY r.versionNo DESC LIMIT 1")
    Optional<ResumeNode> findCurrentByStudentId(String studentId);

    @Query("MATCH (r:Resume {studentId: $studentId, isCurrent: $isCurrent}) RETURN r")
    List<ResumeNode> findAllByStudentIdAndIsCurrent(String studentId, String isCurrent);

    @Query("MATCH (r:Resume {studentId: $studentId}) RETURN r ORDER BY r.versionNo DESC LIMIT 1")
    Optional<ResumeNode> findTopByStudentIdOrderByVersionNoDesc(String studentId);
}
