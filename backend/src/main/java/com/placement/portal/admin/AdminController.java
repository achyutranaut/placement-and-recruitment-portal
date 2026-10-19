package com.placement.portal.admin;

import com.placement.portal.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "University placement administration control panel")
public class AdminController {

    private final IAdminService adminService;

    public AdminController(IAdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Get aggregated administrator placement portal overview")
    public ResponseEntity<ApiResponse<AdminDashboardSummaryDto>> getAdminDashboard() {
        return ResponseEntity.ok(ApiResponse.ok(adminService.getDashboardSummary()));
    }

    @GetMapping("/audit")
    @Operation(summary = "Get real database audit records from APPLICATION_AUDIT table or Neo4j AuditEvent nodes")
    public ResponseEntity<ApiResponse<List<AuditLogDto>>> getAuditLogs() {
        return ResponseEntity.ok(ApiResponse.ok(adminService.getAuditLogs()));
    }

    @PostMapping("/database-console/execute")
    @Operation(summary = "Execute real SQL, PL/SQL or Cypher scenarios in Database Console")
    public ResponseEntity<ApiResponse<DatabaseConsoleResponseDto>> executeConsoleQuery(@RequestBody DatabaseConsoleRequestDto request) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.executeConsoleQuery(request)));
    }
}
