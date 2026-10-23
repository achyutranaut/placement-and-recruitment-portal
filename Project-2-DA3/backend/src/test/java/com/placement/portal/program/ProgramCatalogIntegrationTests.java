package com.placement.portal.program;

import com.placement.portal.auth.AuthResponse;
import com.placement.portal.auth.AuthService;
import com.placement.portal.auth.StudentRegisterRequest;
import com.placement.portal.recruitment.DriveEligibility;
import com.placement.portal.recruitment.DriveEligibilityDto;
import com.placement.portal.recruitment.DriveEligibilityRepository;
import com.placement.portal.recruitment.PlacementDrive;
import com.placement.portal.recruitment.PlacementDriveRepository;
import com.placement.portal.recruitment.PlacementDriveService;
import com.placement.portal.student.Student;
import com.placement.portal.student.StudentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("h2")
@Transactional
public class ProgramCatalogIntegrationTests {

    @Autowired
    private ProgramService programService;

    @Autowired
    private ProgramRepository programRepository;

    @Autowired
    private AuthService authService;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private PlacementDriveRepository driveRepository;

    @Autowired
    private DriveEligibilityRepository driveEligibilityRepository;

    @Autowired
    private PlacementDriveService placementDriveService;

    @Test
    @DisplayName("Catalog contains all 107 active institutional programs across all 4 degree levels")
    void testCatalogCompleteness() {
        List<ProgramDto> programs = programService.getAllPrograms();
        assertTrue(programs.size() >= 107, "Catalog should have at least 107 active programs, found: " + programs.size());

        long bachelorCount = programs.stream().filter(p -> "Bachelor's".equals(p.getProgramLevel())).count();
        long masterCount = programs.stream().filter(p -> "Master's".equals(p.getProgramLevel())).count();
        long integratedCount = programs.stream().filter(p -> "Integrated".equals(p.getProgramLevel())).count();
        long doctoralCount = programs.stream().filter(p -> "Doctoral".equals(p.getProgramLevel())).count();

        assertEquals(47, bachelorCount, "Must have 47 Bachelor's programs");
        assertEquals(40, masterCount, "Must have 40 Master's programs");
        assertEquals(16, integratedCount, "Must have 16 Integrated programs");
        assertEquals(4, doctoralCount, "Must have 4 Doctoral programs");

        // Verify key sample programs
        assertTrue(programs.stream().anyMatch(p -> "BTECH-CSE".equals(p.getProgramId())));
        assertTrue(programs.stream().anyMatch(p -> "BTECH-CSE-AIML".equals(p.getProgramId())));
        assertTrue(programs.stream().anyMatch(p -> "BTECH-CSE-DS".equals(p.getProgramId())));
        assertTrue(programs.stream().anyMatch(p -> "BTECH-ECE".equals(p.getProgramId())));
        assertTrue(programs.stream().anyMatch(p -> "BTECH-MECH".equals(p.getProgramId())));
        assertTrue(programs.stream().anyMatch(p -> "BTECH-IT".equals(p.getProgramId())));
        assertTrue(programs.stream().anyMatch(p -> "BSC-CS".equals(p.getProgramId())));
        assertTrue(programs.stream().anyMatch(p -> "BBA".equals(p.getProgramId())));
        assertTrue(programs.stream().anyMatch(p -> "BCA".equals(p.getProgramId())));
        assertTrue(programs.stream().anyMatch(p -> "MTECH-CSE".equals(p.getProgramId())));
        assertTrue(programs.stream().anyMatch(p -> "MBA".equals(p.getProgramId())));
        assertTrue(programs.stream().anyMatch(p -> "MCA".equals(p.getProgramId())));
        assertTrue(programs.stream().anyMatch(p -> "MSC-DATA-SCI".equals(p.getProgramId())));
        assertTrue(programs.stream().anyMatch(p -> "INT-MTECH-CSE".equals(p.getProgramId())));
        assertTrue(programs.stream().anyMatch(p -> "PHD-ENG".equals(p.getProgramId())));
    }

    @Test
    @DisplayName("Student registration with canonical BTECH-CSE-AIML stores canonical ID and exact program name")
    void testRegisterWithBtechCseAiml() {
        StudentRegisterRequest req = new StudentRegisterRequest();
        req.setName("Aditya Varma");
        req.setEmail("aditya.varma@vitstudent.ac.in");
        req.setRegistrationNo("21BCE2045");
        req.setProgramId("BTECH-CSE-AIML");
        req.setBranch("B.Tech CSE (AI & ML)");
        req.setPassword("SecurePass@123");
        req.setCgpa(new BigDecimal("9.25"));
        req.setDob(LocalDate.of(2003, 5, 20));
        req.setPhone("+91-9876543210");

        AuthResponse resp = authService.registerStudent(req);
        assertNotNull(resp);
        assertNotNull(resp.getToken());

        Student student = studentRepository.findByRegistrationNo("21BCE2045").orElseThrow();
        assertEquals("BTECH-CSE-AIML", student.getProgramId());
        assertEquals("B.Tech CSE (AI & ML)", student.getBranch());
    }

