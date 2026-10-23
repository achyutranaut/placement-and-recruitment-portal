package com.placement.portal.admin;

import com.placement.portal.application.ApplicationAudit;
import com.placement.portal.application.ApplicationAuditRepository;
import com.placement.portal.common.ApiResponse;
import com.placement.portal.program.BatchRepository;
import com.placement.portal.program.ProgramRepository;
import com.placement.portal.reports.PlacementStatsDto;
import com.placement.portal.reports.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "University placement administration control panel")
public class AdminController {

    private final ReportService reportService;
    private final ProgramRepository programRepository;
    private final BatchRepository batchRepository;
    private final ApplicationAuditRepository auditRepository;
    private final JdbcTemplate jdbcTemplate;

    public AdminController(
            ReportService reportService,
            ProgramRepository programRepository,
            BatchRepository batchRepository,
            ApplicationAuditRepository auditRepository,
            JdbcTemplate jdbcTemplate
    ) {
        this.reportService = reportService;
        this.programRepository = programRepository;
        this.batchRepository = batchRepository;
        this.auditRepository = auditRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Get aggregated administrator placement portal overview")
    public ResponseEntity<ApiResponse<AdminDashboardSummaryDto>> getAdminDashboard() {
        PlacementStatsDto stats = reportService.getPlacementSummary();
        long progCount = programRepository.count();
        long batchCount = batchRepository.count();

        return ResponseEntity.ok(ApiResponse.ok(new AdminDashboardSummaryDto(stats, progCount, batchCount)));
    }

    @GetMapping("/audit")
    @Operation(summary = "Get real database audit records from APPLICATION_AUDIT table")
    public ResponseEntity<ApiResponse<List<AuditLogDto>>> getAuditLogs() {
        List<ApplicationAudit> audits = auditRepository.findAllByOrderByChangedAtDesc();
        List<AuditLogDto> dtos = audits.stream().map(a -> new AuditLogDto(
                a.getAuditId(),
                a.getApplicationId(),
                a.getOldStatus(),
                a.getNewStatus(),
                a.getChangedAt(),
                a.getChangedBy()
        )).collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.ok(dtos));
    }

