package com.placement.portal.neo4j.repository;

import com.placement.portal.neo4j.model.AuditEventNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface Neo4jAuditEventRepository extends Neo4jRepository<AuditEventNode, String> {

    @Query("MATCH (ev:AuditEvent {applicationId: $applicationId}) RETURN ev ORDER BY ev.changedAt DESC")
    List<AuditEventNode> findByApplicationId(String applicationId);

    @Query("MATCH (ev:AuditEvent) RETURN ev ORDER BY ev.changedAt DESC")
    List<AuditEventNode> findAllByOrderByChangedAtDesc();
}
