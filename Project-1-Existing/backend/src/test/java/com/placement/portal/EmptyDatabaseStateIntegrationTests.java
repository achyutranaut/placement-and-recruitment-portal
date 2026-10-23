package com.placement.portal;

import com.placement.portal.application.ApplicationRepository;
import com.placement.portal.company.CompanyRepository;
import com.placement.portal.offer.OfferRepository;
import com.placement.portal.recruitment.PlacementDriveRepository;
import com.placement.portal.reports.OverviewReportDto;
import com.placement.portal.reports.ReportService;
import com.placement.portal.student.StudentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
@Transactional
class EmptyDatabaseStateIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ReportService reportService;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private PlacementDriveRepository driveRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private OfferRepository offerRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Test
    @DisplayName("Empty state: API endpoints return clean empty arrays without mock fallback or 500 error")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testCleanEmptyDatabaseResponses() throws Exception {
        // Clear transactionally to test pure empty state
        offerRepository.deleteAll();
        applicationRepository.deleteAll();
        driveRepository.deleteAll();
        companyRepository.deleteAll();
        studentRepository.deleteAll();

        // 1. GET /api/v1/drives -> success with empty array
        mockMvc.perform(get("/api/v1/drives"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());

        // 2. GET /api/v1/companies -> success with empty array
        mockMvc.perform(get("/api/v1/companies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());

        // 3. GET /api/v1/students -> success with empty array
        mockMvc.perform(get("/api/v1/students"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());

        // 4. Report overview handles zero records cleanly
        OverviewReportDto overview = reportService.getOverviewReport();
        assertNotNull(overview);
        assertEquals(0, overview.getTotalStudents());
        assertEquals(0, overview.getActiveDrives());
        assertEquals(0, overview.getTotalApplications());
        assertEquals(0, overview.getTotalOffers());
    }
}
