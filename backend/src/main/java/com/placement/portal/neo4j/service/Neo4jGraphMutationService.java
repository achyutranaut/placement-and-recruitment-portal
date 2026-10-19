package com.placement.portal.neo4j.service;

import com.placement.portal.neo4j.dto.GraphMutationDto.*;
import com.placement.portal.neo4j.model.AuditEventNode;
import com.placement.portal.neo4j.repository.Neo4jAuditEventRepository;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Profile("neo4j")
public class Neo4jGraphMutationService {

    private static final Logger log = LoggerFactory.getLogger(Neo4jGraphMutationService.class);

    private static final Set<String> ALLOWED_LABELS = Set.of(
            "Student", "Company", "Drive", "Application", "Offer", "Program", "Batch", "Interviewer"
    );

    private static final Set<String> ALLOWED_RELATIONSHIPS = Set.of(
            "ENROLLED_IN", "OF_PROGRAM", "HOSTS_DRIVE", "ELIGIBLE_FOR",
            "SUBMITTED_APPLICATION", "FOR_DRIVE", "HAS_INTERVIEW", "CONDUCTED_BY", "RECEIVED_OFFER"
    );

    private static final Map<String, String> LABEL_ID_MAP = Map.of(
            "Student", "studentId",
            "Company", "companyId",
            "Drive", "driveId",
            "Application", "applicationId",
            "Offer", "offerId",
            "Program", "programId",
            "Batch", "batchId",
            "Interviewer", "interviewerName"
    );

    private final Driver neo4jDriver;
    private final Neo4jClient neo4jClient;
    private final Neo4jAuditEventRepository auditRepository;

    public Neo4jGraphMutationService(Driver neo4jDriver, Neo4jClient neo4jClient, Neo4jAuditEventRepository auditRepository) {
        this.neo4jDriver = neo4jDriver;
        this.neo4jClient = neo4jClient;
        this.auditRepository = auditRepository;
    }

    public Map<String, Object> createEntity(CreateEntityRequestDto request, String adminUser) {
        String label = sanitizeLabel(request.getLabel());
        String idProp = LABEL_ID_MAP.get(label);

        Map<String, Object> props = new HashMap<>(request.getProperties() != null ? request.getProperties() : Map.of());

        // Ensure unique ID exists or generate prefix-based canonical ID
        String idVal = (String) props.get(idProp);
        if (idVal == null || idVal.isBlank()) {
            idVal = generateCanonicalId(label);
            props.put(idProp, idVal);
        }

        // Check uniqueness constraint
        String checkCypher = "MATCH (n:" + label + " {" + idProp + ": $id}) RETURN count(n) AS cnt";
        try (Session session = neo4jDriver.session()) {
            var res = session.run(checkCypher, Map.of("id", idVal));
            if (res.hasNext() && res.next().get("cnt").asLong() > 0) {
                throw new IllegalArgumentException("Entity with " + idProp + " '" + idVal + "' already exists in database.");
            }

            // Create node
            String createCypher = "CREATE (n:" + label + ") SET n = $props RETURN n";
            var createRes = session.run(createCypher, Map.of("props", props));
            Map<String, Object> createdProps = createRes.hasNext() ? createRes.next().get("n").asMap() : props;

            // Audit record
            String auditId = "AUD_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            auditRepository.save(new AuditEventNode(auditId, idVal, "NONE", "CREATED_" + label.toUpperCase(), adminUser, "GRAPH_MUTATION_API"));

            log.info("Administrator [{}] created Neo4j node [{}] with ID [{}]", adminUser, label, idVal);
            return createdProps;
        }
    }

    public Map<String, Object> updateEntity(String labelInput, String idVal, UpdateEntityRequestDto request, String adminUser) {
        String label = sanitizeLabel(labelInput);
        String idProp = LABEL_ID_MAP.get(label);

        Map<String, Object> updates = new HashMap<>(request.getProperties() != null ? request.getProperties() : Map.of());
        updates.remove(idProp); // Protect ID from modification

        String updateCypher = "MATCH (n:" + label + " {" + idProp + ": $id}) SET n += $props RETURN n";
        try (Session session = neo4jDriver.session()) {
            var res = session.run(updateCypher, Map.of("id", idVal, "props", updates));
            if (!res.hasNext()) {
                throw new NoSuchElementException("Entity of type '" + label + "' with ID '" + idVal + "' not found.");
            }
            Map<String, Object> updatedProps = res.next().get("n").asMap();

            // Audit record
            String auditId = "AUD_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            auditRepository.save(new AuditEventNode(auditId, idVal, "ACTIVE", "UPDATED_" + label.toUpperCase(), adminUser, "GRAPH_MUTATION_API"));

            log.info("Administrator [{}] updated Neo4j node [{}] ID [{}]", adminUser, label, idVal);
            return updatedProps;
        }
    }

