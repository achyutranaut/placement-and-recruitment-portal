package com.placement.portal.neo4j.repository;

import com.placement.portal.neo4j.model.OfferNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface Neo4jOfferRepository extends Neo4jRepository<OfferNode, String> {

    Optional<OfferNode> findByOfferId(String offerId);

    Optional<OfferNode> findByApplicationId(String applicationId);

    @Query("MATCH (s:Student {studentId: $studentId})-[:SUBMITTED_APPLICATION]->(a:Application)-[:RECEIVED_OFFER]->(o:Offer) RETURN o")
    List<OfferNode> findByStudentId(String studentId);
}
