package com.placement.portal.neo4j;

import com.placement.portal.application.ApplicationDto;
import com.placement.portal.application.IApplicationService;
import com.placement.portal.auth.AuthResponse;
import com.placement.portal.auth.IAuthService;
import com.placement.portal.auth.LoginRequest;
import com.placement.portal.neo4j.dto.GraphShowcaseDto;
import com.placement.portal.neo4j.model.StudentNode;
import com.placement.portal.neo4j.repository.Neo4jStudentRepository;
import com.placement.portal.neo4j.service.Neo4jGraphAnalyticsService;
import com.placement.portal.offer.IOfferService;
import com.placement.portal.offer.OfferDto;
import com.placement.portal.student.IStudentService;
import com.placement.portal.student.StudentDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("neo4j")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class Neo4jBackendIntegrationTests {

    @Autowired
    private IAuthService authService;

    @Autowired
    private IStudentService studentService;

    @Autowired
    private IApplicationService applicationService;

    @Autowired
    private IOfferService offerService;

    @Autowired
    private Neo4jGraphAnalyticsService analyticsService;

    @Autowired
    private Neo4jStudentRepository studentRepository;

    @Test
    @Order(1)
    @DisplayName("Verify Persona Authentication (Student, Admin, Recruiter)")
    void testAuthenticationPersonas() {
        AuthResponse studentAuth = authService.login(new LoginRequest("25bce1799", "password123"));
        assertNotNull(studentAuth.getToken());
        assertEquals("ROLE_STUDENT", studentAuth.getRole());
        assertEquals("STU023", studentAuth.getReferenceId());

        AuthResponse adminAuth = authService.login(new LoginRequest("admin", "admin123"));
        assertNotNull(adminAuth.getToken());
        assertEquals("ROLE_ADMIN", adminAuth.getRole());

        AuthResponse recruiterAuth = authService.login(new LoginRequest("recruiter1", "password123"));
        assertNotNull(recruiterAuth.getToken());
        assertEquals("ROLE_RECRUITER", recruiterAuth.getRole());
    }

    @Test
    @Order(2)
    @DisplayName("Ported Rule: Enforce Program Eligibility via Graph Traversal (ELIGIBLE_FOR)")
    void testProgramEligibilityRule() {
        StudentDto student = studentService.getStudentById("STU023");
        assertNotNull(student);
        assertEquals("BTECH-CSE", student.getProgramId());

        // DRV001 is eligible for BTECH-CSE
        try {
            ApplicationDto app = applicationService.applyForDrive("STU023", "DRV001", LocalDate.now());
            assertNotNull(app.getApplicationId());
        } catch (IllegalArgumentException | IllegalStateException ex) {
            // Already applied, calendar limit, or already submitted
            assertTrue(ex.getMessage().contains("already") || ex.getMessage().contains("Daily limit") || ex.getMessage().contains("limit reached"),
                    "Unexpected exception message: " + ex.getMessage());
        }
    }

    @Test
    @Order(3)
    @DisplayName("Ported Rule: Enforce 1 Application per Student per Calendar Day")
    void testDailyApplicationLimitRule() {
        LocalDate testDate = LocalDate.of(2026, 11, 15);
        try {
            applicationService.applyForDrive("STU023", "DRV002", testDate);
        } catch (Exception ignored) {}

        // Attempting a second application on the same day must throw Daily limit reached
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            applicationService.applyForDrive("STU023", "DRV003", testDate);
        });
        assertTrue(ex.getMessage().contains("Daily limit reached"));
    }

    @Test
    @Order(4)
    @DisplayName("Ported Rule: CGPA Threshold Validation")
    void testCgpaEligibilityThreshold() {
        // Create student with low CGPA (5.0)
        StudentDto lowCgpaStudent = new StudentDto();
        lowCgpaStudent.setStudentId("STU_LOW_CGPA");
        lowCgpaStudent.setName("Low CGPA Student");
        lowCgpaStudent.setEmail("low.cgpa@vitstudent.ac.in");
        lowCgpaStudent.setRegistrationNo("25BCE8888");
        lowCgpaStudent.setCgpa(BigDecimal.valueOf(5.0));
        lowCgpaStudent.setBranch("B.Tech CSE");
        lowCgpaStudent.setProgramId("BTECH-CSE");

        try {
            studentService.createStudent(lowCgpaStudent);
        } catch (Exception ignored) {}

        // Apply for DRV001 (requires 8.5)
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            applicationService.applyForDrive("STU_LOW_CGPA", "DRV001", LocalDate.now());
        });
        assertTrue(thrown.getMessage().toLowerCase().contains("cgpa"), "Expected CGPA error message but got: " + thrown.getMessage());
    }

    @Test
    @Order(5)
    @DisplayName("Graph Showcase Query 1: Multi-Hop Eligible Drives (Student->Program<-Drive)")
    void testEligibleDrivesShowcaseQuery() {
        List<GraphShowcaseDto.EligibleDriveGraphDto> drives = analyticsService.getEligibleDrivesForStudent("STU023");
        assertNotNull(drives);
        assertFalse(drives.isEmpty());
        assertTrue(drives.stream().anyMatch(d -> "DRV001".equals(d.getDriveId())));
    }

    @Test
    @Order(6)
    @DisplayName("Graph Showcase Query 2: Full Placement Path Traversal")
    void testPlacementPathShowcaseQuery() {
        List<GraphShowcaseDto.PlacementPathDto> path = analyticsService.getFullPlacementPath("STU023");
        assertNotNull(path);
        assertFalse(path.isEmpty());
        assertEquals("Achyut Ranaut", path.get(0).getStudentName());
    }

    @Test
    @Order(7)
    @DisplayName("Graph Showcase Query 3: Academic Program Recruitment Pipeline")
    void testProgramPipelineShowcaseQuery() {
        List<GraphShowcaseDto.ProgramHiringPipelineDto> pipeline = analyticsService.getCompaniesRecruitingFromProgram("BTECH-CSE");
        assertNotNull(pipeline);
        assertFalse(pipeline.isEmpty());
        assertTrue(pipeline.stream().anyMatch(p -> "Microsoft India".equals(p.getCompanyName())));
    }

    @Test
    @Order(8)
    @DisplayName("Graph Showcase Query 4: Similar Students Placed Recommendation")
    void testSimilarStudentsPlacedRecommendation() {
        List<GraphShowcaseDto.SimilarPlacedRecommendationDto> recs = analyticsService.getSimilarStudentsPlaced("STU023");
        assertNotNull(recs);
        assertFalse(recs.isEmpty());
        assertEquals("STU002", recs.get(0).getStudentId());
        assertEquals("Flipkart Internet Pvt Ltd", recs.get(0).getCompanyName());
    }

    @Test
    @Order(9)
    @DisplayName("Ported Trigger: Atomic Offer Acceptance Mutex & Student Status Transition")
    void testAtomicOfferAcceptanceMutex() {
        // Fetch existing offer in seed
        List<OfferDto> offers = offerService.getAllOffers();
        assertNotNull(offers);
        assertFalse(offers.isEmpty());

        OfferDto offer = offers.get(0);
        OfferDto accepted = offerService.acceptOffer(offer.getOfferId(), "STU002");
        assertNotNull(accepted);
        assertEquals("ACCEPTED", accepted.getStatus());

        // Verify student node updated to PLACED in Neo4j
        StudentNode student = studentRepository.findByStudentId("STU002").orElseThrow();
        assertEquals("PLACED", student.getPlacementStatus());
    }
}
