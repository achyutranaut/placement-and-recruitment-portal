package com.placement.portal.student;

import com.placement.portal.auth.PortalUser;
import com.placement.portal.auth.Role;
import com.placement.portal.auth.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
public class ResumeControllerSecurityTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StudentResumeRepository resumeRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private UserRepository userRepository;

    private static final String TEST_STUDENT_ID = "STU_TEST_RES";
    private static final String OTHER_STUDENT_ID = "STU_TEST_OTHER";

    @BeforeEach
    void setUp() {
        // Clean up test resumes
        resumeRepository.deleteAll(resumeRepository.findByStudentIdOrderByVersionNoDesc(TEST_STUDENT_ID));
        resumeRepository.deleteAll(resumeRepository.findByStudentIdOrderByVersionNoDesc(OTHER_STUDENT_ID));

        // Ensure test students exist
        if (!studentRepository.existsById(TEST_STUDENT_ID)) {
            studentRepository.save(new Student(
                    TEST_STUDENT_ID, "Test Resume Student", "test.resume@univ.edu",
                    "Campus St", "City", "State", java.time.LocalDate.of(2002, 5, 15), java.math.BigDecimal.valueOf(8.5), "CSE", "REG_RES_01", "PRG001"
            ));
        }
        if (!studentRepository.existsById(OTHER_STUDENT_ID)) {
            studentRepository.save(new Student(
                    OTHER_STUDENT_ID, "Other Student", "other.student@univ.edu",
                    "Campus St", "City", "State", java.time.LocalDate.of(2002, 6, 20), java.math.BigDecimal.valueOf(7.8), "ECE", "REG_RES_02", "PRG001"
            ));
        }

        // Ensure PortalUser exists for student_test and student_other
        if (userRepository.findByUsername("student_test").isEmpty()) {
            userRepository.save(new PortalUser("USR_TEST_RES", "student_test", "$2a$10$dummyHash", Role.ROLE_STUDENT, TEST_STUDENT_ID));
        }
        if (userRepository.findByUsername("student_other").isEmpty()) {
            userRepository.save(new PortalUser("USR_TEST_OTH", "student_other", "$2a$10$dummyHash", Role.ROLE_STUDENT, OTHER_STUDENT_ID));
        }
    }

    private StudentResume createTestResume(String id, String studentId, int version, String isCurrent, String filename, byte[] content) {
        StudentResume r = new StudentResume(
                id, studentId, filename, "application/pdf", (long) content.length,
                content, version, isCurrent
        );
        r.setUploadedAt(LocalDateTime.now().minusHours(10 - version));
        return resumeRepository.save(r);
    }

    @Test
    @DisplayName("Case A: Student deletes own old (archived) resume -> 200 OK and row/BLOB removed from DB")
    @WithMockUser(username = "student_test", roles = {"STUDENT"})
    void testStudentDeletesOwnOldResume() throws Exception {
        byte[] blobData = "%PDF-1.4 old resume content".getBytes(StandardCharsets.UTF_8);
        createTestResume("RES_OLD_1", TEST_STUDENT_ID, 1, "N", "old_resume_v1.pdf", blobData);
        createTestResume("RES_ACTIVE_2", TEST_STUDENT_ID, 2, "Y", "active_resume_v2.pdf", "%PDF-1.4 active".getBytes(StandardCharsets.UTF_8));

        assertTrue(resumeRepository.findById("RES_OLD_1").isPresent(), "Resume should exist before deletion");

        mockMvc.perform(delete("/api/v1/resumes/RES_OLD_1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.resumeId").value("RES_OLD_1"))
                .andExpect(jsonPath("$.data.versionNo").value(1));

        // Verify row and BLOB are removed from database
        Optional<StudentResume> afterDelete = resumeRepository.findById("RES_OLD_1");
        assertTrue(afterDelete.isEmpty(), "Row and BLOB must be completely removed from database");

        // Verify active resume remains intact
        Optional<StudentResume> activeResume = resumeRepository.findById("RES_ACTIVE_2");
        assertTrue(activeResume.isPresent(), "Active resume must remain intact");
        assertEquals("Y", activeResume.get().getIsCurrent());
    }

    @Test
    @DisplayName("Case B: Student attempts to delete active resume when it is the sole resume -> rejected 400")
    @WithMockUser(username = "student_test", roles = {"STUDENT"})
    void testStudentCannotDeleteSoleActiveResume() throws Exception {
        createTestResume("RES_SOLE_1", TEST_STUDENT_ID, 1, "Y", "only_resume.pdf", "%PDF-1.4".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(delete("/api/v1/resumes/RES_SOLE_1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("This is your active resume. Upload another resume and activate it before deleting this version."));

        // Verify it was NOT deleted
        assertTrue(resumeRepository.findById("RES_SOLE_1").isPresent(), "Sole active resume must not be deleted");
    }

    @Test
    @DisplayName("Case B2: Student attempts to delete active resume when multiple versions exist -> rejected with activation prompt")
    @WithMockUser(username = "student_test", roles = {"STUDENT"})
    void testStudentCannotDeleteActiveResumeWhenMultipleVersionsExist() throws Exception {
        createTestResume("RES_ARCH_1", TEST_STUDENT_ID, 1, "N", "v1.pdf", "%PDF-1.4".getBytes(StandardCharsets.UTF_8));
        createTestResume("RES_ACT_2", TEST_STUDENT_ID, 2, "Y", "v2.pdf", "%PDF-1.4".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(delete("/api/v1/resumes/RES_ACT_2"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Cannot delete active resume (Version 2). Please activate another resume version first before deleting this version."));

        assertTrue(resumeRepository.findById("RES_ACT_2").isPresent(), "Active resume must remain in DB");
    }

    @Test
    @DisplayName("Case C: Student attempts to delete another student's resume -> 403 Forbidden")
    @WithMockUser(username = "student_test", roles = {"STUDENT"})
    void testStudentCannotDeleteOtherStudentsResume() throws Exception {
        createTestResume("RES_OTHER_1", OTHER_STUDENT_ID, 1, "N", "other_resume.pdf", "%PDF-1.4".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(delete("/api/v1/resumes/RES_OTHER_1"))
                .andExpect(status().isForbidden());

        assertTrue(resumeRepository.findById("RES_OTHER_1").isPresent(), "Other student's resume must not be deleted");
    }

    @Test
    @DisplayName("Case D: Non-authenticated request -> 401 or 403")
    void testUnauthenticatedRequestIsRejected() throws Exception {
        mockMvc.perform(delete("/api/v1/resumes/RES_ANY"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Case E: Non-existent resume ID -> 404 Not Found")
    @WithMockUser(username = "student_test", roles = {"STUDENT"})
    void testDeleteNonExistentResumeReturns404() throws Exception {
        mockMvc.perform(delete("/api/v1/resumes/RES_DOES_NOT_EXIST_999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Case F & G: Delete v4 from sequence (v6, v5, v4, v3) -> v6, v5, v3 remain with immutable version numbers")
    @WithMockUser(username = "student_test", roles = {"STUDENT"})
    void testDeleteMiddleVersionDoesNotRenumberRemainingVersions() throws Exception {
        createTestResume("RES_V3", TEST_STUDENT_ID, 3, "N", "valid_resume_v3.pdf", "%PDF-1.4 v3".getBytes(StandardCharsets.UTF_8));
        createTestResume("RES_V4", TEST_STUDENT_ID, 4, "N", "valid_resume_v4.pdf", "%PDF-1.4 v4".getBytes(StandardCharsets.UTF_8));
        createTestResume("RES_V5", TEST_STUDENT_ID, 5, "N", "valid_resume_v5.pdf", "%PDF-1.4 v5".getBytes(StandardCharsets.UTF_8));
        createTestResume("RES_V6", TEST_STUDENT_ID, 6, "Y", "student_a_resume_v6.pdf", "%PDF-1.4 v6".getBytes(StandardCharsets.UTF_8));

        // Delete v4
        mockMvc.perform(delete("/api/v1/resumes/RES_V4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.versionNo").value(4));

        // Verify v4 is gone
        assertTrue(resumeRepository.findById("RES_V4").isEmpty());

        // Verify remaining versions: v6, v5, v3 (NO renumbering!)
        List<StudentResume> remaining = resumeRepository.findByStudentIdOrderByVersionNoDesc(TEST_STUDENT_ID);
        assertEquals(3, remaining.size());

        assertEquals(6, remaining.get(0).getVersionNo());
        assertEquals("RES_V6", remaining.get(0).getResumeId());
        assertEquals("Y", remaining.get(0).getIsCurrent());

        assertEquals(5, remaining.get(1).getVersionNo());
        assertEquals("RES_V5", remaining.get(1).getResumeId());
        assertEquals("N", remaining.get(1).getIsCurrent());

        assertEquals(3, remaining.get(2).getVersionNo());
        assertEquals("RES_V3", remaining.get(2).getResumeId());
        assertEquals("N", remaining.get(2).getIsCurrent());
    }

    @Test
    @DisplayName("Case H: Activating an archived version sets it as active and deactivates others")
    @WithMockUser(username = "student_test", roles = {"STUDENT"})
    void testActivateArchivedResume() throws Exception {
        createTestResume("RES_V1", TEST_STUDENT_ID, 1, "N", "v1.pdf", "%PDF-1.4 v1".getBytes(StandardCharsets.UTF_8));
        createTestResume("RES_V2", TEST_STUDENT_ID, 2, "Y", "v2.pdf", "%PDF-1.4 v2".getBytes(StandardCharsets.UTF_8));

        // Activate v1
        mockMvc.perform(put("/api/v1/resumes/RES_V1/activate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resumeId").value("RES_V1"))
                .andExpect(jsonPath("$.data.isCurrent").value("Y"));

        StudentResume v1 = resumeRepository.findById("RES_V1").orElseThrow();
        StudentResume v2 = resumeRepository.findById("RES_V2").orElseThrow();

        assertEquals("Y", v1.getIsCurrent());
        assertEquals("N", v2.getIsCurrent());

        // Now v2 is archived, so v2 can be safely deleted!
        mockMvc.perform(delete("/api/v1/resumes/RES_V2"))
                .andExpect(status().isOk());

        assertTrue(resumeRepository.findById("RES_V2").isEmpty());
    }

    @Test
    @DisplayName("Case I: Admin cleanup deletion succeeds on old resume")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testAdminCanDeleteAnyStudentOldResume() throws Exception {
        createTestResume("RES_STUD_OLD", TEST_STUDENT_ID, 1, "N", "stud_old.pdf", "%PDF-1.4".getBytes(StandardCharsets.UTF_8));
        createTestResume("RES_STUD_ACT", TEST_STUDENT_ID, 2, "Y", "stud_act.pdf", "%PDF-1.4".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(delete("/api/v1/resumes/RES_STUD_OLD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        assertTrue(resumeRepository.findById("RES_STUD_OLD").isEmpty());
    }
}
