package com.placement.portal.neo4j;

import com.placement.portal.admin.compiler.DatabaseConnectionInfoDto;
import com.placement.portal.admin.compiler.SchemaMetadataDto;
import com.placement.portal.admin.compiler.SqlExecuteResponseDto;
import com.placement.portal.admin.compiler.SqlExecutionAuditDto;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Value;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service executing declarative Cypher queries against Neo4j Aura cloud database.
 * Formats results into standard tabular DTOs compatible with the SQL/Cypher editor grid.
 */
@Service
public class Neo4jCypherService {

    private static final Logger log = LoggerFactory.getLogger(Neo4jCypherService.class);
    private static final int MAX_CYPHER_ROWS = 1000;

    private final Neo4jDriverService driverService;
    private final ConcurrentLinkedDeque<SqlExecutionAuditDto> auditHistory = new ConcurrentLinkedDeque<>();
    private final AtomicLong auditIdGen = new AtomicLong(1);

    public Neo4jCypherService(Neo4jDriverService driverService) {
        this.driverService = driverService;
    }

    public SqlExecuteResponseDto execute(String rawCypher) {
        long startTime = System.currentTimeMillis();
        String currentAdmin = getCurrentAdminId();

        if (rawCypher == null || rawCypher.trim().isEmpty()) {
            return SqlExecuteResponseDto.errorResult("CYPHER-EMPTY", "Cypher query cannot be empty", "Cypher query cannot be empty", null, null, 0);
        }

        String cypher = rawCypher.trim();

        try {
            CypherReadOnlyValidator.validate(cypher);
        } catch (IllegalArgumentException e) {
            long elapsed = System.currentTimeMillis() - startTime;
            String msg = e.getMessage();
            logAudit(currentAdmin, "BLOCKED", cypher, "BLOCKED", 0, 0, elapsed, "CYPHER-POLICY-BLOCKED", msg);
            SqlExecuteResponseDto dto = SqlExecuteResponseDto.blockedResult(msg, "Read-only Cypher Policy Guard", cypher);
            dto.setErrorCode("CYPHER-POLICY-BLOCKED");
            dto.setExecutionTimeMs(elapsed);
            return dto;
        }

        if (!driverService.isEnabled()) {
            String msg = "Neo4j Aura integration is disabled in configuration (portal.neo4j.enabled=false). Enable it via environment variable PORTAL_NEO4J_ENABLED=true.";
            logAudit(currentAdmin, "DISABLED", cypher, "ERROR", 0, 0, 0, "NEO4J-DISABLED", msg);
            return SqlExecuteResponseDto.errorResult("NEO4J-DISABLED", msg, msg, null, null, 0);
        }

        if (!driverService.isConnected()) {
            String msg = "Neo4j Aura is currently unreachable. Check your network connection and NEO4J_URI/NEO4J_PASSWORD settings.";
            logAudit(currentAdmin, "UNREACHABLE", cypher, "ERROR", 0, 0, 0, "NEO4J-UNREACHABLE", msg);
            return SqlExecuteResponseDto.errorResult("NEO4J-UNREACHABLE", msg, msg, null, null, 0);
        }

        org.neo4j.driver.SessionConfig sessionConfig = org.neo4j.driver.SessionConfig.builder()
                .withDefaultAccessMode(org.neo4j.driver.AccessMode.READ)
                .build();
        org.neo4j.driver.TransactionConfig txConfig = org.neo4j.driver.TransactionConfig.builder()
                .withTimeout(java.time.Duration.ofSeconds(10))
                .build();

        try (Session session = driverService.openSession(sessionConfig)) {
            Result result = session.run(cypher, txConfig);

            List<String> columns = new ArrayList<>(result.keys());
            List<List<Object>> rows = new ArrayList<>();
            int count = 0;

            while (result.hasNext() && count < MAX_CYPHER_ROWS) {
                Record record = result.next();
                List<Object> row = new ArrayList<>();
                for (String key : columns) {
                    row.add(convertValue(record.get(key)));
                }
                rows.add(row);
                count++;
            }

            long elapsed = System.currentTimeMillis() - startTime;
            SqlExecuteResponseDto dto = SqlExecuteResponseDto.selectResult(columns, rows, count, "Cypher executed on Neo4j Aura.", elapsed);
            dto.setSql(cypher);
            dto.setStatementType("CYPHER");
            dto.setTransactionState("NONE");
            logAudit(currentAdmin, "CYPHER", cypher, "SUCCESS", 0, count, elapsed, null, null);
            return dto;
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            String errorMsg = e.getMessage() != null ? e.getMessage() : "Cypher execution error";
            logAudit(currentAdmin, "CYPHER", cypher, "ERROR", 0, 0, elapsed, "CYPHER-ERROR", errorMsg);
            return SqlExecuteResponseDto.errorResult("CYPHER-ERROR", errorMsg, errorMsg, null, null, elapsed);
        }
    }

    private Object convertValue(Value val) {
        if (val == null || val.isNull()) {
            return null;
        }
        if (val.type().name().equals("NODE")) {
            Node node = val.asNode();
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("<id>", node.elementId());
            map.put("<labels>", node.labels());
            map.putAll(node.asMap());
            return map;
        }
        if (val.type().name().equals("RELATIONSHIP")) {
            Relationship rel = val.asRelationship();
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("<id>", rel.elementId());
            map.put("<type>", rel.type());
            map.putAll(rel.asMap());
            return map;
        }
        if (val.type().name().equals("LIST")) {
            List<Object> list = new ArrayList<>();
            for (Value v : val.values()) {
                list.add(convertValue(v));
            }
            return list;
        }
        if (val.type().name().equals("MAP")) {
            Map<String, Object> map = new LinkedHashMap<>();
            val.asMap().forEach((k, v) -> map.put(k, v != null ? v.toString() : null));
            return map;
        }
        return val.asObject();
    }

