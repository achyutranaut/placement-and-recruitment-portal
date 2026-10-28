package com.placement.portal.admin;

import com.placement.portal.common.ApiResponse;
import com.placement.portal.neo4j.Neo4jProjectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/demo-data")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Demonstration Data Seeder", description = "Endpoints for repeatable mock data management, dry-run validation, and graph synchronization")
public class AdminDemoDataController {

    private final DemoDataSeederService seederService;
    private final Neo4jProjectionService projectionService;

    public AdminDemoDataController(
            DemoDataSeederService seederService,
            Neo4jProjectionService projectionService
    ) {
        this.seederService = seederService;
        this.projectionService = projectionService;
    }

    @GetMapping("/status")
    @Operation(summary = "Inspect current demonstration data presence and entity metrics in Oracle")
    public ResponseEntity<ApiResponse<DemoDataSummaryDto>> getStatus() {
        return ResponseEntity.ok(ApiResponse.ok(seederService.getStatus()));
    }

    @PostMapping("/seed")
    @Operation(summary = "Execute repeatable demonstration mock data seeding with dry-run support")
    public ResponseEntity<ApiResponse<DemoDataSummaryDto>> seedData(
            @RequestParam(name = "dryRun", defaultValue = "false") boolean dryRun,
            @RequestParam(name = "force", defaultValue = "false") boolean force
    ) {
        DemoDataSummaryDto summary = seederService.seedDemoData(dryRun, force);
        return ResponseEntity.ok(ApiResponse.ok(summary.message(), summary));
    }

    @PostMapping("/sync-graph")
    @Operation(summary = "Explicitly project and reconcile Oracle entities into Neo4j graph")
    public ResponseEntity<ApiResponse<Neo4jProjectionService.ProjectionSummary>> syncGraph() {
        Neo4jProjectionService.ProjectionSummary summary = projectionService.reconcileGraphWithOracle();
        return ResponseEntity.ok(ApiResponse.ok(summary.message(), summary));
    }
}