    public void deleteEntity(String labelInput, String idVal, String adminUser) {
        String label = sanitizeLabel(labelInput);
        String idProp = LABEL_ID_MAP.get(label);

        try (Session session = neo4jDriver.session()) {
            // Check existence
            String checkCypher = "MATCH (n:" + label + " {" + idProp + ": $id}) RETURN count(n) AS cnt";
            var checkRes = session.run(checkCypher, Map.of("id", idVal));
            if (!checkRes.hasNext() || checkRes.next().get("cnt").asLong() == 0) {
                throw new NoSuchElementException("Entity of type '" + label + "' with ID '" + idVal + "' not found.");
            }

            // Referential dependency protection
            validateDeletionDependencies(session, label, idVal);

            // Safe node deletion with relationship detachment
            String deleteCypher = "MATCH (n:" + label + " {" + idProp + ": $id}) DETACH DELETE n";
            session.run(deleteCypher, Map.of("id", idVal));

            // Audit record
            String auditId = "AUD_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            auditRepository.save(new AuditEventNode(auditId, idVal, "ACTIVE", "DELETED_" + label.toUpperCase(), adminUser, "GRAPH_MUTATION_API"));

            log.info("Administrator [{}] deleted Neo4j node [{}] ID [{}]", adminUser, label, idVal);
        }
    }

    public void createRelationship(CreateRelationshipRequestDto request, String adminUser) {
        String fromLabel = sanitizeLabel(request.getFromLabel());
        String toLabel = sanitizeLabel(request.getToLabel());
        String relType = sanitizeRelationship(request.getType());

        String fromIdProp = LABEL_ID_MAP.get(fromLabel);
        String toIdProp = LABEL_ID_MAP.get(toLabel);

        String cypher = "MATCH (a:" + fromLabel + " {" + fromIdProp + ": $fromId}), " +
                        "(b:" + toLabel + " {" + toIdProp + ": $toId}) " +
                        "MERGE (a)-[r:" + relType + "]->(b) RETURN type(r)";

        try (Session session = neo4jDriver.session()) {
            var res = session.run(cypher, Map.of("fromId", request.getFromId(), "toId", request.getToId()));
            if (!res.hasNext()) {
                throw new NoSuchElementException("Could not connect entities: ensure both source and target nodes exist in the graph.");
            }

            String auditId = "AUD_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            auditRepository.save(new AuditEventNode(auditId, request.getFromId() + "->" + request.getToId(), "UNCONNECTED", "CONNECTED_" + relType, adminUser, "GRAPH_MUTATION_API"));

            log.info("Administrator [{}] created relationship ({}:{})-[:{}]->({}:{})",
                    adminUser, fromLabel, request.getFromId(), relType, toLabel, request.getToId());
        }
    }

    public void deleteRelationship(DeleteRelationshipRequestDto request, String adminUser) {
        String fromLabel = sanitizeLabel(request.getFromLabel());
        String toLabel = sanitizeLabel(request.getToLabel());
        String relType = sanitizeRelationship(request.getType());

        String fromIdProp = LABEL_ID_MAP.get(fromLabel);
        String toIdProp = LABEL_ID_MAP.get(toLabel);

        String cypher = "MATCH (a:" + fromLabel + " {" + fromIdProp + ": $fromId})-[r:" + relType + "]->(b:" + toLabel + " {" + toIdProp + ": $toId}) " +
                        "DELETE r";

        try (Session session = neo4jDriver.session()) {
            session.run(cypher, Map.of("fromId", request.getFromId(), "toId", request.getToId()));

            String auditId = "AUD_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            auditRepository.save(new AuditEventNode(auditId, request.getFromId() + "->" + request.getToId(), "CONNECTED", "DISCONNECTED_" + relType, adminUser, "GRAPH_MUTATION_API"));

            log.info("Administrator [{}] deleted relationship ({}:{})-[:{}]->({}:{})",
                    adminUser, fromLabel, request.getFromId(), relType, toLabel, request.getToId());
        }
    }

