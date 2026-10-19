package com.placement.portal.neo4j.service;

import com.placement.portal.admin.*;
import com.placement.portal.admin.compiler.ISqlCompilerService;
import com.placement.portal.admin.compiler.SqlExecuteRequestDto;
import com.placement.portal.admin.compiler.SqlExecuteResponseDto;
import com.placement.portal.neo4j.model.AuditEventNode;
import com.placement.portal.neo4j.repository.Neo4jAuditEventRepository;
import com.placement.portal.neo4j.repository.Neo4jBatchRepository;
import com.placement.portal.neo4j.repository.Neo4jProgramRepository;
import com.placement.portal.reports.IReportService;
import com.placement.portal.reports.PlacementStatsDto;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Profile("neo4j")
public class Neo4jAdminService implements IAdminService {

    private final IReportService reportService;
    private final Neo4jProgramRepository programRepository;
    private final Neo4jBatchRepository batchRepository;
    private final Neo4jAuditEventRepository auditRepository;
    private final ISqlCompilerService sqlCompilerService;

    public Neo4jAdminService(
            IReportService reportService,
            Neo4jProgramRepository programRepository,
            Neo4jBatchRepository batchRepository,
            Neo4jAuditEventRepository auditRepository,
            ISqlCompilerService sqlCompilerService
    ) {
        this.reportService = reportService;
        this.programRepository = programRepository;
        this.batchRepository = batchRepository;
        this.auditRepository = auditRepository;
        this.sqlCompilerService = sqlCompilerService;
    }

    @Override
    public AdminDashboardSummaryDto getDashboardSummary() {
        PlacementStatsDto stats = reportService.getPlacementSummary();
        long progCount = programRepository.count();
        long batchCount = batchRepository.count();
        return new AdminDashboardSummaryDto(stats, progCount, batchCount);
    }

    @Override
    public List<AuditLogDto> getAuditLogs() {
        List<AuditEventNode> audits = auditRepository.findAllByOrderByChangedAtDesc();
        return audits.stream().map(a -> new AuditLogDto(
                a.getAuditId(),
                a.getApplicationId(),
                a.getOldStatus(),
                a.getNewStatus(),
                a.getChangedAt(),
                a.getChangedBy(),
                a.getTriggerSource() != null ? a.getTriggerSource() : "NEO4J_GRAPH_AUDIT"
        )).collect(Collectors.toList());
    }

