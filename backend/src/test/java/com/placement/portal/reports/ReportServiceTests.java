package com.placement.portal.reports;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("h2")
@Transactional
class ReportServiceTests {

    @Autowired
    private ReportService reportService;

    @Test
    void testPlacementSummaryMetrics() {
        PlacementStatsDto summary = reportService.getPlacementSummary();

        assertNotNull(summary);
        assertTrue(summary.getTotalStudents() >= 0);
        assertTrue(summary.getTotalCompanies() >= 0);
        assertNotNull(summary.getCompanyStats());
    }

    @Test
    void testCompanyHiringStats() {
        List<CompanyHiringStatDto> stats = reportService.getCompanyHiringStats();
        assertNotNull(stats);
    }
}
