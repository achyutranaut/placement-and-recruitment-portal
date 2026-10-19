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

    @Autowired
    private com.placement.portal.recruitment.ResumeEvaluationRepository evaluationRepository;

    private static final String TEST_STUDENT_ID = "STU_TEST_RES";
    private static final String OTHER_STUDENT_ID = "STU_TEST_OTHER";

    @BeforeEach
    void setUp() {
        // Clean up any referencing evaluations first, then test resumes
        for (StudentResume r : resumeRepository.findByStudentIdOrderByVersionNoDesc(TEST_STUDENT_ID)) {
            evaluationRepository.deleteAll(evaluationRepository.findByResumeId(r.getResumeId()));
        }
        for (StudentResume r : resumeRepository.findByStudentIdOrderByVersionNoDesc(OTHER_STUDENT_ID)) {
            evaluationRepository.deleteAll(evaluationRepository.findByResumeId(r.getResumeId()));
        }
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

    // =========================================================================
    // SET-ACTIVE WORKFLOW TEST SUITE (CASES 1 - 12)
    // =========================================================================

    @Test
    @DisplayName("CASE 1: Student has V1 (active) and V2 (inactive); activates V2 -> V2 is active and V1 is inactive")
    @WithMockUser(username = "student_test", roles = {"STUDENT"})
    void testCase1_ActivateV2FromV1() throws Exception {
        createTestResume("RES_C1_V1", TEST_STUDENT_ID, 1, "Y", "resume_v1.pdf", "%PDF-1.4 version 1".getBytes(StandardCharsets.UTF_8));
        createTestResume("RES_C1_V2", TEST_STUDENT_ID, 2, "N", "resume_v2.pdf", "%PDF-1.4 version 2".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(put("/api/v1/resumes/RES_C1_V2/activate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.resumeId").value("RES_C1_V2"))
                .andExpect(jsonPath("$.data.versionNo").value(2))
                .andExpect(jsonPath("$.data.isCurrent").value("Y"))
                .andExpect(jsonPath("$.data.isActive").value("Y"))
                .andExpect(jsonPath("$.data.active").value(true));

        assertEquals("N", resumeRepository.findById("RES_C1_V1").orElseThrow().getIsCurrent());
        assertEquals("Y", resumeRepository.findById("RES_C1_V2").orElseThrow().getIsCurrent());
        assertEquals(1L, resumeRepository.countByStudentIdAndIsCurrent(TEST_STUDENT_ID, "Y"));
    }

    @Test
    @DisplayName("CASE 2: Student has 3 versions (V1, V2 active, V3); activates V3 -> V3 is active and V1, V2 are inactive")
    @WithMockUser(username = "student_test", roles = {"STUDENT"})
    void testCase2_ThreeVersionsActivateV3FromV2() throws Exception {
        createTestResume("RES_C2_V1", TEST_STUDENT_ID, 1, "N", "resume_v1.pdf", "%PDF-1.4 v1".getBytes(StandardCharsets.UTF_8));
        createTestResume("RES_C2_V2", TEST_STUDENT_ID, 2, "Y", "resume_v2.pdf", "%PDF-1.4 v2".getBytes(StandardCharsets.UTF_8));
        createTestResume("RES_C2_V3", TEST_STUDENT_ID, 3, "N", "resume_v3.pdf", "%PDF-1.4 v3".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(put("/api/v1/resumes/RES_C2_V3/activate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.resumeId").value("RES_C2_V3"))
                .andExpect(jsonPath("$.data.versionNo").value(3))
                .andExpect(jsonPath("$.data.isCurrent").value("Y"));

        assertEquals("N", resumeRepository.findById("RES_C2_V1").orElseThrow().getIsCurrent());
        assertEquals("N", resumeRepository.findById("RES_C2_V2").orElseThrow().getIsCurrent());
        assertEquals("Y", resumeRepository.findById("RES_C2_V3").orElseThrow().getIsCurrent());
        assertEquals(1L, resumeRepository.countByStudentIdAndIsCurrent(TEST_STUDENT_ID, "Y"));
    }

    @Test
    @DisplayName("CASE 3: Student activates already-active version -> idempotent, safe, does not corrupt data")
    @WithMockUser(username = "student_test", roles = {"STUDENT"})
    void testCase3_ActivateAlreadyActiveVersionIsIdempotent() throws Exception {
        byte[] v1Bytes = "%PDF-1.4 original v1 content".getBytes(StandardCharsets.UTF_8);
        byte[] v2Bytes = "%PDF-1.4 active v2 content".getBytes(StandardCharsets.UTF_8);
        createTestResume("RES_C3_V1", TEST_STUDENT_ID, 1, "N", "v1.pdf", v1Bytes);
        createTestResume("RES_C3_V2", TEST_STUDENT_ID, 2, "Y", "v2.pdf", v2Bytes);

        mockMvc.perform(put("/api/v1/resumes/RES_C3_V2/activate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.resumeId").value("RES_C3_V2"))
                .andExpect(jsonPath("$.data.isCurrent").value("Y"));

        StudentResume v1 = resumeRepository.findById("RES_C3_V1").orElseThrow();
        StudentResume v2 = resumeRepository.findById("RES_C3_V2").orElseThrow();
        assertEquals("N", v1.getIsCurrent());
        assertEquals("Y", v2.getIsCurrent());
        assertArrayEquals(v1Bytes, v1.getResumeData());
        assertArrayEquals(v2Bytes, v2.getResumeData());
        assertEquals(1L, resumeRepository.countByStudentIdAndIsCurrent(TEST_STUDENT_ID, "Y"));
    }

    @Test
    @DisplayName("CASE 4: Student attempts to activate another student's resume -> 403 Forbidden and neither student's state is modified")
    @WithMockUser(username = "student_test", roles = {"STUDENT"})
    void testCase4_ActivateOtherStudentsResumeRejected() throws Exception {
        createTestResume("RES_C4_MINE_V1", TEST_STUDENT_ID, 1, "Y", "mine_v1.pdf", "%PDF-1.4 mine1".getBytes(StandardCharsets.UTF_8));
        createTestResume("RES_C4_MINE_V2", TEST_STUDENT_ID, 2, "N", "mine_v2.pdf", "%PDF-1.4 mine2".getBytes(StandardCharsets.UTF_8));

        createTestResume("RES_C4_OTHER_V1", OTHER_STUDENT_ID, 1, "Y", "other_v1.pdf", "%PDF-1.4 other1".getBytes(StandardCharsets.UTF_8));
        createTestResume("RES_C4_OTHER_V2", OTHER_STUDENT_ID, 2, "N", "other_v2.pdf", "%PDF-1.4 other2".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(put("/api/v1/resumes/RES_C4_OTHER_V2/activate"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));

        // Verify neither student's active resume changed
        assertEquals("Y", resumeRepository.findById("RES_C4_MINE_V1").orElseThrow().getIsCurrent());
        assertEquals("N", resumeRepository.findById("RES_C4_MINE_V2").orElseThrow().getIsCurrent());
        assertEquals("Y", resumeRepository.findById("RES_C4_OTHER_V1").orElseThrow().getIsCurrent());
        assertEquals("N", resumeRepository.findById("RES_C4_OTHER_V2").orElseThrow().getIsCurrent());
    }

    @Test
    @DisplayName("CASE 5: Selected resume ID does not exist -> 404 Not Found and previous active resume remains unchanged")
    @WithMockUser(username = "student_test", roles = {"STUDENT"})
    void testCase5_ActivateNonExistentResumeReturns404AndPreservesActive() throws Exception {
        createTestResume("RES_C5_V1", TEST_STUDENT_ID, 1, "Y", "v1.pdf", "%PDF-1.4 v1".getBytes(StandardCharsets.UTF_8));
        createTestResume("RES_C5_V2", TEST_STUDENT_ID, 2, "N", "v2.pdf", "%PDF-1.4 v2".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(put("/api/v1/resumes/RES_NON_EXISTENT_ID_999/activate"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));

        assertEquals("Y", resumeRepository.findById("RES_C5_V1").orElseThrow().getIsCurrent());
        assertEquals("N", resumeRepository.findById("RES_C5_V2").orElseThrow().getIsCurrent());
        assertEquals(1L, resumeRepository.countByStudentIdAndIsCurrent(TEST_STUDENT_ID, "Y"));
    }

    @Autowired
    private org.springframework.transaction.PlatformTransactionManager transactionManager;

    @Test
    @DisplayName("CASE 6: Database transaction fails mid-activation -> rollback preserves previous active resume committed")
    void testCase6_TransactionFailureRollsBackAndPreservesPreviousActiveState() {
        createTestResume("RES_C6_V1", TEST_STUDENT_ID, 1, "Y", "v1.pdf", "%PDF-1.4 v1".getBytes(StandardCharsets.UTF_8));
        createTestResume("RES_C6_V2", TEST_STUDENT_ID, 2, "N", "v2.pdf", "%PDF-1.4 v2".getBytes(StandardCharsets.UTF_8));

        org.springframework.transaction.support.TransactionTemplate txTemplate =
                new org.springframework.transaction.support.TransactionTemplate(transactionManager);

        assertThrows(RuntimeException.class, () -> txTemplate.executeWithoutResult(status -> {
            // Step 1 of activation: deactivate all resumes for student
            resumeRepository.deactivateAllForStudent(TEST_STUDENT_ID);
            // Simulate a database/runtime failure before Step 2 commits
            throw new RuntimeException("Simulated database failure after deactivating resumes");
        }));

        // Verify rollback preserved RES_C6_V1 as active ('Y') and RES_C6_V2 as inactive ('N')
        assertEquals("Y", resumeRepository.findById("RES_C6_V1").orElseThrow().getIsCurrent());
        assertEquals("N", resumeRepository.findById("RES_C6_V2").orElseThrow().getIsCurrent());
        assertEquals(1L, resumeRepository.countByStudentIdAndIsCurrent(TEST_STUDENT_ID, "Y"));
    }

    @Test
    @DisplayName("CASE 7: Activation succeeds -> refreshing page (re-fetching student resumes and current resume) shows new active resume")
    @WithMockUser(username = "student_test", roles = {"STUDENT"})
    void testCase7_RefreshAfterActivationShowsNewActiveResume() throws Exception {
        createTestResume("RES_C7_V1", TEST_STUDENT_ID, 1, "N", "v1_archived.pdf", "%PDF-1.4 v1".getBytes(StandardCharsets.UTF_8));
        createTestResume("RES_C7_V2", TEST_STUDENT_ID, 2, "Y", "v2_current.pdf", "%PDF-1.4 v2".getBytes(StandardCharsets.UTF_8));

        // Activate V1
        mockMvc.perform(put("/api/v1/resumes/RES_C7_V1/activate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resumeId").value("RES_C7_V1"))
                .andExpect(jsonPath("$.data.isCurrent").value("Y"));

        // Simulate page refresh: GET /api/v1/resumes/student/{studentId}/current
        mockMvc.perform(get("/api/v1/resumes/student/" + TEST_STUDENT_ID + "/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resumeId").value("RES_C7_V1"))
                .andExpect(jsonPath("$.data.versionNo").value(1))
                .andExpect(jsonPath("$.data.fileName").value("v1_archived.pdf"))
                .andExpect(jsonPath("$.data.isCurrent").value("Y"))
                .andExpect(jsonPath("$.data.isActive").value("Y"));

        // Simulate page refresh: GET /api/v1/resumes/student/{studentId}
        mockMvc.perform(get("/api/v1/resumes/student/" + TEST_STUDENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].resumeId").value("RES_C7_V2"))
                .andExpect(jsonPath("$.data[0].isCurrent").value("N"))
                .andExpect(jsonPath("$.data[1].resumeId").value("RES_C7_V1"))
                .andExpect(jsonPath("$.data[1].isCurrent").value("Y"));
    }

    @Test
    @DisplayName("CASE 8 & CASE 9: Download and Preview active resume after activation -> returns newly active BLOB with no-store headers")
    @WithMockUser(username = "student_test", roles = {"STUDENT"})
    void testCase8And9_DownloadAndPreviewActiveResumeAfterActivation() throws Exception {
        byte[] v1Payload = "%PDF-1.4 DISTINCT_PAYLOAD_VERSION_ONE".getBytes(StandardCharsets.UTF_8);
        byte[] v2Payload = "%PDF-1.4 DISTINCT_PAYLOAD_VERSION_TWO".getBytes(StandardCharsets.UTF_8);

        createTestResume("RES_C8_V1", TEST_STUDENT_ID, 1, "Y", "resume_v1.pdf", v1Payload);
        createTestResume("RES_C8_V2", TEST_STUDENT_ID, 2, "N", "resume_v2.pdf", v2Payload);

        // Activate V2
        mockMvc.perform(put("/api/v1/resumes/RES_C8_V2/activate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resumeId").value("RES_C8_V2"))
                .andExpect(jsonPath("$.data.isCurrent").value("Y"));

        // CASE 8: Download via /student/{studentId}/current/download returns V2 BLOB bytes
        byte[] downloadedCurrent = mockMvc.perform(get("/api/v1/resumes/student/" + TEST_STUDENT_ID + "/current/download"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("resume_v2.pdf")))
                .andReturn().getResponse().getContentAsByteArray();
        assertArrayEquals(v2Payload, downloadedCurrent, "Downloaded active resume BLOB must match newly activated V2");

        // CASE 9: Preview stream via /{resumeId}/download for active resume returns V2 BLOB bytes
        byte[] previewBytes = mockMvc.perform(get("/api/v1/resumes/RES_C8_V2/download"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
                .andReturn().getResponse().getContentAsByteArray();
        assertArrayEquals(v2Payload, previewBytes, "Preview stream must return newly activated V2 BLOB");
    }

    @Test
    @DisplayName("CASE 10: Sequential activation V2 then V3 -> V3 remains active after page refresh and new backend read")
    @WithMockUser(username = "student_test", roles = {"STUDENT"})
    void testCase10_SequentialActivationV2ThenV3() throws Exception {
        createTestResume("RES_C10_V1", TEST_STUDENT_ID, 1, "Y", "v1.pdf", "%PDF-1.4 v1".getBytes(StandardCharsets.UTF_8));
        createTestResume("RES_C10_V2", TEST_STUDENT_ID, 2, "N", "v2.pdf", "%PDF-1.4 v2".getBytes(StandardCharsets.UTF_8));
        createTestResume("RES_C10_V3", TEST_STUDENT_ID, 3, "N", "v3.pdf", "%PDF-1.4 v3".getBytes(StandardCharsets.UTF_8));

        // Activate V2
        mockMvc.perform(put("/api/v1/resumes/RES_C10_V2/activate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resumeId").value("RES_C10_V2"))
                .andExpect(jsonPath("$.data.isCurrent").value("Y"));

        // Immediately activate V3
        mockMvc.perform(put("/api/v1/resumes/RES_C10_V3/activate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resumeId").value("RES_C10_V3"))
                .andExpect(jsonPath("$.data.isCurrent").value("Y"));

        // Verify via fresh backend read
        mockMvc.perform(get("/api/v1/resumes/student/" + TEST_STUDENT_ID + "/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resumeId").value("RES_C10_V3"))
                .andExpect(jsonPath("$.data.versionNo").value(3))
                .andExpect(jsonPath("$.data.isCurrent").value("Y"));

        assertEquals("N", resumeRepository.findById("RES_C10_V1").orElseThrow().getIsCurrent());
        assertEquals("N", resumeRepository.findById("RES_C10_V2").orElseThrow().getIsCurrent());
        assertEquals("Y", resumeRepository.findById("RES_C10_V3").orElseThrow().getIsCurrent());
        assertEquals(1L, resumeRepository.countByStudentIdAndIsCurrent(TEST_STUDENT_ID, "Y"));
    }

    @Autowired
    private IResumeService resumeService;

    @Test
    @DisplayName("CASE 11: Two concurrent activation requests arrive close together -> DB ends in valid state with exactly 1 active resume")
    void testCase11_ConcurrentActivationRequestsMaintainSingleActiveInvariant() throws Exception {
        createTestResume("RES_C11_V1", TEST_STUDENT_ID, 1, "Y", "v1.pdf", "%PDF-1.4 v1".getBytes(StandardCharsets.UTF_8));
        createTestResume("RES_C11_V2", TEST_STUDENT_ID, 2, "N", "v2.pdf", "%PDF-1.4 v2".getBytes(StandardCharsets.UTF_8));
        createTestResume("RES_C11_V3", TEST_STUDENT_ID, 3, "N", "v3.pdf", "%PDF-1.4 v3".getBytes(StandardCharsets.UTF_8));

        java.util.concurrent.CountDownLatch startGate = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(2);

        java.util.concurrent.Future<ResumeDto> f1 = pool.submit(() -> {
            startGate.await();
            return resumeService.activateResume(TEST_STUDENT_ID, "RES_C11_V2", false);
        });
        java.util.concurrent.Future<ResumeDto> f2 = pool.submit(() -> {
            startGate.await();
            return resumeService.activateResume(TEST_STUDENT_ID, "RES_C11_V3", false);
        });

        startGate.countDown();
        f1.get(5, java.util.concurrent.TimeUnit.SECONDS);
        f2.get(5, java.util.concurrent.TimeUnit.SECONDS);
        pool.shutdown();

        long activeCount = resumeRepository.countByStudentIdAndIsCurrent(TEST_STUDENT_ID, "Y");
        assertEquals(1L, activeCount, "Concurrent activations must leave exactly 1 active resume version");
        ResumeDto finalActive = resumeService.getCurrentResume(TEST_STUDENT_ID);
        assertNotNull(finalActive);
        assertTrue("RES_C11_V2".equals(finalActive.getResumeId()) || "RES_C11_V3".equals(finalActive.getResumeId()));
    }

    @Autowired
    private com.placement.portal.recruitment.IRecruiterEvaluationService evaluationService;

    @Autowired
    private com.placement.portal.application.ApplicationRepository applicationRepository;

    @Autowired
    private com.placement.portal.recruitment.PlacementDriveRepository driveRepository;

    @Autowired
    private com.placement.portal.company.CompanyRepository companyRepository;

    @Test
    @DisplayName("CASE 12: Placement application workflow uses newly active resume while preserving historical evaluation records")
    @WithMockUser(username = "student_test", roles = {"STUDENT"})
    void testCase12_PlacementApplicationWorkflowUsesNewlyActiveResumeAndPreservesHistory() throws Exception {
        createTestResume("RES_C12_V1", TEST_STUDENT_ID, 1, "Y", "v1_initial.pdf", "%PDF-1.4 v1".getBytes(StandardCharsets.UTF_8));
        createTestResume("RES_C12_V2", TEST_STUDENT_ID, 2, "N", "v2_updated.pdf", "%PDF-1.4 v2".getBytes(StandardCharsets.UTF_8));

        // Ensure company, drive, and application exist for historical evaluation check
        if (!companyRepository.existsById("CMP_C12")) {
            companyRepository.save(new com.placement.portal.company.Company("CMP_C12", "Case12 Corp", "hr@c12.com"));
        }
        if (!driveRepository.existsById("DRV_C12")) {
            driveRepository.save(new com.placement.portal.recruitment.PlacementDrive(
                    "DRV_C12", "Software Engineer", java.math.BigDecimal.valueOf(7.0),
                    java.time.LocalDate.now().plusDays(10), java.time.LocalDate.now().plusDays(20),
                    5, java.math.BigDecimal.valueOf(18.0),
                    "Software Engineer Role", "Bengaluru", "CSE, ECE", "OA -> GD -> HR"
            ));
        }
        if (!applicationRepository.existsById("APP_C12")) {
            applicationRepository.save(new com.placement.portal.application.Application(
                    "APP_C12", TEST_STUDENT_ID, "DRV_C12", java.time.LocalDate.now(), "APPLIED"
            ));
        }

        // Recruiter evaluates application while V1 is active -> records RES_C12_V1 on APP_C12
        ResumeDto activeBefore = resumeService.getCurrentResume(TEST_STUDENT_ID);
        assertEquals("RES_C12_V1", activeBefore.getResumeId());

        com.placement.portal.recruitment.ResumeEvaluationDto evalRequest = new com.placement.portal.recruitment.ResumeEvaluationDto();
        evalRequest.setResumeId(activeBefore.getResumeId());
        evalRequest.setApplicationId("APP_C12");
        evalRequest.setRecruiterId("RECRUITER_TEST");
        evalRequest.setTechnicalScore(5);
        evalRequest.setEducationScore(4);
        evalRequest.setProjectScore(5);
        evalRequest.setExperienceScore(4);
        evalRequest.setOverallScore(5);
        evalRequest.setComments("Evaluated V1");
        evaluationService.recordEvaluation(evalRequest, () -> "recruiter1");

        // Now student activates V2
        mockMvc.perform(put("/api/v1/resumes/RES_C12_V2/activate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resumeId").value("RES_C12_V2"))
                .andExpect(jsonPath("$.data.isCurrent").value("Y"));

        // 1. Subsequent placement workflows querying active resume get V2
        ResumeDto activeAfter = resumeService.getCurrentResume(TEST_STUDENT_ID);
        assertNotNull(activeAfter);
        assertEquals("RES_C12_V2", activeAfter.getResumeId());
        assertEquals(2, activeAfter.getVersionNo());

        // 2. Previously submitted evaluation record for APP_C12 still retains RES_C12_V1
        var existingEvals = evaluationService.getEvaluationsByApplication("APP_C12");
        assertFalse(existingEvals.isEmpty());
        assertEquals("RES_C12_V1", existingEvals.get(0).getResumeId(),
                "Previously recorded evaluation must retain the resume version evaluated at that time");
    }
}