    public SchemaMetadataDto getSchemaMetadata() {
        List<SchemaMetadataDto.TableMetadata> labelsList = new ArrayList<>();
        List<SchemaMetadataDto.ViewMetadata> relationshipsList = new ArrayList<>();
        List<SchemaMetadataDto.ProcedureMetadata> constraintsList = new ArrayList<>();
        List<SchemaMetadataDto.FunctionMetadata> indexesList = new ArrayList<>();
        List<SchemaMetadataDto.TriggerMetadata> projectionsList = new ArrayList<>();

        if (!driverService.isEnabled() || !driverService.isConnected()) {
            return new SchemaMetadataDto(labelsList, relationshipsList, constraintsList, indexesList, projectionsList);
        }

        try (Session session = driverService.openSession()) {
            // 1. Discover node labels
            Result labelResult = session.run("CALL db.labels() YIELD label RETURN label ORDER BY label");
            while (labelResult.hasNext()) {
                String label = labelResult.next().get("label").asString();
                long count = 0;
                try {
                    Result countRes = session.run("MATCH (n:" + label + ") RETURN count(n) AS cnt");
                    if (countRes.hasNext()) {
                        count = countRes.next().get("cnt").asLong();
                    }
                } catch (Exception ignored) {}

                List<SchemaMetadataDto.ColumnMetadata> cols = new ArrayList<>();
                try {
                    Result sampleRes = session.run("MATCH (n:" + label + ") RETURN n LIMIT 1");
                    if (sampleRes.hasNext()) {
                        Node node = sampleRes.next().get("n").asNode();
                        for (Map.Entry<String, Object> entry : node.asMap().entrySet()) {
                            String pName = entry.getKey();
                            String pType = entry.getValue() != null ? entry.getValue().getClass().getSimpleName() : "String";
                            boolean isPk = pName.toLowerCase().endsWith("id");
                            cols.add(new SchemaMetadataDto.ColumnMetadata(pName, pType, 255, true, isPk, null));
                        }
                    }
                } catch (Exception ignored) {}

                labelsList.add(new SchemaMetadataDto.TableMetadata("Node: " + label, count, cols));
            }

            // 2. Discover relationship types
            Result relResult = session.run("CALL db.relationshipTypes() YIELD relationshipType RETURN relationshipType ORDER BY relationshipType");
            while (relResult.hasNext()) {
                String relType = relResult.next().get("relationshipType").asString();
                relationshipsList.add(new SchemaMetadataDto.ViewMetadata("[:" + relType + "]"));
            }

            // 3. Constraints
            try {
                Result cRes = session.run("SHOW CONSTRAINTS YIELD name, type");
                while (cRes.hasNext()) {
                    Record r = cRes.next();
                    constraintsList.add(new SchemaMetadataDto.ProcedureMetadata("Constraint: " + r.get("name").asString() + " (" + r.get("type").asString() + ")", "ACTIVE"));
                }
            } catch (Exception ignored) {}

            projectionsList.add(new SchemaMetadataDto.TriggerMetadata("OracleTransactionalSync", "Oracle23ai", "ON_PROJECTION_SYNC", "CONFIGURED"));
        } catch (Exception e) {
            log.warn("Error fetching Neo4j schema: {}", e.getMessage());
        }

        return new SchemaMetadataDto(labelsList, relationshipsList, constraintsList, indexesList, projectionsList);
    }

    public DatabaseConnectionInfoDto getConnectionInfo() {
        Map<String, Object> status = driverService.getStatus();
        boolean conn = Boolean.TRUE.equals(status.get("connected"));
        return new DatabaseConnectionInfoDto(
                "Neo4j Aura Cloud Graph Database",
                "Neo4j 5.x Aura",
                (String) status.get("username"),
                (String) status.get("uri"),
                7687,
                (String) status.get("database"),
                conn ? "CONNECTED" : (Boolean.TRUE.equals(status.get("enabled")) ? "UNREACHABLE" : "DISABLED"),
                "org.neo4j.driver:neo4j-java-driver"
        );
    }

    public List<SqlExecutionAuditDto> getExecutionHistory(int limit) {
        int max = Math.min(Math.max(limit, 1), 100);
        List<SqlExecutionAuditDto> list = new ArrayList<>(auditHistory);
        if (list.size() > max) {
            return list.subList(0, max);
        }
        return list;
    }

    private void logAudit(String adminId, String stmtType, String cypher, String status, Integer affectedRows, Integer returnedRows, long elapsed, String errCode, String errMsg) {
        SqlExecutionAuditDto audit = new SqlExecutionAuditDto(
                auditIdGen.getAndIncrement(),
                adminId,
                LocalDateTime.now(),
                stmtType,
                cypher,
                status,
                affectedRows,
                returnedRows,
                elapsed,
                errCode,
                errMsg
        );
        auditHistory.addFirst(audit);
        while (auditHistory.size() > 100) {
            auditHistory.removeLast();
        }
    }

    private String getCurrentAdminId() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getName() != null && !auth.getName().isBlank()) {
                return auth.getName();
            }
        } catch (Exception ignored) {}
        return "admin";
    }
}
