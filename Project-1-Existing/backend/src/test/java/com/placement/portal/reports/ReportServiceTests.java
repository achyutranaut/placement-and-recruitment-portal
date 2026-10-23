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

    @Autowired
    private ReportJdbcDao reportJdbcDao;

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

    @Test
    void testOverviewReportApplicationStatusCounts() {
        OverviewReportDto overview = reportService.getOverviewReport();
        assertNotNull(overview);
        assertNotNull(overview.getApplicationStatusCounts());

        // Verify all 8 canonical statuses from CHK_APP_STATUS are present
        String[] canonicalStatuses = {
            "APPLIED", "SHORTLISTED", "INTERVIEWING", "SELECTED", "OFFERED", "ACCEPTED", "DECLINED", "REJECTED"
        };
        for (String status : canonicalStatuses) {
            assertTrue(overview.getApplicationStatusCounts().containsKey(status),
                    "Expected status " + status + " in applicationStatusCounts");
            assertTrue(overview.getApplicationStatusCounts().get(status) >= 0,
                    "Count for status " + status + " should be non-negative");
        }

        // Verify that totalApplications is strictly equal to the sum of all status counts
        long sumOfStatusCounts = overview.getApplicationStatusCounts().values().stream()
                .mapToLong(Long::longValue)
                .sum();
        assertEquals(overview.getTotalApplications(), sumOfStatusCounts,
                "totalApplications must equal the sum of applicationStatusCounts");
    }

    @Test
    void testReportJdbcDaoDirectAggregation() {
        java.util.Map<String, Long> counts = reportJdbcDao.getApplicationStatusCounts();
        assertNotNull(counts);
        assertTrue(counts.containsKey("ACCEPTED"));
        assertTrue(counts.containsKey("DECLINED"));
        assertTrue(counts.containsKey("OFFERED"));
    }
}