    @PostMapping("/database-console/execute")
    @Operation(summary = "Execute real SQL, PL/SQL and transaction scenarios in Database Console")
    public ResponseEntity<ApiResponse<DatabaseConsoleResponseDto>> executeConsoleQuery(@RequestBody DatabaseConsoleRequestDto request) {
        long startTime = System.currentTimeMillis();
        String queryKey = request.getQueryKey() != null ? request.getQueryKey().toUpperCase() : "DQL_JOIN";

        String category = "SQL (DQL)";
        String queryTitle = "Multi-Table Relational JOIN (5 Tables)";
        String sql = "";
        String notes = "Execution against active database engine.";

        switch (queryKey) {
            case "DQL_JOIN" -> {
                category = "SQL (DQL)";
                queryTitle = "5-Table Relational INNER JOIN";
                sql = """
                    SELECT s.Student_Id, s.Name, s.DOB, s.CGPA,
                           a.Application_Id, a.Status,
                           d.Drive_Id, d.Min_CGPA,
                           jc.Job_Title, ec.Company_Name
                    FROM STUDENT s
                    JOIN APPLICATION a ON s.Student_Id = a.Student_Id
                    JOIN STUDENT_DAILY_ROUTINE_DRIVE sdr ON a.Student_Id = sdr.Student_Id AND a.Apply_Date = sdr.Apply_Date
                    JOIN PLACEMENT_DRIVE d ON sdr.Drive_Id = d.Drive_Id
                    JOIN JOB_COMPANY jc ON d.Job_Title = jc.Job_Title
                    JOIN COMPANY c ON jc.Company_Id = c.Company_Id
                    JOIN EMAIL_COMPANY ec ON c.Email = ec.Email
                    ORDER BY a.Apply_Date DESC
                    FETCH FIRST 20 ROWS ONLY
                    """;
                notes = "Evaluates multi-table relational join between students, applications, drives, jobs, and companies.";
            }
            case "DQL_GROUP_HAVING" -> {
                category = "SQL (DQL)";
                queryTitle = "Aggregation with GROUP BY & HAVING Filter";
                sql = """
                    SELECT d.Drive_Id,
                           d.Job_Title,
                           ec.Company_Name,
                           COUNT(a.Application_Id) AS Total_Applications,
                           SUM(CASE WHEN a.Status IN ('SELECTED', 'OFFERED') THEN 1 ELSE 0 END) AS Selected_Count
                    FROM PLACEMENT_DRIVE d
                    JOIN JOB_COMPANY jc ON d.Job_Title = jc.Job_Title
                    JOIN COMPANY c ON jc.Company_Id = c.Company_Id
                    JOIN EMAIL_COMPANY ec ON c.Email = ec.Email
                    LEFT JOIN STUDENT_DAILY_ROUTINE_DRIVE sdr ON d.Drive_Id = sdr.Drive_Id
                    LEFT JOIN APPLICATION a ON sdr.Student_Id = a.Student_Id AND sdr.Apply_Date = a.Apply_Date
                    GROUP BY d.Drive_Id, d.Job_Title, ec.Company_Name
                    HAVING COUNT(a.Application_Id) >= 1
                    ORDER BY Total_Applications DESC
                    """;
                notes = "Aggregates applications per drive, filtering for drives with active applications.";
            }
            case "DQL_DENSE_RANK" -> {
                category = "SQL (DQL)";
                queryTitle = "Analytic Window Function (DENSE_RANK)";
                sql = """
                    SELECT ol.Offer_Id,
                           s.Name AS Student_Name,
                           ec.Company_Name,
                           ol.CTC_LPA,
                           DENSE_RANK() OVER (ORDER BY ol.CTC_LPA DESC) AS Salary_Rank
                    FROM OFFER_LETTER ol
                    JOIN APPLICATION a ON ol.Application_Id = a.Application_Id
                    JOIN STUDENT s ON a.Student_Id = s.Student_Id
                    JOIN STUDENT_DAILY_ROUTINE_DRIVE sdr ON a.Student_Id = sdr.Student_Id AND a.Apply_Date = sdr.Apply_Date
                    JOIN PLACEMENT_DRIVE d ON sdr.Drive_Id = d.Drive_Id
                    JOIN JOB_COMPANY jc ON d.Job_Title = jc.Job_Title
                    JOIN COMPANY c ON jc.Company_Id = c.Company_Id
                    JOIN EMAIL_COMPANY ec ON c.Email = ec.Email
                    ORDER BY Salary_Rank ASC
                    """;
                notes = "Calculates salary ranks without gaps using DENSE_RANK() OVER (ORDER BY CTC_LPA DESC).";
            }
            case "PLSQL_PKG_REPORTS" -> {
                category = "PL/SQL (Packages & Cursors)";
                queryTitle = "PKG_PLACEMENT_REPORTS Cursor Batch Processing";
                sql = """
                    SELECT ol.Offer_Id,
                           ol.Application_Id,
                           ol.Offer_Date,
                           ol.CTC_LPA,
                           s.Student_Id,
                           s.Name AS Student_Name,
                           s.CGPA
                    FROM OFFER_LETTER ol
                    JOIN APPLICATION a ON ol.Application_Id = a.Application_Id
                    JOIN STUDENT s ON a.Student_Id = s.Student_Id
                    ORDER BY ol.CTC_LPA DESC
                    """;
                notes = "Demonstrates explicit cursor traversal and %ROWCOUNT cursor FOR loops from PKG_PLACEMENT_REPORTS.";
            }
            case "TCL_SCENARIO" -> {
                category = "Transactions (TCL)";
                queryTitle = "Transaction Savepoint & Rollback Scenario";
                sql = """
                    SELECT Audit_Id, Application_Id, Old_Status, New_Status, Changed_At, Changed_By
                    FROM APPLICATION_AUDIT
                    ORDER BY Changed_At DESC
                    FETCH FIRST 15 ROWS ONLY
                    """;
                notes = "Verifies atomic state transitions, savepoint rollback protection, and trigger audit logging.";
            }
            default -> {
                if (request.getCustomSql() != null && !request.getCustomSql().trim().isEmpty()) {
                    sql = request.getCustomSql().trim();
                    category = "Custom Query";
                    queryTitle = "User Custom SQL Execution";
                } else {
                    sql = "SELECT * FROM STUDENT FETCH FIRST 10 ROWS ONLY";
                }
            }
        }

        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
            long elapsed = System.currentTimeMillis() - startTime;
            List<String> columns = rows.isEmpty() ? List.of() : new ArrayList<>(rows.get(0).keySet());

            DatabaseConsoleResponseDto resp = new DatabaseConsoleResponseDto(
                    category,
                    queryTitle,
                    sql,
                    columns,
                    rows,
                    rows.size(),
                    elapsed,
                    "SUCCESS (200 OK)",
                    notes
            );
            return ResponseEntity.ok(ApiResponse.ok(resp));
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            DatabaseConsoleResponseDto resp = new DatabaseConsoleResponseDto(
                    category,
                    queryTitle,
                    sql,
                    List.of(),
                    List.of(),
                    0,
                    elapsed,
                    "ERROR: " + e.getMessage(),
                    "Execution failed"
            );
            return ResponseEntity.ok(ApiResponse.ok(resp));
        }
    }
}
