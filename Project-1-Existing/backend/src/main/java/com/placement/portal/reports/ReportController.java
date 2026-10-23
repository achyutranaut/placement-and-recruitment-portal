package com.placement.portal.reports;

import com.placement.portal.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reports")
@Tag(name = "Reports", description = "Analytical placement statistics, company distributions, and metrics")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/summary")
    @Operation(summary = "Get overall placement statistics (calls Oracle PL/SQL GET_PLACEMENT_RATE and GET_AVERAGE_PACKAGE)")
    public ResponseEntity<ApiResponse<PlacementStatsDto>> getSummary() {
        return ResponseEntity.ok(ApiResponse.ok(reportService.getPlacementSummary()));
    }

    @GetMapping("/overview")
    @Operation(summary = "Get consolidated placement overview statistics (single source of truth)")
    public ResponseEntity<ApiResponse<OverviewReportDto>> getOverview() {
        return ResponseEntity.ok(ApiResponse.ok(reportService.getOverviewReport()));
    }

    @GetMapping("/companies")
    @Operation(summary = "Get company-wise hiring statistics using DQL aggregations")
    public ResponseEntity<ApiResponse<List<CompanyHiringStatDto>>> getCompanyHiringStats() {
        return ResponseEntity.ok(ApiResponse.ok(reportService.getCompanyHiringStats()));
    }
}
