package com.placement.portal.recruitment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.placement.portal.application.Application;
import com.placement.portal.application.ApplicationRepository;
import com.placement.portal.application.ApplicationService;
import com.placement.portal.auth.LoginRequest;
import com.placement.portal.company.JobCompany;
import com.placement.portal.company.JobCompanyRepository;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
@Transactional
public class PlacementDriveLifecycleTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PlacementDriveService driveService;

    @Autowired
    private PlacementDriveRepository driveRepository;

    @Autowired
    private DriveEligibilityRepository driveEligibilityRepository;

    @Autowired
    private JobCompanyRepository jobCompanyRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private String loginAndGetToken(String username, String password) throws Exception {
        LoginRequest req = new LoginRequest(username, password);
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        Map<String, Object> map = objectMapper.readValue(responseBody, Map.class);
        Map<String, Object> data = (Map<String, Object>) map.get("data");
        return (String) data.get("token");
    }

    // =========================================================================
    // PART 2: EXPIRED DRIVE LIFECYCLE & DATE BOUNDARY TESTS
    // =========================================================================

    @Test
    @DisplayName("Boundary Test 1: Future deadline drive is active and open for applications")
    void testFutureDeadlineDriveIsActive() {
        LocalDate today = driveService.getToday();
        PlacementDrive drive = new PlacementDrive(
                "DRV_TEST_FUTURE",
                "Systems Architecture Specialist",
                BigDecimal.valueOf(7.00),
                today.plusDays(20),
                today.plusDays(10),
                10,
                BigDecimal.valueOf(18.00),
                "Future drive test description",
                "Bengaluru",
                "Computer Science",
                "Round 1: Coding | Round 2: Tech"
        );
        driveRepository.save(drive);
        driveEligibilityRepository.save(new DriveEligibility(drive.getDriveId(), "BCE"));
        jobCompanyRepository.save(new JobCompany(drive.getJobTitle(), "COM001"));

        assertFalse(driveService.isDeadlinePassed(drive.getApplicationDeadline()), "Future deadline must not be passed");

        // Verify active drive list includes it
        List<PlacementDriveDto> activeDrives = driveService.getAllDrives(false);
        assertTrue(activeDrives.stream().anyMatch(d -> d.getDriveId().equals("DRV_TEST_FUTURE")),
                "Future deadline drive must be present in active drives listing");

        DriveEligibilityDto elig = driveService.evaluateEligibility("DRV_TEST_FUTURE", "STU001");
        assertTrue(elig.isEligible(), "Student STU001 must be eligible for active future drive");
        assertFalse(elig.isDeadlinePassed());
        assertTrue(elig.isDriveOpen());
    }

    @Test
    @DisplayName("Boundary Test 2: Expired drive is automatically excluded from active student listing")
    void testExpiredDriveIsExcludedFromActiveListing() {
        LocalDate today = driveService.getToday();
        PlacementDrive drive = new PlacementDrive(
                "DRV_TEST_EXPIRED",
                "Legacy Systems Maintainer",
                BigDecimal.valueOf(6.50),
                today.minusDays(5),
                today.minusDays(10),
                5,
                BigDecimal.valueOf(10.00),
                "Expired drive test description",
                "Pune",
                "Computer Science",
                "Round 1: OA"
        );
        driveRepository.save(drive);
        driveEligibilityRepository.save(new DriveEligibility(drive.getDriveId(), "BCE"));
        jobCompanyRepository.save(new JobCompany(drive.getJobTitle(), "COM001"));

        assertTrue(driveService.isDeadlinePassed(drive.getApplicationDeadline()), "Past deadline must be passed");

        // Excluded from active student list
        List<PlacementDriveDto> activeDrives = driveService.getAllDrives(false);
        assertFalse(activeDrives.stream().anyMatch(d -> d.getDriveId().equals("DRV_TEST_EXPIRED")),
                "Expired drive must be excluded from active student listings");

        // Included when includeExpired is true (for historical records / recruiter / admin)
        List<PlacementDriveDto> allDrives = driveService.getAllDrives(true);
        assertTrue(allDrives.stream().anyMatch(d -> d.getDriveId().equals("DRV_TEST_EXPIRED")),
                "Expired drive must remain visible in historical records");

        PlacementDriveDto expiredDto = allDrives.stream()
                .filter(d -> d.getDriveId().equals("DRV_TEST_EXPIRED"))
                .findFirst()
                .orElseThrow();
        assertEquals("EXPIRED", expiredDto.getStatus());
        assertTrue(expiredDto.isExpired());
    }

    @Test
    @DisplayName("Boundary Test 3: Drive at the exact deadline boundary (today) remains open")
    void testDeadlineBoundaryTodayRemainsOpen() {
        LocalDate today = driveService.getToday();
        PlacementDrive drive = new PlacementDrive(
                "DRV_TEST_TODAY",
                "Cloud DevSecOps Engineer",
                BigDecimal.valueOf(7.50),
                today.plusDays(7),
                today, // Exact boundary: deadline is today!
                8,
                BigDecimal.valueOf(16.00),
                "Boundary test description",
                "Hyderabad",
                "Computer Science",
                "Round 1: Screening"
        );
        driveRepository.save(drive);
        driveEligibilityRepository.save(new DriveEligibility(drive.getDriveId(), "BCE"));
        jobCompanyRepository.save(new JobCompany(drive.getJobTitle(), "COM001"));

        assertFalse(driveService.isDeadlinePassed(drive.getApplicationDeadline()),
                "Deadline on today must remain open until day has passed");

        List<PlacementDriveDto> activeDrives = driveService.getAllDrives(false);
        assertTrue(activeDrives.stream().anyMatch(d -> d.getDriveId().equals("DRV_TEST_TODAY")),
                "Drive with deadline today must appear in active listings");

        DriveEligibilityDto elig = driveService.evaluateEligibility("DRV_TEST_TODAY", "STU001");
        assertTrue(elig.isEligible(), "Student must be eligible on the day of the deadline");
        assertFalse(elig.isDeadlinePassed());
        assertTrue(elig.isDriveOpen());
    }

    @Test
    @DisplayName("Boundary Test 4: Application attempt against an expired drive is strictly rejected")
    void testApplicationAttemptAgainstExpiredDriveRejected() {
        LocalDate today = driveService.getToday();
        PlacementDrive drive = new PlacementDrive(
                "DRV_TEST_REJECT_APP",
                "Senior Blockchain Developer",
                BigDecimal.valueOf(7.00),
                today.minusDays(2),
                today.minusDays(8), // deadline passed
                5,
                BigDecimal.valueOf(25.00),
                "Expired drive",
                "Remote",
                "Computer Science",
                "Round 1: Coding"
        );
        driveRepository.save(drive);
        driveEligibilityRepository.save(new DriveEligibility(drive.getDriveId(), "BCE"));
        jobCompanyRepository.save(new JobCompany(drive.getJobTitle(), "COM001"));

        // Backend evaluateEligibility check
        DriveEligibilityDto elig = driveService.evaluateEligibility("DRV_TEST_REJECT_APP", "STU001");
        assertFalse(elig.isEligible(), "Expired drive must report student is ineligible");
        assertTrue(elig.isDeadlinePassed());
        assertFalse(elig.isDriveOpen());
        assertTrue(elig.getMessage().contains("Application deadline passed"));

        // ApplicationService.applyForDrive must throw IllegalArgumentException
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                applicationService.applyForDrive("STU001", "DRV_TEST_REJECT_APP", today));
        assertTrue(ex.getMessage().contains("Application deadline passed"));
    }

    @Test
    @DisplayName("Boundary Test 5: Existing application associated with a now-expired drive remains accessible")
    void testExistingApplicationForExpiredDrivePreserved() {
        LocalDate today = driveService.getToday();
        PlacementDrive drive = new PlacementDrive(
                "DRV_TEST_HIST_APP",
                "Database Reliability Engineer",
                BigDecimal.valueOf(7.00),
                today.minusDays(5),
                today.minusDays(15),
                4,
                BigDecimal.valueOf(20.00),
                "Historical drive",
                "Chennai",
                "Computer Science",
                "Round 1: SQL"
        );
        driveRepository.save(drive);
        driveEligibilityRepository.save(new DriveEligibility(drive.getDriveId(), "BCE"));
        jobCompanyRepository.save(new JobCompany(drive.getJobTitle(), "COM001"));

        // Existing historical application created in the past
        Application app = new Application("APP_HIST_01", "STU001", drive.getDriveId(), today.minusDays(20), "SHORTLISTED");
        applicationRepository.save(app);

        // Verify application remains accessible
        var retrievedApp = applicationService.getApplicationById("APP_HIST_01");
        assertNotNull(retrievedApp);
        assertEquals("STU001", retrievedApp.getStudentId());
        assertEquals("DRV_TEST_HIST_APP", retrievedApp.getDriveId());
        assertEquals("SHORTLISTED", retrievedApp.getStatus());

        // Verify drive details can still be queried by ID (history preserved)
        PlacementDriveDto retrievedDrive = driveService.getDriveById("DRV_TEST_HIST_APP");
        assertNotNull(retrievedDrive);
        assertEquals("Database Reliability Engineer", retrievedDrive.getJobTitle());
        assertEquals("EXPIRED", retrievedDrive.getStatus());
    }

    // =========================================================================
    // PART 3: RECRUITER PLACEMENT DRIVE CREATION TESTS
    // =========================================================================

    @Test
    @DisplayName("Recruiter Test 1: Authorized recruiter creates drive successfully with Oracle program catalog")
    void testAuthorizedRecruiterCreatesDrive() throws Exception {
        String token = loginAndGetToken("recruiter1", "password123"); // authorized for COM004 (Microsoft)
        LocalDate today = driveService.getToday();

        PlacementDriveDto createDto = new PlacementDriveDto();
        createDto.setCompanyId("COM004");
        createDto.setJobTitle("Principal Cloud AI Engineer");
        createDto.setMinCgpa(BigDecimal.valueOf(8.00));
        createDto.setCtc(BigDecimal.valueOf(35.00));
        createDto.setOpenings(12);
        createDto.setLocation("Bengaluru / Hyderabad");
        createDto.setApplicationDeadline(today.plusDays(14));
        createDto.setDriveDate(today.plusDays(25));
        createDto.setJobDescription("Build state-of-the-art cloud AI models and distributed training infrastructures.");
        createDto.setSelectionProcess("Round 1: Online Assessment | Round 2: Deep Learning Design | Round 3: Engineering VP");
        createDto.setEligibleProgramIds(List.of("BCE", "BCT", "BDS"));

        MvcResult result = mockMvc.perform(post("/api/v1/drives")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.jobTitle").value("Principal Cloud AI Engineer"))
                .andExpect(jsonPath("$.data.companyId").value("COM004"))
                .andExpect(jsonPath("$.data.minCgpa").value(8.00))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andReturn();

        String respStr = result.getResponse().getContentAsString();
        Map<String, Object> map = objectMapper.readValue(respStr, Map.class);
        Map<String, Object> data = (Map<String, Object>) map.get("data");
        String createdDriveId = (String) data.get("driveId");
        assertNotNull(createdDriveId);

        // Verify persistence in Oracle / Repository
        PlacementDrive persistedDrive = driveRepository.findById(createdDriveId).orElseThrow();
        assertEquals("Principal Cloud AI Engineer", persistedDrive.getJobTitle());
        assertEquals(0, BigDecimal.valueOf(8.00).compareTo(persistedDrive.getMinCgpa()));
        assertEquals(12, persistedDrive.getOpenings());

        // Verify JOB_COMPANY mapping
        JobCompany jc = jobCompanyRepository.findById("Principal Cloud AI Engineer").orElseThrow();
        assertEquals("COM004", jc.getCompanyId());

        // Verify DRIVE_ELIGIBILITY junction rows
        List<DriveEligibility> eligibilities = driveEligibilityRepository.findByDriveId(createdDriveId);
        assertEquals(3, eligibilities.size());
        List<String> savedProgIds = eligibilities.stream().map(DriveEligibility::getProgramId).toList();
        assertTrue(savedProgIds.containsAll(List.of("BCE", "BCT", "BDS")));
    }

    @Test
    @DisplayName("Recruiter Test 2: Recruiter cannot create a drive for an unauthorized company")
    void testRecruiterCannotCreateDriveForUnauthorizedCompany() throws Exception {
        // recruiter2 is authorized ONLY for COM001 (TCS), NOT COM004 (Microsoft)
        String token = loginAndGetToken("recruiter2", "password123");
        LocalDate today = driveService.getToday();

        PlacementDriveDto createDto = new PlacementDriveDto();
        createDto.setCompanyId("COM004"); // Attempt to hijack Microsoft
        createDto.setJobTitle("Unauthorized Cloud Engineer");
        createDto.setMinCgpa(BigDecimal.valueOf(7.00));
        createDto.setCtc(BigDecimal.valueOf(20.00));
        createDto.setOpenings(5);
        createDto.setLocation("Bengaluru");
        createDto.setApplicationDeadline(today.plusDays(10));
        createDto.setDriveDate(today.plusDays(20));
        createDto.setSelectionProcess("Round 1: Screening");
        createDto.setEligibleProgramIds(List.of("BCE"));

        mockMvc.perform(post("/api/v1/drives")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Recruiter Test 3: Student cannot create a placement drive (403 Forbidden)")
    void testStudentCannotCreateDrive() throws Exception {
        String token = loginAndGetToken("student1", "password123");
        LocalDate today = driveService.getToday();

        PlacementDriveDto createDto = new PlacementDriveDto();
        createDto.setCompanyId("COM001");
        createDto.setJobTitle("Student Created Drive");
        createDto.setMinCgpa(BigDecimal.valueOf(7.00));
        createDto.setCtc(BigDecimal.valueOf(15.00));
        createDto.setOpenings(10);
        createDto.setLocation("Bengaluru");
        createDto.setApplicationDeadline(today.plusDays(10));
        createDto.setDriveDate(today.plusDays(20));
        createDto.setSelectionProcess("Round 1: Test");
        createDto.setEligibleProgramIds(List.of("BCE"));

        mockMvc.perform(post("/api/v1/drives")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Recruiter Test 4: Form validation rejects invalid deadlines, missing programs, or negative openings")
    void testFormValidationRules() {
        LocalDate today = driveService.getToday();

        // 1. Missing job title
        PlacementDriveDto dto1 = new PlacementDriveDto();
        dto1.setCompanyId("COM001");
        dto1.setJobTitle("");
        assertThrows(IllegalArgumentException.class, () -> driveService.createDrive(dto1));

        // 2. Past application deadline
        PlacementDriveDto dto2 = new PlacementDriveDto();
        dto2.setCompanyId("COM001");
        dto2.setJobTitle("Valid Title");
        dto2.setApplicationDeadline(today.minusDays(1)); // past
        assertThrows(IllegalArgumentException.class, () -> driveService.createDrive(dto2));

        // 3. Drive date before deadline
        PlacementDriveDto dto3 = new PlacementDriveDto();
        dto3.setCompanyId("COM001");
        dto3.setJobTitle("Valid Title 2");
        dto3.setApplicationDeadline(today.plusDays(10));
        dto3.setDriveDate(today.plusDays(5)); // before deadline
        assertThrows(IllegalArgumentException.class, () -> driveService.createDrive(dto3));

        // 4. Non-positive openings
        PlacementDriveDto dto4 = new PlacementDriveDto();
        dto4.setCompanyId("COM001");
        dto4.setJobTitle("Valid Title 3");
        dto4.setApplicationDeadline(today.plusDays(10));
        dto4.setDriveDate(today.plusDays(15));
        dto4.setOpenings(0);
        assertThrows(IllegalArgumentException.class, () -> driveService.createDrive(dto4));

        // 5. No eligible programs selected
        PlacementDriveDto dto5 = new PlacementDriveDto();
        dto5.setCompanyId("COM001");
        dto5.setJobTitle("Valid Title 4");
        dto5.setApplicationDeadline(today.plusDays(10));
        dto5.setDriveDate(today.plusDays(15));
        dto5.setOpenings(5);
        dto5.setCtc(BigDecimal.valueOf(10.0));
        dto5.setEligibleProgramIds(List.of()); // empty
        assertThrows(IllegalArgumentException.class, () -> driveService.createDrive(dto5));
    }

    @Test
    @DisplayName("End-to-End: Created drive is visible to eligible students and permits application")
    void testEndToEndDriveCreationAndStudentApplication() throws Exception {
        String recruiterToken = loginAndGetToken("recruiter1", "password123");
        LocalDate today = driveService.getToday();

        // Recruiter creates a new drive
        PlacementDriveDto createDto = new PlacementDriveDto();
        createDto.setCompanyId("COM004");
        createDto.setJobTitle("Full-Stack Enterprise Architect");
        createDto.setMinCgpa(BigDecimal.valueOf(8.00));
        createDto.setCtc(BigDecimal.valueOf(28.00));
        createDto.setOpenings(10);
        createDto.setLocation("Bengaluru");
        createDto.setApplicationDeadline(today.plusDays(10));
        createDto.setDriveDate(today.plusDays(20));
        createDto.setJobDescription("Full stack development at global scale.");
        createDto.setSelectionProcess("Round 1: Coding Assessment | Round 2: System Architecture | Round 3: Fitment");
        createDto.setEligibleProgramIds(List.of("BCE")); // CSE only

        MvcResult result = mockMvc.perform(post("/api/v1/drives")
                        .header("Authorization", "Bearer " + recruiterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> map = objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
        Map<String, Object> data = (Map<String, Object>) map.get("data");
        String newDriveId = (String) data.get("driveId");

        // STU001 (CSE, CGPA 9.25) -> Eligible
        DriveEligibilityDto eligEligible = driveService.evaluateEligibility(newDriveId, "STU001");
        assertTrue(eligEligible.isEligible(), "STU001 must be eligible for the newly created drive");

        // STU009 (BME Mechanical, CGPA 7.60) -> Ineligible (program not eligible & CGPA < 8.00)
        DriveEligibilityDto eligIneligible = driveService.evaluateEligibility(newDriveId, "STU009");
        assertFalse(eligIneligible.isEligible(), "STU009 must be ineligible");

        // Student applies to the newly created drive
        var application = applicationService.applyForDrive("STU001", newDriveId, today);
        assertNotNull(application);
        assertEquals(newDriveId, application.getDriveId());
        assertEquals("STU001", application.getStudentId());
    }
}
