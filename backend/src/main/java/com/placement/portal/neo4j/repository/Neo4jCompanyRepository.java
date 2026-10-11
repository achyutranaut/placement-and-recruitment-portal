package com.placement.portal.neo4j.repository;

import com.placement.portal.neo4j.model.CompanyNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface Neo4jCompanyRepository extends Neo4jRepository<CompanyNode, String> {

    Optional<CompanyNode> findByCompanyId(String companyId);

    @Query("MATCH (c:Company) RETURN c ORDER BY c.companyName ASC")
    List<CompanyNode> findAllOrdered();

    @Query("MATCH (u:PortalUser {userId: $userId})-[:AUTHORIZED_FOR]->(c:Company) RETURN c")
    List<CompanyNode> findAuthorizedCompaniesForUser(String userId);
}
