package com.placement.portal.neo4j.repository;

import com.placement.portal.neo4j.model.PortalUserNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface Neo4jUserRepository extends Neo4jRepository<PortalUserNode, String> {

    Optional<PortalUserNode> findByUsername(String username);

    Optional<PortalUserNode> findByUserId(String userId);

    @org.springframework.data.neo4j.repository.query.Query("MATCH (u:PortalUser {userId: $userId}) MATCH (c:Company {companyId: $companyId}) MERGE (u)-[:AUTHORIZED_FOR]->(c)")
    void assignCompany(String userId, String companyId);

    @org.springframework.data.neo4j.repository.query.Query("MATCH (u:PortalUser {userId: $userId})-[r:AUTHORIZED_FOR]->(c:Company {companyId: $companyId}) DELETE r")
    void revokeCompany(String userId, String companyId);
}