    @Override
    public DatabaseConsoleResponseDto executeConsoleQuery(DatabaseConsoleRequestDto request) {
        long startTime = System.currentTimeMillis();
        String queryKey = request.getQueryKey() != null ? request.getQueryKey().toUpperCase() : "DQL_JOIN";
        String category = "Graph Traversal (Cypher)";
        String queryTitle = "Multi-Entity Pattern Match";
        String cypher = "";
        String notes = "Execution against Neo4j Graph Database 5.x.";

        switch (queryKey) {
            case "DQL_JOIN" -> {
                category = "Graph Traversal (Cypher)";
                queryTitle = "Multi-Hop Student-Application-Drive-Company Traversal";
                cypher = """
                    MATCH (s:Student)-[:SUBMITTED_APPLICATION]->(a:Application)-[:FOR_DRIVE]->(d:Drive)<-[:HOSTS_DRIVE]-(c:Company)
                    RETURN s.studentId AS Student_Id, s.name AS Name, s.cgpa AS CGPA,
                           a.applicationId AS Application_Id, a.status AS Status,
                           d.driveId AS Drive_Id, d.jobTitle AS Job_Title,
                           c.companyName AS Company_Name
                    LIMIT 20
                """;
                notes = "Evaluates graph path traversal matching students, applications, drives, and companies without relational multi-table JOIN overhead.";
            }
            case "DQL_GROUP_HAVING" -> {
                category = "Graph Analytics (Cypher)";
                queryTitle = "Drive Application Aggregation & Filtering";
                cypher = """
                    MATCH (c:Company)-[:HOSTS_DRIVE]->(d:Drive)<-[:FOR_DRIVE]-(a:Application)
                    WITH d, c, count(a) AS Total_Applications,
                         sum(CASE WHEN a.status IN ['SELECTED', 'OFFERED', 'ACCEPTED'] THEN 1 ELSE 0 END) AS Selected_Count
                    WHERE Total_Applications >= 1
                    RETURN d.driveId AS Drive_Id, d.jobTitle AS Job_Title, c.companyName AS Company_Name,
                           Total_Applications, Selected_Count
                    ORDER BY Total_Applications DESC
                """;
                notes = "Aggregates applications per drive directly across graph relationships.";
            }
            case "DQL_DENSE_RANK" -> {
                category = "Graph Analytics (Cypher)";
                queryTitle = "Offer CTC Ranking by Company";
                cypher = """
                    MATCH (s:Student)-[:SUBMITTED_APPLICATION]->(a:Application)-[:RECEIVED_OFFER]->(o:Offer),
                          (a)-[:FOR_DRIVE]->(d:Drive)<-[:HOSTS_DRIVE]-(c:Company)
                    RETURN o.offerId AS Offer_Id, s.name AS Student_Name, c.companyName AS Company_Name,
                           toFloat(o.ctcLpa) AS CTC_LPA
                    ORDER BY CTC_LPA DESC
                """;
                notes = "Sorts student offer packages across graph paths.";
            }
            case "PLSQL_PKG_REPORTS" -> {
                category = "Graph Traversal (Cypher)";
                queryTitle = "Placed Student Graph Pathway";
                cypher = """
                    MATCH (s:Student)-[:SUBMITTED_APPLICATION]->(a:Application)-[:RECEIVED_OFFER]->(o:Offer)
                    RETURN o.offerId AS Offer_Id, a.applicationId AS Application_Id,
                           o.offerDate AS Offer_Date, toFloat(o.ctcLpa) AS CTC_LPA,
                           s.studentId AS Student_Id, s.name AS Student_Name, s.cgpa AS CGPA
                    ORDER BY CTC_LPA DESC
                """;
                notes = "Traverses all placed student offer edges in graph.";
            }
            case "TCL_SCENARIO" -> {
                category = "Audit & Compliance (Cypher)";
                queryTitle = "Application State Audit History";
                cypher = """
                    MATCH (ae:AuditEvent)
                    RETURN ae.auditId AS Audit_Id, ae.applicationId AS Application_Id,
                           ae.oldStatus AS Old_Status, ae.newStatus AS New_Status,
                           ae.changedAt AS Changed_At, ae.changedBy AS Changed_By
                    ORDER BY Changed_At DESC
                    LIMIT 15
                """;
                notes = "Retrieves state transition audit trail nodes in Neo4j.";
            }
            default -> {
                if (request.getCustomSql() != null && !request.getCustomSql().trim().isEmpty()) {
                    cypher = request.getCustomSql().trim();
                    category = "Custom Cypher Query";
                    queryTitle = "User Custom Query Execution";
                } else {
                    cypher = "MATCH (s:Student) RETURN s.studentId AS Student_Id, s.name AS Name, s.cgpa AS CGPA LIMIT 10";
                }
            }
        }

        SqlExecuteRequestDto execReq = new SqlExecuteRequestDto(cypher, "QUERY", true);
        SqlExecuteResponseDto execResp = sqlCompilerService.execute(execReq);
        long elapsed = System.currentTimeMillis() - startTime;

        if (execResp.isSuccess()) {
            List<String> cols = execResp.getColumns() != null ? execResp.getColumns() : List.of();
            List<Map<String, Object>> rows = new ArrayList<>();
            if (execResp.getRows() != null) {
                for (List<Object> r : execResp.getRows()) {
                    Map<String, Object> rowMap = new LinkedHashMap<>();
                    for (int i = 0; i < cols.size() && i < r.size(); i++) {
                        rowMap.put(cols.get(i), r.get(i));
                    }
                    rows.add(rowMap);
                }
            }
            return new DatabaseConsoleResponseDto(
                    category,
                    queryTitle,
                    cypher,
                    cols,
                    rows,
                    rows.size(),
                    elapsed,
                    "SUCCESS (200 OK)",
                    notes
            );
        } else {
            return new DatabaseConsoleResponseDto(
                    category,
                    queryTitle,
                    cypher,
                    List.of(),
                    List.of(),
                    0,
                    elapsed,
                    "ERROR: " + execResp.getErrorMessage(),
                    "Execution failed"
            );
        }
    }
}
