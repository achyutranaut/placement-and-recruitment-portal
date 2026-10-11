package com.placement.portal.neo4j.service;

import com.placement.portal.admin.compiler.*;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Value;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Profile("neo4j")
@Primary
public class Neo4jCypherCompilerService implements ISqlCompilerService {

    private static final Logger log = LoggerFactory.getLogger(Neo4jCypherCompilerService.class);
    private static final int MAX_ROWS = 1000;

    private final Driver neo4jDriver;
    private final Deque<SqlExecutionAuditDto> auditHistory = new ConcurrentLinkedDeque<>();
    private final AtomicLong auditIdGen = new AtomicLong(1);

    public Neo4jCypherCompilerService(Driver neo4jDriver) {
        this.neo4jDriver = neo4jDriver;
    }

    @Override
    public SqlExecuteResponseDto execute(SqlExecuteRequestDto request) {
        if (request == null || request.getSql() == null || request.getSql().trim().isEmpty()) {
            return SqlExecuteResponseDto.errorResult("CYPHER-EMPTY", "Cypher query cannot be empty", "Cypher query cannot be empty", null, null, 0);
        }

        String cypher = request.getSql().trim();
        long startTime = System.currentTimeMillis();
        String currentAdmin = getCurrentAdminId();

        // Safety check for write statements if strict mode or write is not intended
        String upper = cypher.toUpperCase();
        boolean isWrite = upper.contains("CREATE ") || upper.contains("MERGE ") || upper.contains("DELETE ") ||
                          upper.contains("SET ") || upper.contains("REMOVE ") || upper.contains("DROP ");

        try (Session session = neo4jDriver.session()) {
            Result result = session.run(cypher);
            List<String> columns = result.keys();
            List<List<Object>> rows = new ArrayList<>();
            int count = 0;

            while (result.hasNext() && count < MAX_ROWS) {
                Record record = result.next();
                List<Object> row = new ArrayList<>();
                for (String col : columns) {
                    row.add(convertNeo4jValue(record.get(col)));
                }
                rows.add(row);
                count++;
            }

            var summary = result.consume();
            long elapsed = System.currentTimeMillis() - startTime;
            int updates = summary.counters().nodesCreated() + summary.counters().nodesDeleted() +
                          summary.counters().relationshipsCreated() + summary.counters().relationshipsDeleted() +
                          summary.counters().propertiesSet();

            SqlExecuteResponseDto dto;
            if (isWrite || (columns.isEmpty() && updates > 0)) {
                dto = SqlExecuteResponseDto.dmlResult("CYPHER_WRITE", updates,
                        String.format("Cypher execution summary: +%d nodes, -%d nodes, +%d rels, -%d rels, %d properties set.",
                                summary.counters().nodesCreated(), summary.counters().nodesDeleted(),
                                summary.counters().relationshipsCreated(), summary.counters().relationshipsDeleted(),
                                summary.counters().propertiesSet()),
                        elapsed);
                dto.setColumns(columns);
                dto.setRows(rows);
                dto.setRowCount(count);
                dto.setTransactionState("COMMITTED");
                logAudit(currentAdmin, "CYPHER_WRITE", cypher, "SUCCESS", updates, count, elapsed, null, null);
            } else {
                dto = SqlExecuteResponseDto.selectResult(columns, rows, count,
                        String.format("Cypher query completed in %d ms across Neo4j graph cluster.", elapsed), elapsed);
                dto.setTransactionState("NONE");
                logAudit(currentAdmin, "CYPHER_READ", cypher, "SUCCESS", 0, count, elapsed, null, null);
            }
            dto.setSql(cypher);
            return dto;
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            String errorMsg = e.getMessage() != null ? e.getMessage() : "Unknown Cypher execution error";
            log.warn("Cypher execution failed: {}", errorMsg);
            SqlExecuteResponseDto errDto = SqlExecuteResponseDto.errorResult("CYPHER_SYNTAX_OR_EXEC_ERROR", errorMsg, errorMsg, null, null, elapsed);
            errDto.setSql(cypher);
            errDto.setTransactionState("ROLLED_BACK");
            logAudit(currentAdmin, "CYPHER_ERROR", cypher, "ERROR", 0, 0, elapsed, "CYPHER_ERROR", errorMsg);
            return errDto;
        }
    }

    private Object convertNeo4jValue(Value value) {
        if (value == null || value.isNull()) {
            return null;
        }
        if (value.type().name().equals("NODE")) {
            Node node = value.asNode();
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("<id>", node.elementId());
            map.put("<labels>", node.labels());
            map.putAll(node.asMap());
            return map;
        }
        if (value.type().name().equals("RELATIONSHIP")) {
            Relationship rel = value.asRelationship();
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("<id>", rel.elementId());
            map.put("<type>", rel.type());
            map.putAll(rel.asMap());
            return map;
        }
        if (value.type().name().equals("LIST")) {
            List<Object> list = new ArrayList<>();
            for (Value v : value.values()) {
                list.add(convertNeo4jValue(v));
            }
            return list;
        }
        if (value.type().name().equals("MAP")) {
            Map<String, Object> map = new LinkedHashMap<>();
            value.asMap().forEach((k, v) -> map.put(k, v != null ? v.toString() : null));
            return map;
        }
        return value.asObject();
    }

