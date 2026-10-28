package com.placement.portal.neo4j;

import com.placement.portal.application.Application;
import com.placement.portal.application.ApplicationRepository;
import com.placement.portal.neo4j.dto.GraphShowcaseDto;
import com.placement.portal.student.Student;
import com.placement.portal.student.StudentRepository;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Value;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * Executes high-performance graph traversals for placement journeys, eligibility paths,
 * peer recommendations, and administrative topology mutations against Neo4j.
 */
@Service
public class Neo4jGraphService {

    private static final Logger log = LoggerFactory.getLogger(Neo4jGraphService.class);

    private final Neo4jDriverService driverService;
    private final com.placement.portal.recruitment.PlacementDriveRepository driveRepository;
    private final ApplicationRepository applicationRepository;
    private final StudentRepository studentRepository;

    public Neo4jGraphService(
            Neo4jDriverService driverService,
            @Autowired(required = false) com.placement.portal.recruitment.PlacementDriveRepository driveRepository,
            @Autowired(required = false) ApplicationRepository applicationRepository,
            @Autowired(required = false) StudentRepository studentRepository
    ) {
        this.driverService = driverService;
        this.driveRepository = driveRepository;
        this.applicationRepository = applicationRepository;
        this.studentRepository = studentRepository;
    }

    public List<GraphShowcaseDto.PlacementPathDto> getPlacementPath(String studentId) {
        if (!driverService.isEnabled() || !driverService.isConnected()) {
            return Collections.emptyList();
        }

        try (Session session = driverService.openSession()) {
            String cypher = """
                MATCH (s:Student {studentId: $studentId})
                OPTIONAL MATCH (s)-[:ENROLLED_IN]->(p:Program)
                WITH s, head(collect(DISTINCT coalesce(p.programName, s.branch, 'Academic Program'))) AS programName
                MATCH (s)-[:SUBMITTED_APPLICATION]->(a:Application)-[:FOR_DRIVE]->(d:Drive)
                OPTIONAL MATCH (c:Company)-[:HOSTS_DRIVE]->(d)
                OPTIONAL MATCH (a)-[:RECEIVED_OFFER]->(o:Offer)
                WITH s, programName, a, d,
                     head(collect(DISTINCT coalesce(c.companyName, 'Corporate Partner'))) AS companyName,
                     head(collect(DISTINCT o)) AS o
                RETURN s.studentId AS studentId,
                       s.name AS studentName,
                       programName,
                       a.applicationId AS applicationId,
                       a.status AS applicationStatus,
                       d.driveId AS driveId,
                       d.jobTitle AS jobTitle,
                       companyName,
                       o.offerId AS offerId,
                       o.ctcLpa AS ctcLpa,
                       o.status AS offerStatus
                ORDER BY a.applyDate DESC
            """;

            Result result = session.run(cypher, Map.of("studentId", studentId));
            List<GraphShowcaseDto.PlacementPathDto> list = new ArrayList<>();

            while (result.hasNext()) {
                Record r = result.next();
                GraphShowcaseDto.PlacementPathDto dto = new GraphShowcaseDto.PlacementPathDto();
                dto.setStudentId(getString(r, "studentId"));
                dto.setStudentName(getString(r, "studentName"));
                dto.setProgramName(getString(r, "programName"));
                dto.setApplicationId(getString(r, "applicationId"));
                dto.setApplicationStatus(getString(r, "applicationStatus"));
                dto.setDriveId(getString(r, "driveId"));
                dto.setJobTitle(getString(r, "jobTitle"));
                dto.setCompanyName(getString(r, "companyName"));
                dto.setOfferId(getString(r, "offerId"));
                dto.setCtcLpa(getBigDecimal(r, "ctcLpa"));
                dto.setOfferStatus(getString(r, "offerStatus"));

                // Fetch interview steps
                List<GraphShowcaseDto.InterviewStepDto> steps = new ArrayList<>();
                try {
                    String ivCypher = """
                        MATCH (a:Application {applicationId: $appId})-[:HAS_INTERVIEW]->(iv:Interview)
                        RETURN iv.roundNo AS roundNo, iv.interviewerName AS interviewerName, iv.result AS result
                    """;
                    Result ivRes = session.run(ivCypher, Map.of("appId", dto.getApplicationId()));
                    while (ivRes.hasNext()) {
                        Record ivR = ivRes.next();
                        steps.add(new GraphShowcaseDto.InterviewStepDto(
                                getInteger(ivR, "roundNo"),
                                getString(ivR, "interviewerName"),
                                getString(ivR, "result"),
                                BigDecimal.valueOf(85.0)
                        ));
                    }
                } catch (Exception ignored) {}
                dto.setInterviewSteps(steps);

                list.add(dto);
            }
            return list;
        } catch (Exception e) {
            log.warn("Error running placement path traversal: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<GraphShowcaseDto.EligibleDriveGraphDto> getEligibleDrives(String studentId) {
        if (!driverService.isEnabled() || !driverService.isConnected()) {
            return Collections.emptyList();
        }

        try (Session session = driverService.openSession()) {
            String cypher = """
                MATCH (s:Student {studentId: $studentId})-[:ENROLLED_IN]->(p:Program)
                MATCH (d:Drive)-[:REQUIRES_PROGRAM]->(p)
                OPTIONAL MATCH (c:Company)-[:HOSTS_DRIVE]->(d)
                WHERE s.cgpa >= d.minCgpa
                WITH s, d,
                     head(collect(DISTINCT coalesce(c.companyName, 'Corporate Partner'))) AS companyName,
                     head(collect([(s.studentId), 'ENROLLED_IN', (p.programId), 'REQUIRES_PROGRAM', (d.driveId)])) AS traversalPath
                RETURN d.driveId AS driveId,
                       d.jobTitle AS jobTitle,
                       companyName,
                       d.ctcLpa AS ctcLpa,
                       d.minCgpa AS minCgpa,
                       traversalPath
                ORDER BY d.ctcLpa DESC
            """;

            Result result = session.run(cypher, Map.of("studentId", studentId));
            List<GraphShowcaseDto.EligibleDriveGraphDto> list = new ArrayList<>();

            while (result.hasNext()) {
                Record r = result.next();
                String driveId = getString(r, "driveId");
                LocalDate deadline = null;

                if (driveRepository != null && driveId != null) {
                    var driveOpt = driveRepository.findById(driveId);
                    if (driveOpt.isPresent()) {
                        deadline = driveOpt.get().getApplicationDeadline();
                        if (deadline != null && LocalDate.now().isAfter(deadline)) {
                            continue; // Exclude expired drives from graph-derived eligibility results
                        }
                    }
                }

                List<String> path = new ArrayList<>();
                Value pathVal = r.get("traversalPath");
                if (pathVal != null && !pathVal.isNull()) {
                    for (Value v : pathVal.values()) {
                        path.add(v.asString());
                    }
                }

                list.add(new GraphShowcaseDto.EligibleDriveGraphDto(
                        driveId,
                        getString(r, "jobTitle"),
                        getString(r, "companyName"),
                        getBigDecimal(r, "ctcLpa"),
                        getBigDecimal(r, "minCgpa"),
                        deadline != null ? deadline : LocalDate.now().plusMonths(1),
                        path
                ));
            }
            return list;
        } catch (Exception e) {
            log.warn("Error running eligible drives traversal: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<GraphShowcaseDto.SimilarPlacedRecommendationDto> getPeerRecommendations(String studentId) {
        if (!driverService.isEnabled() || !driverService.isConnected()) {
            return Collections.emptyList();
        }

        try (Session session = driverService.openSession()) {
            String cypher = """
                MATCH (s:Student {studentId: $studentId})-[:ENROLLED_IN]->(p:Program)<-[:ENROLLED_IN]-(peer:Student)
                WHERE peer.studentId <> s.studentId AND peer.placementStatus = 'PLACED'
                MATCH (peer)-[:SUBMITTED_APPLICATION]->(a:Application)-[:FOR_DRIVE]->(d:Drive)<-[:HOSTS_DRIVE]-(c:Company)
                MATCH (a)-[:RECEIVED_OFFER]->(o:Offer)
                RETURN peer.studentId AS peerId,
                       peer.name AS peerName,
                       peer.cgpa AS peerCgpa,
                       c.companyName AS companyName,
                       d.jobTitle AS jobTitle,
                       o.ctcLpa AS ctcLpa,
                       abs(s.cgpa - peer.cgpa) AS cgpaDiff
                ORDER BY cgpaDiff ASC
                LIMIT 15
            """;

            Result result = session.run(cypher, Map.of("studentId", studentId));
            List<GraphShowcaseDto.SimilarPlacedRecommendationDto> list = new ArrayList<>();

            while (result.hasNext()) {
                Record r = result.next();
                list.add(new GraphShowcaseDto.SimilarPlacedRecommendationDto(
                        getString(r, "peerId"),
                        getString(r, "peerName"),
                        getBigDecimal(r, "peerCgpa"),
                        getString(r, "companyName"),
                        getString(r, "jobTitle"),
                        getBigDecimal(r, "ctcLpa"),
                        r.get("cgpaDiff") != null && !r.get("cgpaDiff").isNull() ? r.get("cgpaDiff").asDouble() : 0.0
                ));
            }
            return list;
        } catch (Exception e) {
            log.warn("Error running peer recommendations traversal: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<GraphShowcaseDto.ProgramHiringPipelineDto> getProgramPipeline(String programId) {
        if (!driverService.isEnabled() || !driverService.isConnected()) {
            return Collections.emptyList();
        }

        try (Session session = driverService.openSession()) {
            String cypher = """
                MATCH (p:Program {programId: $progId})<-[:REQUIRES_PROGRAM]-(d:Drive)<-[:HOSTS_DRIVE]-(c:Company)
                OPTIONAL MATCH (d)<-[:FOR_DRIVE]-(a:Application)-[:RECEIVED_OFFER]->(o:Offer)
                RETURN p.programId AS programId,
                       p.programName AS programName,
                       c.companyId AS companyId,
                       c.companyName AS companyName,
                       count(DISTINCT d) AS drivesCount,
                       count(DISTINCT o) AS offersCount,
                       avg(o.ctcLpa) AS avgCtc
            """;

            Result result = session.run(cypher, Map.of("progId", programId));
            List<GraphShowcaseDto.ProgramHiringPipelineDto> list = new ArrayList<>();

            while (result.hasNext()) {
                Record r = result.next();
                list.add(new GraphShowcaseDto.ProgramHiringPipelineDto(
                        getString(r, "programId"),
                        getString(r, "programName"),
                        getString(r, "companyId"),
                        getString(r, "companyName"),
                        r.get("drivesCount").asLong(0),
                        r.get("offersCount").asLong(0),
                        r.get("avgCtc") != null && !r.get("avgCtc").isNull() ? r.get("avgCtc").asDouble() : 0.0
                ));
            }
            return list;
        } catch (Exception e) {
            log.warn("Error running program pipeline query: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<GraphShowcaseDto.InterviewerWorkloadDto> getInterviewerWorkload() {
        if (!driverService.isEnabled() || !driverService.isConnected()) {
            return Collections.emptyList();
        }

        try (Session session = driverService.openSession()) {
            String cypher = """
                MATCH (iv:Interview)
                RETURN iv.interviewerName AS name,
                       iv.roundNo AS roundNo,
                       count(iv) AS total,
                       count(CASE WHEN iv.result = 'CLEARED' THEN 1 END) AS cleared,
                       count(CASE WHEN iv.result = 'REJECTED' THEN 1 END) AS rejected
                ORDER BY total DESC
            """;

            Result result = session.run(cypher);
            List<GraphShowcaseDto.InterviewerWorkloadDto> list = new ArrayList<>();

            while (result.hasNext()) {
                Record r = result.next();
                list.add(new GraphShowcaseDto.InterviewerWorkloadDto(
                        getString(r, "name"),
                        getInteger(r, "roundNo"),
                        r.get("total").asLong(0),
                        r.get("cleared").asLong(0),
                        r.get("rejected").asLong(0)
                ));
            }
            return list;
        } catch (Exception e) {
            log.warn("Error running interviewer workload query: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public Map<String, Object> getWholeGraph(int limit) {
        if (!driverService.isEnabled() || !driverService.isConnected()) {
            return Map.of(
                    "status", "UNAVAILABLE",
                    "message", "Neo4j Aura is disabled or unreachable. Set portal.neo4j.enabled=true and provide valid credentials to view live Aura graph.",
                    "nodes", List.of(),
                    "edges", List.of()
            );
        }

        int max = Math.min(Math.max(limit, 10), 300);
        try (Session session = driverService.openSession()) {
            String cypher = "MATCH (n)-[r]->(m) RETURN n, r, m LIMIT " + max;
            Result result = session.run(cypher);
            return formatGraphResponse(result);
        } catch (Exception e) {
            log.warn("Error running whole graph query: {}", e.getMessage());
            return Map.of("status", "ERROR", "message", e.getMessage(), "nodes", List.of(), "edges", List.of());
        }
    }

    private Map<String, Object> formatGraphResponse(Result result) {
        Map<String, Map<String, Object>> nodeMap = new LinkedHashMap<>();
        List<Map<String, Object>> edgeList = new ArrayList<>();
        Set<String> edgeSet = new HashSet<>();

        while (result.hasNext()) {
            Record rec = result.next();
            for (String k : rec.keys()) {
                Value val = rec.get(k);
                if (val == null || val.isNull()) continue;

                if (val.type().name().equals("NODE")) {
                    Node node = val.asNode();
                    String id = node.elementId();
                    if (!nodeMap.containsKey(id)) {
                        Map<String, Object> nMap = new LinkedHashMap<>();
                        String primaryLabel = node.labels().iterator().hasNext() ? node.labels().iterator().next() : "Node";
                        String label = getNodeLabel(node, primaryLabel);

                        nMap.put("id", id);
                        nMap.put("label", label);
                        nMap.put("group", primaryLabel);
                        nMap.put("properties", sanitizeNodeProperties(node.asMap()));
                        nodeMap.put(id, nMap);
                    }
                } else if (val.type().name().equals("RELATIONSHIP")) {
                    Relationship rel = val.asRelationship();
                    String edgeKey = rel.startNodeElementId() + "->" + rel.type() + "->" + rel.endNodeElementId();
                    if (!edgeSet.contains(edgeKey)) {
                        edgeSet.add(edgeKey);
                        Map<String, Object> eMap = new LinkedHashMap<>();
                        eMap.put("id", rel.elementId());
                        eMap.put("from", rel.startNodeElementId());
                        eMap.put("to", rel.endNodeElementId());
                        eMap.put("label", rel.type());
                        eMap.put("properties", rel.asMap());
                        edgeList.add(eMap);
                    }
                }
            }
        }

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("status", "SUCCESS");
        resp.put("nodes", new ArrayList<>(nodeMap.values()));
        resp.put("edges", edgeList);
        return resp;
    }

    private String getNodeLabel(Node node, String type) {
        Map<String, Object> props = node.asMap();
        return switch (type) {
            case "Student" -> props.getOrDefault("name", props.getOrDefault("studentId", "Student")).toString();
            case "Company" -> props.getOrDefault("companyName", props.getOrDefault("companyId", "Company")).toString();
            case "Drive" -> props.getOrDefault("jobTitle", props.getOrDefault("driveId", "Drive")).toString();
            case "Program" -> props.getOrDefault("programName", props.getOrDefault("programId", "Program")).toString();
            case "Application" -> props.getOrDefault("status", "Application") + " (" + props.getOrDefault("applicationId", "") + ")";
            case "Offer" -> "Offer: ₹" + props.getOrDefault("ctcLpa", "?") + " LPA";
            case "Interview" -> "Interview: " + props.getOrDefault("result", "PENDING");
            default -> type + " (" + props.getOrDefault("id", node.elementId()) + ")";
        };
    }

    private Map<String, Object> sanitizeNodeProperties(Map<String, Object> props) {
        Map<String, Object> clean = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : props.entrySet()) {
            String k = e.getKey();
            if (k.toLowerCase().contains("password") || k.toLowerCase().contains("secret") || k.toLowerCase().contains("token")) {
                continue;
            }
            clean.put(k, e.getValue());
        }
        return clean;
    }

    private String getString(Record r, String col) {
        Value v = r.get(col);
        return (v != null && !v.isNull()) ? v.asString() : null;
    }

    private BigDecimal getBigDecimal(Record r, String col) {
        Value v = r.get(col);
        if (v == null || v.isNull()) return BigDecimal.ZERO;
        try {
            return BigDecimal.valueOf(v.asDouble());
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private Integer getInteger(Record r, String col) {
        Value v = r.get(col);
        if (v == null || v.isNull()) return 1;
        try {
            return v.asInt();
        } catch (Exception e) {
            return 1;
        }
    }

    /**
     * Updates node properties directly in Neo4j and synchronizes with authoritative Oracle entities where applicable.
     */
    public Map<String, Object> updateNode(String nodeType, String id, Map<String, Object> properties) {
        if (!driverService.isEnabled() || !driverService.isConnected()) {
            throw new IllegalStateException("Neo4j is disabled or unreachable. Check connection settings.");
        }

        Map<String, Object> cleanProps = new LinkedHashMap<>();
        if (properties != null) {
            for (Map.Entry<String, Object> entry : properties.entrySet()) {
                String k = entry.getKey();
                Object v = entry.getValue();
                if (k == null || k.isBlank()) continue;
                String cleanKey = k.trim();
                // Filter dangerous or sensitive property names
                if (cleanKey.toLowerCase().contains("password") ||
                    cleanKey.toLowerCase().contains("secret") ||
                    cleanKey.toLowerCase().contains("token")) {
                    continue;
                }
                // Coerce numeric fields appropriately if string provided
                if (cleanKey.equalsIgnoreCase("cgpa") || cleanKey.equalsIgnoreCase("minCgpa") || cleanKey.equalsIgnoreCase("ctcLpa")) {
                    Double d = parseDoubleSafe(v);
                    cleanProps.put(cleanKey, d != null ? d : v);
                } else {
                    cleanProps.put(cleanKey, v);
                }
            }
        }

        try (Session session = driverService.openSession()) {
            // Update node matched by elementId or domain identifier
            String cypher = """
                MATCH (n)
                WHERE elementId(n) = $id
                   OR n.applicationId = $id
                   OR n.studentId = $id
                   OR n.companyId = $id
                   OR n.driveId = $id
                   OR n.offerId = $id
                   OR n.interviewId = $id
                   OR n.programId = $id
                SET n += $props
                RETURN n, elementId(n) AS elemId, labels(n) AS nodeLabels
            """;

            // If updating Application and applicationStatus is passed, also update status property
            if (cleanProps.containsKey("applicationStatus") && !cleanProps.containsKey("status")) {
                cleanProps.put("status", cleanProps.get("applicationStatus"));
            }

            Result res = session.run(cypher, Map.of("id", id, "props", cleanProps));

            // If updating an Application node, propagate related properties to connected subgraph
            if ("Application".equalsIgnoreCase(nodeType)) {
                if (cleanProps.containsKey("status")) {
                    session.run(
                        "MATCH (a:Application) WHERE a.applicationId = $id OR elementId(a) = $id SET a.status = $status",
                        Map.of("id", id, "status", cleanProps.get("status"))
                    );
                }
                if (cleanProps.containsKey("offerStatus")) {
                    session.run(
                        "MATCH (a:Application)-[:RECEIVED_OFFER]->(o:Offer) WHERE a.applicationId = $id OR elementId(a) = $id SET o.status = $status",
                        Map.of("id", id, "status", cleanProps.get("offerStatus"))
                    );
                }
                if (cleanProps.containsKey("packageLpa")) {
                    Double ctc = parseDoubleSafe(cleanProps.get("packageLpa"));
                    if (ctc != null) {
                        session.run(
                            "MATCH (a:Application)-[:RECEIVED_OFFER]->(o:Offer) WHERE a.applicationId = $id OR elementId(a) = $id SET o.ctcLpa = $ctc",
                            Map.of("id", id, "ctc", ctc)
                        );
                    }
                }
                if (cleanProps.containsKey("jobTitle")) {
                    session.run(
                        "MATCH (a:Application)-[:FOR_DRIVE]->(d:Drive) WHERE a.applicationId = $id OR elementId(a) = $id SET d.jobTitle = $jobTitle",
                        Map.of("id", id, "jobTitle", cleanProps.get("jobTitle"))
                    );
                }
                if (cleanProps.containsKey("companyName")) {
                    session.run(
                        "MATCH (a:Application)-[:FOR_DRIVE]->(d:Drive)<-[:HOSTS_DRIVE]-(c:Company) WHERE a.applicationId = $id OR elementId(a) = $id SET c.companyName = $compName",
                        Map.of("id", id, "compName", cleanProps.get("companyName"))
                    );
                }
            }

            // Synchronize with Oracle source of truth if entity exists
            syncToOracleIfPresent(nodeType, id, cleanProps);

            Map<String, Object> updatedDetails = new LinkedHashMap<>(cleanProps);
            updatedDetails.put("id", id);
            updatedDetails.put("nodeType", nodeType);
            log.info("Successfully updated Neo4j node [{}] {}: {}", nodeType, id, cleanProps);
            return updatedDetails;
        } catch (Exception e) {
            log.error("Failed to update Neo4j node [{}] {}: {}", nodeType, id, e.getMessage(), e);
            throw new RuntimeException("Failed to update Neo4j node: " + e.getMessage(), e);
        }
    }

    /**
     * Removes an entity node and its associated relationships from Neo4j.
     */
    public void deleteNode(String nodeType, String id) {
        if (!driverService.isEnabled() || !driverService.isConnected()) {
            throw new IllegalStateException("Neo4j is disabled or unreachable. Check connection settings.");
        }

        try (Session session = driverService.openSession()) {
            String cypher = """
                MATCH (n)
                WHERE elementId(n) = $id
                   OR n.applicationId = $id
                   OR n.studentId = $id
                   OR n.companyId = $id
                   OR n.driveId = $id
                   OR n.offerId = $id
                   OR n.interviewId = $id
                   OR n.programId = $id
                DETACH DELETE n
            """;
            session.run(cypher, Map.of("id", id));
            log.info("Successfully deleted Neo4j node [{}] {}", nodeType, id);
        } catch (Exception e) {
            log.error("Failed to delete Neo4j node [{}] {}: {}", nodeType, id, e.getMessage(), e);
            throw new RuntimeException("Failed to delete Neo4j node: " + e.getMessage(), e);
        }
    }

    /**
     * Registers a new entity node into Neo4j.
     */
    public Map<String, Object> createNode(String label, Map<String, Object> properties) {
        if (!driverService.isEnabled() || !driverService.isConnected()) {
            throw new IllegalStateException("Neo4j is disabled or unreachable. Check connection settings.");
        }

        String safeLabel = sanitizeLabel(label);
        Map<String, Object> cleanProps = new LinkedHashMap<>();
        if (properties != null) {
            for (Map.Entry<String, Object> e : properties.entrySet()) {
                if (e.getKey() != null && !e.getKey().isBlank() && e.getValue() != null) {
                    cleanProps.put(e.getKey().trim(), e.getValue());
                }
            }
        }
        if (!cleanProps.containsKey("id") && !cleanProps.containsKey("companyId") && !cleanProps.containsKey("name")) {
            cleanProps.put("id", "NODE_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        }

        try (Session session = driverService.openSession()) {
            String cypher = String.format("CREATE (n:%s $props) RETURN n, elementId(n) AS elemId", safeLabel);
            Result res = session.run(cypher, Map.of("props", cleanProps));
            Map<String, Object> result = new LinkedHashMap<>(cleanProps);
            result.put("label", safeLabel);
            if (res.hasNext()) {
                Record rec = res.next();
                result.put("elementId", rec.get("elemId").asString());
            }
            log.info("Successfully created Neo4j node [{}]: {}", safeLabel, cleanProps);
            return result;
        } catch (Exception e) {
            log.error("Failed to create Neo4j node [{}]: {}", safeLabel, e.getMessage(), e);
            throw new RuntimeException("Failed to create Neo4j node: " + e.getMessage(), e);
        }
    }

    /**
     * Establishes a directed relationship between two nodes in Neo4j.
     */
    public Map<String, Object> createRelationship(String fromLabel, String fromId, String toLabel, String toId, String relType) {
        if (!driverService.isEnabled() || !driverService.isConnected()) {
            throw new IllegalStateException("Neo4j is disabled or unreachable. Check connection settings.");
        }

        String safeRel = sanitizeRelType(relType);

        try (Session session = driverService.openSession()) {
            String cypher = String.format("""
                MATCH (a), (b)
                WHERE (elementId(a) = $fromId OR a.applicationId = $fromId OR a.studentId = $fromId OR a.companyId = $fromId OR a.driveId = $fromId OR a.offerId = $fromId OR a.interviewId = $fromId OR a.programId = $fromId)
                  AND (elementId(b) = $toId OR b.applicationId = $toId OR b.studentId = $toId OR b.companyId = $toId OR b.driveId = $toId OR b.offerId = $toId OR b.interviewId = $toId OR b.programId = $toId)
                MERGE (a)-[r:%s]->(b)
                RETURN r, elementId(r) AS relId
            """, safeRel);

            Result res = session.run(cypher, Map.of("fromId", fromId, "toId", toId));
            if (!res.hasNext()) {
                throw new IllegalArgumentException(String.format("Could not find matching source node [%s] or target node [%s] in Neo4j.", fromId, toId));
            }
            log.info("Established relationship [{}] between [{}] and [{}]", safeRel, fromId, toId);
            return Map.of("fromId", fromId, "toId", toId, "type", safeRel);
        } catch (Exception e) {
            log.error("Failed to establish Neo4j relationship {}: {}", safeRel, e.getMessage(), e);
            throw new RuntimeException("Failed to establish relationship: " + e.getMessage(), e);
        }
    }

    private void syncToOracleIfPresent(String nodeType, String id, Map<String, Object> props) {
        try {
            if ("Application".equalsIgnoreCase(nodeType) && applicationRepository != null) {
                var appOpt = applicationRepository.findById(id);
                if (appOpt.isPresent()) {
                    var app = appOpt.get();
                    if (props.containsKey("status") && props.get("status") != null) {
                        app.setStatus(props.get("status").toString());
                    } else if (props.containsKey("applicationStatus") && props.get("applicationStatus") != null) {
                        app.setStatus(props.get("applicationStatus").toString());
                    }
                    applicationRepository.save(app);
                    log.info("Synchronized updated Application [{}] status to Oracle database.", id);
                }
            } else if ("Student".equalsIgnoreCase(nodeType) && studentRepository != null) {
                var stuOpt = studentRepository.findById(id);
                if (stuOpt.isPresent()) {
                    var stu = stuOpt.get();
                    if (props.containsKey("name") && props.get("name") != null) {
                        stu.setName(props.get("name").toString());
                    }
                    if (props.containsKey("placementStatus") && props.get("placementStatus") != null) {
                        stu.setPlacementStatus(props.get("placementStatus").toString());
                    }
                    studentRepository.save(stu);
                    log.info("Synchronized updated Student [{}] to Oracle database.", id);
                }
            }
        } catch (Exception e) {
            log.warn("Non-fatal: could not sync updated node [{}] {} to Oracle: {}", nodeType, id, e.getMessage());
        }
    }

    private String sanitizeLabel(String label) {
        if (label == null || label.isBlank()) return "Node";
        String cleaned = label.replaceAll("[^a-zA-Z0-9_]", "");
        return cleaned.isEmpty() ? "Node" : cleaned;
    }

    private String sanitizeRelType(String relType) {
        if (relType == null || relType.isBlank()) return "CONNECTED_TO";
        String cleaned = relType.replaceAll("[^a-zA-Z0-9_]", "").toUpperCase();
        return cleaned.isEmpty() ? "CONNECTED_TO" : cleaned;
    }

    private Double parseDoubleSafe(Object val) {
        if (val == null) return null;
        if (val instanceof Number n) return n.doubleValue();
        String str = val.toString().replaceAll("[^0-9.]", "").trim();
        if (str.isEmpty()) return null;
        try {
            return Double.parseDouble(str);
        } catch (Exception e) {
            return null;
        }
    }
}