    @Test
    @DisplayName("Student registration with MBA and MCA uses proper registration patterns")
    void testRegisterWithMbaAndMca() {
        StudentRegisterRequest reqMba = new StudentRegisterRequest();
        reqMba.setName("Meera Sen");
        reqMba.setEmail("meera.sen@vitstudent.ac.in");
        reqMba.setRegistrationNo("24MBA1042");
        reqMba.setProgramId("MBA");
        reqMba.setBranch("MBA");
        reqMba.setPassword("SecurePass@123");
        reqMba.setCgpa(new BigDecimal("8.70"));
        reqMba.setDob(LocalDate.of(2001, 8, 14));
        reqMba.setPhone("+91-9876543211");

        authService.registerStudent(reqMba);
        Student studentMba = studentRepository.findByRegistrationNo("24MBA1042").orElseThrow();
        assertEquals("MBA", studentMba.getProgramId());
        assertEquals("MBA", studentMba.getBranch());

        StudentRegisterRequest reqMca = new StudentRegisterRequest();
        reqMca.setName("Kunal Shah");
        reqMca.setEmail("kunal.shah@vitstudent.ac.in");
        reqMca.setRegistrationNo("24MCA1088");
        reqMca.setProgramId("MCA");
        reqMca.setBranch("MCA");
        reqMca.setPassword("SecurePass@123");
        reqMca.setCgpa(new BigDecimal("8.90"));
        reqMca.setDob(LocalDate.of(2001, 3, 10));
        reqMca.setPhone("+91-9876543212");

        authService.registerStudent(reqMca);
        Student studentMca = studentRepository.findByRegistrationNo("24MCA1088").orElseThrow();
        assertEquals("MCA", studentMca.getProgramId());
        assertEquals("MCA", studentMca.getBranch());
    }

    @Test
    @DisplayName("Registration rejects invalid non-existent PROGRAM_ID")
    void testRegistrationRejectsInvalidProgramId() {
        StudentRegisterRequest req = new StudentRegisterRequest();
        req.setName("Fake User");
        req.setEmail("fake.user@vitstudent.ac.in");
        req.setRegistrationNo("21BCE9991");
        req.setProgramId("INVALID-PROG-ID");
        req.setBranch("Unknown");
        req.setPassword("SecurePass@123");
        req.setCgpa(new BigDecimal("8.00"));
        req.setDob(LocalDate.of(2003, 1, 1));
        req.setPhone("+91-9876543213");

        assertThrows(IllegalArgumentException.class, () -> authService.registerStudent(req));
    }

    @Test
    @DisplayName("Registration rejects invalid registration number format for selected program")
    void testRegistrationRejectsInvalidRegNoFormat() {
        StudentRegisterRequest req = new StudentRegisterRequest();
        req.setName("Wrong Reg User");
        req.setEmail("wrong.reg@vitstudent.ac.in");
        // B.Tech CSE requires ^2[0-9]BCE[0-9]{4}$ - using invalid format
        req.setRegistrationNo("INVALIDREGNO");
        req.setProgramId("BTECH-CSE");
        req.setBranch("B.Tech CSE");
        req.setPassword("SecurePass@123");
        req.setCgpa(new BigDecimal("8.00"));
        req.setDob(LocalDate.of(2003, 1, 1));
        req.setPhone("+91-9876543214");

        assertThrows(IllegalArgumentException.class, () -> authService.registerStudent(req));
    }

    @Test
    @DisplayName("Placement eligibility: Drive eligible for BTECH-CSE; Student BTECH-CSE CGPA 9.25 -> ELIGIBLE")
    void testEligibilityBtechCseEligible() {
        PlacementDrive drive = new PlacementDrive(
                "TEST_DRV_01",
                "Software Engineer",
                new BigDecimal("8.00"),
                LocalDate.now().plusDays(10),
                LocalDate.now().plusDays(5),
                10,
                new BigDecimal("20.00"),
                "SDE Role",
                "Bengaluru",
                "B.Tech CSE",
                "OA -> Tech -> HR"
        );
        driveRepository.save(drive);
        driveEligibilityRepository.save(new DriveEligibility("TEST_DRV_01", "BTECH-CSE"));

        Student student = new Student(
                "TEST_STU_01",
                "Dev CSE",
                "dev.cse@vitstudent.ac.in",
                "Street 1", "Chennai", "Tamil Nadu",
                LocalDate.of(2003, 2, 2),
                new BigDecimal("9.25"),
                "B.Tech CSE",
                "21BCE3001",
                "BTECH-CSE"
        );
        studentRepository.save(student);

        DriveEligibilityDto result = placementDriveService.evaluateEligibility("TEST_DRV_01", "TEST_STU_01");
        assertTrue(result.isEligible(), "Student should be fully eligible");
        assertTrue(result.isProgramEligible(), "Program must be eligible");
        assertTrue(result.isCgpaEligible(), "CGPA must be eligible");
    }

