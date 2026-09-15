package com.placement.portal.recruitment;

import com.placement.portal.application.ApplicationDto;
import com.placement.portal.application.ApplicationService;
import com.placement.portal.student.Student;
import com.placement.portal.student.StudentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
@Transactional
class EligibilityAndPipelineIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PlacementDriveService driveService;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private PlacementDriveRepository driveRepository;

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private com.placement.portal.offer.OfferService offerService;

    @Autowired
    private com.placement.portal.offer.OfferRepository offerRepository;

    @Autowired
    private com.placement.portal.application.ApplicationRepository applicationRepository;

    @Autowired
    private com.placement.portal.interview.InterviewRepository interviewRepository;

    private void clearInterviewRounds(String appId) {
        interviewRepository.save(new com.placement.portal.interview.Interview(
                appId, "Vikram Malhotra", BigDecimal.valueOf(90), null, null, "CLEARED", "Y", "N"
        ));
        interviewRepository.save(new com.placement.portal.interview.Interview(
                appId, "Sangeeta Rao", null, BigDecimal.valueOf(88), null, "CLEARED", "Y", "N"
        ));
        interviewRepository.save(new com.placement.portal.interview.Interview(
                appId, "Arvind Swamy", null, null, BigDecimal.valueOf(92), "CLEARED", "Y", "N"
        ));
    }

    // ============================================================
    // BUG #1 TEST SUITE: 6 Authoritative Eligibility Scenarios
    // ============================================================

    @Test
    @DisplayName("Case 1: CGPA 9.25 >= 8.50, BCE (CSE) -> ELIGIBLE")
    void testCase1_EligibleCseStudent() {
        // STU001: CGPA 9.25, Program BCE. DRV001: min 8.50, eligible BCE, BCT, BDS. Deadline 2026-09-20
        DriveEligibilityDto result = driveService.evaluateEligibility("DRV001", "STU001");

        assertTrue(result.isEligible(), "Student STU001 must be ELIGIBLE for DRV001");
        assertTrue(result.isCgpaEligible(), "CGPA 9.25 >= 8.50 must be cgpaEligible");
        assertTrue(result.isProgramEligible(), "Program BCE must be programEligible");
        assertEquals(0, BigDecimal.valueOf(9.25).compareTo(result.getStudentCgpa()));
        assertEquals(0, BigDecimal.valueOf(8.50).compareTo(result.getMinimumCgpa()));
        assertFalse(result.isDeadlinePassed(), "Deadline 2026-09-20 is not passed");
    }

    @Test
    @DisplayName("Case 2: CGPA exactly 8.50 == 8.50, Program BCE -> ELIGIBLE")
    void testCase2_ExactThresholdCgpa() {
        // Temporarily adjust student CGPA to exactly 8.50
        Student student = studentRepository.findById("STU001").orElseThrow();
        BigDecimal originalCgpa = student.getCgpa();
        try {
            student.setCgpa(BigDecimal.valueOf(8.50));
            studentRepository.save(student);

            DriveEligibilityDto result = driveService.evaluateEligibility("DRV001", "STU001");
            assertTrue(result.isEligible(), "Student with exact threshold 8.50 must be ELIGIBLE");
            assertTrue(result.isCgpaEligible(), "CGPA 8.50 >= 8.50 must be true");
            assertTrue(result.isProgramEligible());
        } finally {
            student.setCgpa(originalCgpa);
            studentRepository.save(student);
        }
    }

    @Test
    @DisplayName("Case 3: CGPA 8.49 < 8.50, Program BCE -> NOT ELIGIBLE")
    void testCase3_BelowThresholdCgpa() {
        Student student = studentRepository.findById("STU001").orElseThrow();
        BigDecimal originalCgpa = student.getCgpa();
        try {
            student.setCgpa(BigDecimal.valueOf(8.49));
            studentRepository.save(student);

            DriveEligibilityDto result = driveService.evaluateEligibility("DRV001", "STU001");
            assertFalse(result.isEligible(), "Student with CGPA 8.49 must NOT be eligible for min CGPA 8.50");
            assertFalse(result.isCgpaEligible(), "cgpaEligible must be false for 8.49 < 8.50");
            assertTrue(result.isProgramEligible(), "Program is still eligible");
        } finally {
            student.setCgpa(originalCgpa);
            studentRepository.save(student);
        }
    }

    @Test
    @DisplayName("Case 4: CGPA 9.25, Program BME (Mechanical) for DRV001 -> NOT ELIGIBLE")
    void testCase4_IneligibleProgram() {
        // STU009 is Mechanical Engineering (BME), CGPA 7.60. Set CGPA to 9.25 to isolate program check.
        Student student = studentRepository.findById("STU009").orElseThrow();
        BigDecimal originalCgpa = student.getCgpa();
        try {
            student.setCgpa(BigDecimal.valueOf(9.25));
            studentRepository.save(student);

            DriveEligibilityDto result = driveService.evaluateEligibility("DRV001", "STU009");
            assertFalse(result.isEligible(), "BME student must NOT be eligible for DRV001 (requires BCE, BCT, BDS)");
            assertTrue(result.isCgpaEligible(), "CGPA 9.25 >= 8.50 is satisfied");
            assertFalse(result.isProgramEligible(), "Program BME is not in DRIVE_ELIGIBILITY for DRV001");
        } finally {
            student.setCgpa(originalCgpa);
            studentRepository.save(student);
        }
    }

    @Test
    @DisplayName("Case 5: Eligible CGPA + Eligible Program + Expired Deadline -> NOT ELIGIBLE")
    void testCase5_ExpiredDeadline() {
        PlacementDrive drive = driveRepository.findById("DRV001").orElseThrow();
        LocalDate originalDeadline = drive.getApplicationDeadline();
        try {
            // Set deadline to yesterday
            drive.setApplicationDeadline(LocalDate.now().minusDays(1));
            driveRepository.save(drive);

            DriveEligibilityDto result = driveService.evaluateEligibility("DRV001", "STU001");
            assertFalse(result.isEligible(), "Drive with expired deadline must NOT be eligible");
            assertTrue(result.isDeadlinePassed(), "deadlinePassed must be true");
            assertTrue(result.isCgpaEligible(), "CGPA was eligible");
            assertTrue(result.isProgramEligible(), "Program was eligible");
        } finally {
            drive.setApplicationDeadline(originalDeadline);
            driveRepository.save(drive);
        }
    }

    @Test
    @DisplayName("Case 6: Eligible CGPA + Eligible Program + Open Drive -> ELIGIBLE")
    void testCase6_OpenDriveEligible() {
        PlacementDrive drive = driveRepository.findById("DRV001").orElseThrow();
        LocalDate originalDeadline = drive.getApplicationDeadline();
        try {
            // Future deadline
            drive.setApplicationDeadline(LocalDate.now().plusMonths(2));
            driveRepository.save(drive);

            DriveEligibilityDto result = driveService.evaluateEligibility("DRV001", "STU001");
            assertTrue(result.isEligible(), "Eligible student + open drive must be ELIGIBLE");
            assertFalse(result.isDeadlinePassed(), "Deadline not passed");
            assertTrue(result.isDriveOpen(), "Drive must be open");
        } finally {
            drive.setApplicationDeadline(originalDeadline);
            driveRepository.save(drive);
        }
    }

    // ============================================================
    // API AUTHORITATIVE ENDPOINT: GET /api/v1/drives/{id}/eligibility
    // ============================================================

    @Test
    @WithMockUser(username = "student1", roles = {"STUDENT"})
    @DisplayName("API: GET /api/v1/drives/DRV001/eligibility resolves student from principal")
    void testDriveEligibilityEndpoint_PrincipalResolution() throws Exception {
        mockMvc.perform(get("/api/v1/drives/DRV001/eligibility"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.eligible").value(true))
                .andExpect(jsonPath("$.data.cgpaEligible").value(true))
                .andExpect(jsonPath("$.data.programEligible").value(true))
                .andExpect(jsonPath("$.data.studentCgpa").value(9.25))
                .andExpect(jsonPath("$.data.minimumCgpa").value(8.50))
                .andExpect(jsonPath("$.data.studentProgram").isString())
                .andExpect(jsonPath("$.data.eligiblePrograms").isArray())
                .andExpect(jsonPath("$.data.eligiblePrograms", hasItem("Computer Science & Engineering")));
    }

    @Test
    @WithMockUser(username = "student1", roles = {"STUDENT"})
    @DisplayName("API: GET /api/v1/drives/eligibility returns map of all drives with DRV004 and DRV006 eligible")
    void testAllDrivesEligibilityEndpoint_PrincipalResolution() throws Exception {
        mockMvc.perform(get("/api/v1/drives/eligibility"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.DRV001.eligible").value(true))
                .andExpect(jsonPath("$.data.DRV004.eligible").value(true))
                .andExpect(jsonPath("$.data.DRV004.cgpaEligible").value(true))
                .andExpect(jsonPath("$.data.DRV004.programEligible").value(true))
                .andExpect(jsonPath("$.data.DRV005.eligible").value(true))
                .andExpect(jsonPath("$.data.DRV006.eligible").value(true))
                .andExpect(jsonPath("$.data.DRV006.cgpaEligible").value(true))
                .andExpect(jsonPath("$.data.DRV006.programEligible").value(true));
    }

    // ============================================================
    // BUG #2 TEST SUITE: Application Pipeline & Ownership Integrity
    // ============================================================

    @Test
    @WithMockUser(username = "student1", roles = {"STUDENT"})
    @DisplayName("API: GET /api/v1/applications/my returns real applications for student1")
    void testGetMyApplications_Student1() throws Exception {
        mockMvc.perform(get("/api/v1/applications/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$.data[?(@.driveId == 'DRV001')].status").value("OFFERED"))
                .andExpect(jsonPath("$.data[?(@.driveId == 'DRV001')].studentId").value("STU001"))
                .andExpect(jsonPath("$.data[?(@.driveId == 'DRV001')].applicationId").value("APP001"));
    }

    @Test
    @WithMockUser(username = "student2", roles = {"STUDENT"})
    @DisplayName("API: GET /api/v1/applications/my isolates student2 from student1")
    void testGetMyApplications_Student2Isolation() throws Exception {
        mockMvc.perform(get("/api/v1/applications/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                // Student2 only gets STU002 applications
                .andExpect(jsonPath("$.data[*].studentId", everyItem(is("STU002"))));
    }

    @Test
    @DisplayName("API: GET /api/v1/applications/my requires authentication")
    void testGetMyApplications_UnauthenticatedForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/applications/my"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "student1", roles = {"STUDENT"})
    @DisplayName("Security: Student1 cannot apply for another student (STU002)")
    void testApplyForDrive_CrossStudentForbidden() throws Exception {
        String json = """
                {
                    "studentId": "STU002",
                    "driveId": "DRV001",
                    "applyDate": "2026-09-01"
                }
                """;

        mockMvc.perform(post("/api/v1/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "student1", roles = {"STUDENT"})
    @DisplayName("Backend Rule: Ineligible student submission is rejected at service/database layer")
    void testBackendAuthoritativeRejection_IneligibleSubmission() {
        // STU009 is Mechanical Engineering (BME) -> ineligible for DRV001
        Exception ex = assertThrows(Exception.class, () ->
                applicationService.applyForDrive("STU009", "DRV001", LocalDate.of(2026, 9, 1)));

        assertTrue(ex.getMessage().contains("Ineligible") || ex.getMessage().contains("eligibility") ||
                   (ex.getCause() != null && ex.getCause().getMessage().contains("Ineligible")));
    }

    // ============================================================
    // INTERVIEW SCORES & OVERALL CANDIDATE SCORE TEST SUITE
    // ============================================================

    @Test
    @DisplayName("Service: Case A - Completed candidate APP001 gets overall score 94.33")
    void testApplicationScores_CaseA_CompletedCandidate() {
        ApplicationDto app = applicationService.getApplicationById("APP001");
        assertNotNull(app, "APP001 must exist");
        assertNotNull(app.getScores(), "Scores DTO must be present");
        assertNotNull(app.getScoreCompletion(), "ScoreCompletion DTO must be present");

        assertEquals(0, BigDecimal.valueOf(95).compareTo(app.getScores().getOa()), "OA score must be 95");
        assertEquals(0, BigDecimal.valueOf(92).compareTo(app.getScores().getTechnical()), "Technical score must be 92");
        assertEquals(0, BigDecimal.valueOf(96).compareTo(app.getScores().getHr()), "HR score must be 96");
        assertEquals(new BigDecimal("94.33"), app.getScores().getOverall(), "Overall score must be 94.33 ((95+92+96)/3)");

        assertTrue(app.getScoreCompletion().isOaCompleted(), "OA must be completed");
        assertTrue(app.getScoreCompletion().isTechnicalCompleted(), "Technical must be completed");
        assertTrue(app.getScoreCompletion().isHrCompleted(), "HR must be completed");
        assertTrue(app.getScoreCompletion().isAllRoundsCompleted(), "All rounds completed must be true");

        assertNotNull(app.getInterviews());
        assertEquals(3, app.getInterviews().size());
        assertEquals(Integer.valueOf(1), app.getInterviews().get(0).getRoundNo());
        assertEquals(Integer.valueOf(2), app.getInterviews().get(1).getRoundNo());
        assertEquals(Integer.valueOf(3), app.getInterviews().get(2).getRoundNo());
    }

    @Test
    @DisplayName("Service: Case B - In-progress candidate APP002 gets overall score 86.50 from completed rounds")
    void testApplicationScores_CaseB_InProgressCandidate() {
        ApplicationDto app = applicationService.getApplicationById("APP002");
        assertNotNull(app, "APP002 must exist");
        assertNotNull(app.getScores(), "Scores DTO must be present");
        assertNotNull(app.getScoreCompletion(), "ScoreCompletion DTO must be present");

        assertEquals(0, BigDecimal.valueOf(88).compareTo(app.getScores().getOa()), "OA score must be 88");
        assertEquals(0, BigDecimal.valueOf(85).compareTo(app.getScores().getTechnical()), "Technical score must be 85");
        assertNull(app.getScores().getHr(), "HR score must be null (not yet conducted)");
        assertEquals(new BigDecimal("86.50"), app.getScores().getOverall(), "Overall score must be 86.50 ((88+85)/2)");
        assertEquals(2, app.getScores().getCompletedRounds(), "Completed scored rounds must be 2");

        assertTrue(app.getScoreCompletion().isOaCompleted(), "OA must be completed");
        assertFalse(app.getScoreCompletion().isTechnicalCompleted(), "Technical is PENDING, so completed is false");
        assertFalse(app.getScoreCompletion().isHrCompleted(), "HR is not conducted, so completed is false");
        assertFalse(app.getScoreCompletion().isAllRoundsCompleted(), "All rounds completed must be false");
    }

    @Test
    @DisplayName("Service: Case C - Candidate APP003 with only 1 round (OA=90) gets overall score 90.00")
    void testApplicationScores_CaseC_SingleRoundCandidate() {
        ApplicationDto app = applicationService.getApplicationById("APP003");
        assertNotNull(app, "APP003 must exist");
        assertNotNull(app.getScores());

        assertEquals(0, BigDecimal.valueOf(90).compareTo(app.getScores().getOa()), "OA score must be 90");
        assertNull(app.getScores().getTechnical(), "Technical score must be null");
        assertNull(app.getScores().getHr(), "HR score must be null");
        assertEquals(new BigDecimal("90.00"), app.getScores().getOverall(), "Overall score must be 90.00");
        assertEquals(1, app.getScores().getCompletedRounds(), "Completed scored rounds must be 1");
    }

    @Test
    @DisplayName("Service: Case D - Candidate with no evaluated rounds gets null overall score (never 0)")
    void testApplicationScores_CaseD_NoEvaluatedRounds() {
        ApplicationDto app = applicationService.getApplicationById("APP016");
        assertNotNull(app, "APP016 must exist");
        assertNotNull(app.getScores());

        assertNull(app.getScores().getOa(), "OA score must be null");
        assertNull(app.getScores().getTechnical(), "Technical score must be null");
        assertNull(app.getScores().getHr(), "HR score must be null");
        assertNull(app.getScores().getOverall(), "Overall score must be null (never 0)");
        assertEquals(0, app.getScores().getCompletedRounds(), "Completed scored rounds must be 0");
    }

    @Test
    @WithMockUser(username = "student1", roles = {"STUDENT"})
    @DisplayName("API: GET /api/v1/applications/APP001/scores returns 94.33 for owner")
    void testGetApplicationScores_OwnerAuthorized() throws Exception {
        mockMvc.perform(get("/api/v1/applications/APP001/scores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.oa").value(95.0))
                .andExpect(jsonPath("$.data.technical").value(92.0))
                .andExpect(jsonPath("$.data.hr").value(96.0))
                .andExpect(jsonPath("$.data.overall").value(94.33))
                .andExpect(jsonPath("$.data.completedRounds").value(3));
    }

    @Test
    @WithMockUser(username = "student1", roles = {"STUDENT"})
    @DisplayName("API: GET /api/v1/applications/APP002/scores returns 86.50 overall from 2 completed rounds")
    void testGetApplicationScores_InProgressCandidate() throws Exception {
        mockMvc.perform(get("/api/v1/applications/APP002/scores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.oa").value(88.0))
                .andExpect(jsonPath("$.data.technical").value(85.0))
                .andExpect(jsonPath("$.data.hr").doesNotExist())
                .andExpect(jsonPath("$.data.overall").value(86.50))
                .andExpect(jsonPath("$.data.completedRounds").value(2));
    }

    @Test
    @WithMockUser(username = "student2", roles = {"STUDENT"})
    @DisplayName("API Security: Student2 cannot access scores for APP001 (owned by STU001)")
    void testGetApplicationScores_CrossStudentForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/applications/APP001/scores"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "student1", roles = {"STUDENT"})
    @DisplayName("API: GET /api/v1/interviews/my returns authenticated student's interviews")
    void testGetMyInterviews_AuthenticatedStudent() throws Exception {
        mockMvc.perform(get("/api/v1/interviews/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[?(@.applicationId == 'APP001' && @.interviewerName == 'Vikram Malhotra')].score").value(95))
                .andExpect(jsonPath("$.data[?(@.applicationId == 'APP001' && @.interviewerName == 'Sangeeta Rao')].score").value(92))
                .andExpect(jsonPath("$.data[?(@.applicationId == 'APP001' && @.interviewerName == 'Arvind Swamy')].score").value(96));
    }

    // ============================================================
    // DRIVE ID TRACEABILITY & MULTIPLE OFFER MUTEX TEST SUITE
    // ============================================================

    @Test
    @DisplayName("Drive ID: Application entity and DTO preserve canonical Oracle drive ID (DRV001)")
    void testDriveIdTraceability() {
        ApplicationDto app1 = applicationService.getApplicationById("APP001");
        assertNotNull(app1.getDriveId(), "Drive ID must not be null");
        assertEquals("DRV001", app1.getDriveId(), "APP001 must have canonical DRV001 driveId");
        assertFalse(app1.getDriveId().contains("DRVXXX"), "Must never contain DRVXXX placeholder");

        ApplicationDto app2 = applicationService.getApplicationById("APP002");
        assertEquals("DRV002", app2.getDriveId(), "APP002 must have canonical DRV002 driveId");
    }

    @Test
    @WithMockUser(username = "student4", roles = {"STUDENT"})
    @DisplayName("Multiple Offers: Student4 receives multiple offers, selects one, competing offers become DECLINED")
    void testMultipleOffers_AtomicAcceptAndDecline() throws Exception {
        // 1. Verify student4 has 2 active offers: OFF004 (APP007) and OFF005 (APP008)
        mockMvc.perform(get("/api/v1/offers/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[?(@.offerId == 'OFF004')].status").value("OFFERED"))
                .andExpect(jsonPath("$.data[?(@.offerId == 'OFF005')].status").value("OFFERED"));

        // 2. Student4 accepts OFF004
        mockMvc.perform(post("/api/v1/offers/OFF004/accept"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.offerId").value("OFF004"))
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.data.acceptedAt").isNotEmpty());

        // 3. Verify OFF004 is ACCEPTED and OFF005 is DECLINED
        mockMvc.perform(get("/api/v1/offers/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.offerId == 'OFF004')].status").value("ACCEPTED"))
                .andExpect(jsonPath("$.data[?(@.offerId == 'OFF005')].status").value("DECLINED"));

        // 4. Verify applications reflect accepted and declined
        ApplicationDto app7 = applicationService.getApplicationById("APP007");
        assertEquals("ACCEPTED", app7.getStatus());
        ApplicationDto app8 = applicationService.getApplicationById("APP008");
        assertEquals("DECLINED", app8.getStatus());

        // 5. Verify student status is updated to PLACED
        Student stu4 = studentRepository.findById("STU004").orElseThrow();
        assertEquals("PLACED", stu4.getPlacementStatus());
    }

    @Test
    @WithMockUser(username = "student4", roles = {"STUDENT"})
    @DisplayName("Multiple Offers Mutex: Attempting to accept a second offer throws error")
    void testMultipleOffers_DoubleAcceptanceForbidden() throws Exception {
        // Accept first offer
        mockMvc.perform(post("/api/v1/offers/OFF004/accept"))
                .andExpect(status().isOk());

        // Attempt to accept second offer -> should fail with error
        mockMvc.perform(post("/api/v1/offers/OFF005/accept"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "student2", roles = {"STUDENT"})
    @DisplayName("Offer Authorization: Student2 cannot accept Student4's offer (OFF004)")
    void testOfferAcceptance_CrossStudentForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/offers/OFF004/accept"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "student1", roles = {"STUDENT"})
    @DisplayName("Package Consistency: Drive DRV001 and Offer OFF001 packages are strictly aligned at 44.00 LPA")
    void testPackageConsistency_MicrosoftDriveAndOfferAligned() throws Exception {
        // 1. Verify Placement Drive DRV001 package is 44.00 LPA
        mockMvc.perform(get("/api/v1/drives/DRV001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.driveId").value("DRV001"))
                .andExpect(jsonPath("$.data.ctc").value(44.00))
                .andExpect(jsonPath("$.data.packageLpa").value(44.00));

        // 2. Verify Offer OFF001 package is 44.00 LPA
        mockMvc.perform(get("/api/v1/offers/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.offerId == 'OFF001')].ctcLpa").value(44.00));

        // 3. Verify cross-drive CTC packages are accurately aligned across all seeded drives & offers
        mockMvc.perform(get("/api/v1/drives/DRV002"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ctc").value(7.50));

        mockMvc.perform(get("/api/v1/drives/DRV003"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ctc").value(9.50));

        mockMvc.perform(get("/api/v1/drives/DRV004"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ctc").value(18.50));
    }

    // ============================================================
    // OFFER RELEASE AND STATUS CONSTRAINTS TEST SUITE
    // ============================================================

    @Test
    @DisplayName("Offer Release: TCS DRV002 stores canonical CTC 7.50 LPA and STATUS = 'OFFERED' (no NULL status error)")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testOfferRelease_Tcs_7_50Lpa_StatusOffered() {
        com.placement.portal.application.Application app = new com.placement.portal.application.Application(
                "APP_TEST_TCS", "STU001", "DRV002", LocalDate.now(), "SELECTED"
        );
        applicationRepository.save(app);
        clearInterviewRounds("APP_TEST_TCS");

        com.placement.portal.offer.IssueOfferDto dto = new com.placement.portal.offer.IssueOfferDto(
                "APP_TEST_TCS", null, LocalDate.now()
        );
        com.placement.portal.offer.OfferDto result = offerService.issueOffer(dto);

        assertNotNull(result);
        assertNotNull(result.getOfferId());
        assertEquals("OFFERED", result.getStatus(), "OFFER_LETTER.STATUS must be 'OFFERED'");
        assertEquals(0, BigDecimal.valueOf(7.50).compareTo(result.getCtcLpa()), "CTC must be canonical 7.50 LPA from DRV002 record");

        com.placement.portal.offer.OfferLetter entity = offerRepository.findById(result.getOfferId()).orElseThrow();
        assertNotNull(entity.getStatus(), "Database column STATUS must not be null");
        assertEquals("OFFERED", entity.getStatus());
        assertEquals(0, BigDecimal.valueOf(7.50).compareTo(entity.getCtcLpa()));

        com.placement.portal.application.Application updatedApp = applicationRepository.findById("APP_TEST_TCS").orElseThrow();
        assertEquals("OFFERED", updatedApp.getStatus(), "Application status must transition to OFFERED");
    }

    @Test
    @DisplayName("Offer Release: Microsoft DRV001 stores canonical CTC 44.00 LPA")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testOfferRelease_Microsoft_44Lpa() {
        com.placement.portal.application.Application app = new com.placement.portal.application.Application(
                "APP_TEST_MS", "STU002", "DRV001", LocalDate.now(), "SELECTED"
        );
        applicationRepository.save(app);
        clearInterviewRounds("APP_TEST_MS");

        com.placement.portal.offer.IssueOfferDto dto = new com.placement.portal.offer.IssueOfferDto(
                "APP_TEST_MS", null, LocalDate.now()
        );
        com.placement.portal.offer.OfferDto result = offerService.issueOffer(dto);

        assertNotNull(result);
        assertEquals("OFFERED", result.getStatus());
        assertEquals(0, BigDecimal.valueOf(44.00).compareTo(result.getCtcLpa()), "CTC must be canonical 44.00 LPA for Microsoft DRV001");
    }

    @Test
    @DisplayName("Offer Release: Duplicate offers for the same application are strictly prevented")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testPreventDuplicateOffers() {
        com.placement.portal.application.Application app = new com.placement.portal.application.Application(
                "APP_TEST_DUP", "STU003", "DRV002", LocalDate.now(), "SELECTED"
        );
        applicationRepository.save(app);
        clearInterviewRounds("APP_TEST_DUP");

        com.placement.portal.offer.IssueOfferDto dto = new com.placement.portal.offer.IssueOfferDto(
                "APP_TEST_DUP", null, LocalDate.now()
        );
        offerService.issueOffer(dto);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            offerService.issueOffer(dto);
        });

        assertTrue(ex.getMessage().contains("already been issued"), "Duplicate offer attempt must be blocked");
    }

    @Test
    @DisplayName("API: POST /api/v1/offers successfully releases offer via controller with HTTP 200")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testOfferReleaseApiController() throws Exception {
        com.placement.portal.application.Application app = new com.placement.portal.application.Application(
                "APP_API_TEST", "STU004", "DRV002", LocalDate.now(), "SELECTED"
        );
        applicationRepository.save(app);
        clearInterviewRounds("APP_API_TEST");

        String jsonPayload = """
            {
                "applicationId": "APP_API_TEST",
                "offerDate": "2026-09-12"
            }
            """;

        mockMvc.perform(post("/api/v1/offers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("OFFERED"))
                .andExpect(jsonPath("$.data.ctcLpa").value(7.50));
    }
}