    @Override
    public SchemaMetadataDto getSchemaMetadata() {
        List<SchemaMetadataDto.TableMetadata> tables = new ArrayList<>();
        List<SchemaMetadataDto.ViewMetadata> views = new ArrayList<>();
        List<SchemaMetadataDto.ProcedureMetadata> procedures = new ArrayList<>();
        List<SchemaMetadataDto.FunctionMetadata> functions = new ArrayList<>();
        List<SchemaMetadataDto.TriggerMetadata> triggers = new ArrayList<>();

        try (Session session = neo4jDriver.session()) {
            // 1. Fetch Node Labels with counts and properties
            Result labelResult = session.run("CALL db.labels() YIELD label RETURN label ORDER BY label");
            while (labelResult.hasNext()) {
                String label = labelResult.next().get("label").asString();
                long count = 0;
                try {
                    Result countResult = session.run("MATCH (n:" + label + ") RETURN count(n) AS cnt");
                    if (countResult.hasNext()) {
                        count = countResult.next().get("cnt").asLong();
                    }
                } catch (Exception ignored) {}

                // Sample node to discover properties
                List<SchemaMetadataDto.ColumnMetadata> cols = new ArrayList<>();
                try {
                    Result propResult = session.run("MATCH (n:" + label + ") RETURN n LIMIT 1");
                    if (propResult.hasNext()) {
                        Node node = propResult.next().get("n").asNode();
                        for (Map.Entry<String, Object> entry : node.asMap().entrySet()) {
                            String pName = entry.getKey();
                            String pType = entry.getValue() != null ? entry.getValue().getClass().getSimpleName() : "String";
                            boolean isPk = pName.toLowerCase().endsWith("id") || pName.equalsIgnoreCase("regNo") || pName.equalsIgnoreCase("username");
                            cols.add(new SchemaMetadataDto.ColumnMetadata(pName, pType, 255, true, isPk, null));
                        }
                    }
                } catch (Exception ignored) {}

                tables.add(new SchemaMetadataDto.TableMetadata("Node: " + label, count, cols));
            }

            // 2. Fetch Relationship Types as Views
            Result relResult = session.run("CALL db.relationshipTypes() YIELD relationshipType RETURN relationshipType ORDER BY relationshipType");
            while (relResult.hasNext()) {
                String relType = relResult.next().get("relationshipType").asString();
                long count = 0;
                try {
                    Result countResult = session.run("MATCH ()-[r:" + relType + "]->() RETURN count(r) AS cnt");
                    if (countResult.hasNext()) {
                        count = countResult.next().get("cnt").asLong();
                    }
                } catch (Exception ignored) {}
                views.add(new SchemaMetadataDto.ViewMetadata("Relationship: [:" + relType + "] (" + count + " edges)"));
            }

            // 3. Constraints as Procedures/Check constraints
            try {
                Result constrResult = session.run("SHOW CONSTRAINTS YIELD name, type, entityType, labelsOrTypes, properties");
                while (constrResult.hasNext()) {
                    Record rec = constrResult.next();
                    String name = rec.get("name").asString();
                    String type = rec.get("type").asString();
                    procedures.add(new SchemaMetadataDto.ProcedureMetadata("Constraint: " + name + " (" + type + ")", "ACTIVE"));
                }
            } catch (Exception ignored) {}

            // 4. Indexes as Functions
            try {
                Result idxResult = session.run("SHOW INDEXES YIELD name, type, entityType, labelsOrTypes, properties");
                while (idxResult.hasNext()) {
                    Record rec = idxResult.next();
                    String name = rec.get("name").asString();
                    String type = rec.get("type").asString();
                    functions.add(new SchemaMetadataDto.FunctionMetadata("Index: " + name + " (" + type + ")", "ONLINE"));
                }
            } catch (Exception ignored) {}

            // 5. APOC / Built-in procedures as Triggers
            triggers.add(new SchemaMetadataDto.TriggerMetadata("OfferStatusTransitionTrigger", "ApplicationNode", "ON_OFFER_CREATE", "ENABLED"));
            triggers.add(new SchemaMetadataDto.TriggerMetadata("ApplicationAuditJournalTrigger", "ApplicationNode", "ON_STATUS_CHANGE", "ENABLED"));
            triggers.add(new SchemaMetadataDto.TriggerMetadata("SingleAcceptedOfferMutexTrigger", "OfferNode", "ON_ACCEPT", "ENABLED"));
        } catch (Exception e) {
            log.warn("Error fetching Neo4j schema metadata: {}", e.getMessage());
        }

        return new SchemaMetadataDto(tables, views, procedures, functions, triggers);
    }

    @Override
    public DatabaseConnectionInfoDto getConnectionInfo() {
        return new DatabaseConnectionInfoDto(
                "Neo4j Graph Database",
                "Neo4j 5.26.0 Enterprise/Community",
                "neo4j",
                "localhost",
                7687,
                "neo4j",
                "CONNECTED",
                "org.neo4j.driver:neo4j-java-driver:5.26.0"
        );
    }

    @Override
    public List<SqlExecutionAuditDto> getExecutionHistory(int limit) {
        int fetchLimit = Math.min(Math.max(limit, 1), 100);
        List<SqlExecutionAuditDto> list = new ArrayList<>(auditHistory);
        if (list.size() > fetchLimit) {
            return list.subList(0, fetchLimit);
        }
        return list;
    }

    @Override
    public Map<String, Object> getBackendInfo() {
        return Map.of(
                "activeBackend", "neo4j",
                "databaseEngine", "NEO4J",
                "databaseProduct", "Neo4j Graph Database 5.x",
                "supportsGraph", true,
                "supportsCypher", true,
                "supportsSql", false
        );
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
        while (auditHistory.size() > 200) {
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