    @Test
    @DisplayName("Placement eligibility: Drive eligible for BTECH-CSE-AIML; Student BTECH-CSE-AIML CGPA 9.25 -> ELIGIBLE")
    void testEligibilityBtechCseAimlEligible() {
        PlacementDrive drive = new PlacementDrive(
                "TEST_DRV_02",
                "AI/ML Engineer",
                new BigDecimal("8.50"),
                LocalDate.now().plusDays(10),
                LocalDate.now().plusDays(5),
                5,
                new BigDecimal("28.00"),
                "AI/ML Specialist",
                "Hyderabad",
                "B.Tech CSE (AI & ML)",
                "OA -> ML Interview -> HR"
        );
        driveRepository.save(drive);
        driveEligibilityRepository.save(new DriveEligibility("TEST_DRV_02", "BTECH-CSE-AIML"));

        Student student = new Student(
                "TEST_STU_02",
                "Dev AIML",
                "dev.aiml@vitstudent.ac.in",
                "Street 2", "Chennai", "Tamil Nadu",
                LocalDate.of(2003, 3, 3),
                new BigDecimal("9.25"),
                "B.Tech CSE (AI & ML)",
                "21BCE3002",
                "BTECH-CSE-AIML"
        );
        studentRepository.save(student);

        DriveEligibilityDto result = placementDriveService.evaluateEligibility("TEST_DRV_02", "TEST_STU_02");
        assertTrue(result.isEligible(), "Student should be eligible for AI/ML drive");
        assertTrue(result.isProgramEligible(), "Program must be eligible");
    }

    @Test
    @DisplayName("Placement eligibility: Drive eligible ONLY for BTECH-MECH; Student BTECH-CSE -> NOT ELIGIBLE")
    void testEligibilityMechDriveRejectsCseStudent() {
        PlacementDrive drive = new PlacementDrive(
                "TEST_DRV_03",
                "Design Engineer",
                new BigDecimal("7.00"),
                LocalDate.now().plusDays(10),
                LocalDate.now().plusDays(5),
                8,
                new BigDecimal("12.00"),
                "Mechanical Design",
                "Pune",
                "B.Tech Mechanical",
                "Technical Test -> Interview"
        );
        driveRepository.save(drive);
        driveEligibilityRepository.save(new DriveEligibility("TEST_DRV_03", "BTECH-MECH"));

        Student student = new Student(
                "TEST_STU_03",
                "Dev CSE Reject",
                "dev.reject@vitstudent.ac.in",
                "Street 3", "Chennai", "Tamil Nadu",
                LocalDate.of(2003, 4, 4),
                new BigDecimal("9.50"),
                "B.Tech CSE",
                "21BCE3003",
                "BTECH-CSE"
        );
        studentRepository.save(student);

        DriveEligibilityDto result = placementDriveService.evaluateEligibility("TEST_DRV_03", "TEST_STU_03");
        assertFalse(result.isEligible(), "CSE student must NOT be eligible for Mechanical-only drive");
        assertFalse(result.isProgramEligible(), "Program must not be eligible");
        assertTrue(result.isCgpaEligible(), "CGPA is above threshold");
    }

    @Test
    @DisplayName("Placement eligibility: Drive eligible for CSE, AIML, DS; Student Data Science -> ELIGIBLE (No fuzzy string matching)")
    void testEligibilityMultiProgramMatching() {
        PlacementDrive drive = new PlacementDrive(
                "TEST_DRV_04",
                "Data Analytics Engineer",
                new BigDecimal("8.00"),
                LocalDate.now().plusDays(10),
                LocalDate.now().plusDays(5),
                12,
                new BigDecimal("18.00"),
                "Data Role",
                "Bengaluru",
                "B.Tech CSE, B.Tech CSE (AI & ML), B.Tech CSE (Data Science)",
                "OA -> Technical -> HR"
        );
        driveRepository.save(drive);
        driveEligibilityRepository.save(new DriveEligibility("TEST_DRV_04", "BTECH-CSE"));
        driveEligibilityRepository.save(new DriveEligibility("TEST_DRV_04", "BTECH-CSE-AIML"));
        driveEligibilityRepository.save(new DriveEligibility("TEST_DRV_04", "BTECH-CSE-DS"));

        Student studentDs = new Student(
                "TEST_STU_04",
                "Dev DS",
                "dev.ds@vitstudent.ac.in",
                "Street 4", "Bengaluru", "Karnataka",
                LocalDate.of(2003, 7, 7),
                new BigDecimal("8.60"),
                "B.Tech CSE (Data Science)",
                "21BCE3004",
                "BTECH-CSE-DS"
        );
        studentRepository.save(studentDs);

        DriveEligibilityDto result = placementDriveService.evaluateEligibility("TEST_DRV_04", "TEST_STU_04");
        assertTrue(result.isEligible(), "BTECH-CSE-DS student must be eligible for multi-program drive");
        assertTrue(result.isProgramEligible());
    }
}