    public List<Map<String, Object>> getEntitiesByLabel(String labelInput, int limit) {
        String label = sanitizeLabel(labelInput);
        String idProp = LABEL_ID_MAP.get(label);

        String cypher = "MATCH (n:" + label + ") RETURN n." + idProp + " AS id, n LIMIT " + Math.min(Math.max(limit, 1), 200);
        try (Session session = neo4jDriver.session()) {
            var res = session.run(cypher);
            List<Map<String, Object>> list = new ArrayList<>();
            while (res.hasNext()) {
                Record rec = res.next();
                Map<String, Object> map = new LinkedHashMap<>(rec.get("n").asMap());
                map.put("id", rec.get("id").asString());
                map.put("nodeType", label);
                list.add(map);
            }
            return list;
        }
    }

    private void validateDeletionDependencies(Session session, String label, String idVal) {
        if ("Student".equals(label)) {
            var res = session.run("MATCH (s:Student {studentId: $id})-[:SUBMITTED_APPLICATION]->(a:Application) RETURN count(a) AS cnt", Map.of("id", idVal));
            if (res.hasNext() && res.next().get("cnt").asLong() > 0) {
                throw new IllegalStateException("Integrity Violation: Cannot delete Student '" + idVal + "' because active submitted applications exist.");
            }
        } else if ("Company".equals(label)) {
            var res = session.run("MATCH (c:Company {companyId: $id})-[:HOSTS_DRIVE]->(d:Drive) RETURN count(d) AS cnt", Map.of("id", idVal));
            if (res.hasNext() && res.next().get("cnt").asLong() > 0) {
                throw new IllegalStateException("Integrity Violation: Cannot delete Company '" + idVal + "' because active placement drives are hosted by this company.");
            }
        } else if ("Drive".equals(label)) {
            var res = session.run("MATCH (d:Drive {driveId: $id})<-[:FOR_DRIVE]-(a:Application) RETURN count(a) AS cnt", Map.of("id", idVal));
            if (res.hasNext() && res.next().get("cnt").asLong() > 0) {
                throw new IllegalStateException("Integrity Violation: Cannot delete Drive '" + idVal + "' because student applications are already submitted for this drive.");
            }
        } else if ("Application".equals(label)) {
            var res = session.run("MATCH (a:Application {applicationId: $id})-[:RECEIVED_OFFER]->(o:Offer) RETURN count(o) AS cnt", Map.of("id", idVal));
            if (res.hasNext() && res.next().get("cnt").asLong() > 0) {
                throw new IllegalStateException("Integrity Violation: Cannot delete Application '" + idVal + "' because an offer has been issued for this application.");
            }
        }
    }

    private String sanitizeLabel(String input) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("Node label is required.");
        }
        for (String allowed : ALLOWED_LABELS) {
            if (allowed.equalsIgnoreCase(input.trim())) {
                return allowed;
            }
        }
        throw new IllegalArgumentException("Invalid node label '" + input + "'. Allowed labels: " + ALLOWED_LABELS);
    }

    private String sanitizeRelationship(String input) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("Relationship type is required.");
        }
        for (String allowed : ALLOWED_RELATIONSHIPS) {
            if (allowed.equalsIgnoreCase(input.trim())) {
                return allowed;
            }
        }
        throw new IllegalArgumentException("Invalid relationship type '" + input + "'. Allowed types: " + ALLOWED_RELATIONSHIPS);
    }

    private String generateCanonicalId(String label) {
        String prefix = switch (label) {
            case "Student" -> "STU";
            case "Company" -> "COM";
            case "Drive" -> "DRV";
            case "Application" -> "APP";
            case "Offer" -> "OFF";
            case "Program" -> "PRG";
            case "Batch" -> "BAT";
            case "Interviewer" -> "INT";
            default -> "ENT";
        };
        return prefix + "_" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
}
