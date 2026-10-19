package com.placement.portal.student;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("oracle")
@EnabledIf(value = "isOracleAvailable", disabledReason = "Requires a live Oracle database listener at localhost:1521")
public class LiveOracleResumeIntegrationTests {

    static boolean isOracleAvailable() {
        String host = System.getProperty("oracle.host", System.getenv().getOrDefault("ORACLE_HOST", "localhost"));
        int port = 1521;
        try {
            port = Integer.parseInt(System.getProperty("oracle.port", System.getenv().getOrDefault("ORACLE_PORT", "1521")));
        } catch (NumberFormatException ignored) {}
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 500);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StudentResumeRepository resumeRepository;

    private static final String STUDENT_USERNAME = "25bce1799";
    private static final String STUDENT_ID = "STU023";

    @Test
    @DisplayName("Live Oracle DB: Upload test resume, verify BLOB creation, activate/delete lifecycle, verify BLOB purged")
    @WithMockUser(username = STUDENT_USERNAME, roles = {"STUDENT"})
    void testLiveOracleUploadAndPermanentDeletion() throws Exception {
        // Step 1: Upload a new test resume
        byte[] pdfBytes = "%PDF-1.4 Institutional test verification binary payload for Oracle BLOB testing".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test_verification_resume.pdf",
                "application/pdf",
                pdfBytes
        );

        String uploadResponse = mockMvc.perform(multipart("/api/v1/resumes/upload")
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn().getResponse().getContentAsString();

        // Extract uploaded resume ID
        com.fasterxml.jackson.databind.JsonNode json = new com.fasterxml.jackson.databind.ObjectMapper().readTree(uploadResponse);
        String uploadedResumeId = json.path("data").path("resumeId").asText();
        int uploadedVersionNo = json.path("data").path("versionNo").asInt();

        assertNotNull(uploadedResumeId, "Uploaded resume ID must not be null");
        // Version must be a positive integer greater than zero. STU023 always
        // has at least one prior resume version (V1) seeded in Oracle, so a new
        // upload must be >= 2. Use a lower bound that holds across repeated runs
        // on a persistent Oracle database — never hardcode an upper-run count.
        assertTrue(uploadedVersionNo >= 2, "Uploaded version must be >= 2 (STU023 already has V1 in Oracle)");

        // Step 2: Query the live Oracle database repository to confirm row and BLOB exist
        Optional<StudentResume> inDbOpt = resumeRepository.findById(uploadedResumeId);
        assertTrue(inDbOpt.isPresent(), "Row must exist in Oracle STUDENT_RESUME table");
        StudentResume inDb = inDbOpt.get();
        assertEquals(uploadedVersionNo, inDb.getVersionNo());
        assertEquals("Y", inDb.getIsCurrent(), "New upload should automatically be active");
        assertNotNull(inDb.getResumeData(), "Oracle BLOB data must not be null");
        assertEquals(pdfBytes.length, inDb.getResumeData().length, "BLOB length in Oracle must match uploaded bytes");

        // Step 3: Attempt to delete the currently active resume while it's active -> must be rejected
        mockMvc.perform(delete("/api/v1/resumes/" + uploadedResumeId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Cannot delete active resume (Version " + uploadedVersionNo + "). Please activate another resume version first before deleting this version."));

        // Step 4: Re-activate an older resume (e.g. Version 6 or Version 1)
        List<StudentResume> stuResumes = resumeRepository.findByStudentIdOrderByVersionNoDesc(STUDENT_ID);
        StudentResume olderResume = stuResumes.stream()
                .filter(r -> !r.getResumeId().equalsIgnoreCase(uploadedResumeId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Older resume version must exist for STU023"));

        mockMvc.perform(put("/api/v1/resumes/" + olderResume.getResumeId() + "/activate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isCurrent").value("Y"));

        // Confirm in Oracle DB that olderResume is 'Y' and uploadedResumeId is 'N'
        StudentResume olderAfter = resumeRepository.findById(olderResume.getResumeId()).orElseThrow();
        assertEquals("Y", olderAfter.getIsCurrent());
        StudentResume uploadedAfter = resumeRepository.findById(uploadedResumeId).orElseThrow();
        assertEquals("N", uploadedAfter.getIsCurrent());

        // Step 5: Permanently delete the test resume
        mockMvc.perform(delete("/api/v1/resumes/" + uploadedResumeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.resumeId").value(uploadedResumeId));

        // Step 6: Verify directly in Oracle Database that row AND BLOB are completely gone
        Optional<StudentResume> purgedOpt = resumeRepository.findById(uploadedResumeId);
        assertTrue(purgedOpt.isEmpty(), "Row and BLOB must be completely removed from Oracle database");

        // Step 7: Verify remaining versions are intact and NOT renumbered
        List<StudentResume> remaining = resumeRepository.findByStudentIdOrderByVersionNoDesc(STUDENT_ID);
        assertFalse(remaining.stream().anyMatch(r -> r.getResumeId().equalsIgnoreCase(uploadedResumeId)),
                "Deleted resume must not exist in remaining list");
        assertTrue(remaining.stream().anyMatch(r -> r.getResumeId().equalsIgnoreCase(olderResume.getResumeId()) && "Y".equals(r.getIsCurrent())),
                "Active resume must remain intact and functional");
    }

    @Test
    @DisplayName("Live Oracle DB: Full Set-Active workflow across multiple resume versions, BLOB download, and consistency invariants")
    @WithMockUser(username = STUDENT_USERNAME, roles = {"STUDENT"})
    void testLiveOracleSetActiveWorkflowAllCases() throws Exception {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();

        // Remember original active resume for STU023 to restore after test
        List<StudentResume> initialResumes = resumeRepository.findByStudentIdOrderByVersionNoDesc(STUDENT_ID);
        assertFalse(initialResumes.isEmpty(), "STU023 must have existing resumes in Oracle");
        String originalActiveId = initialResumes.stream()
                .filter(r -> "Y".equalsIgnoreCase(r.getIsCurrent()))
                .map(StudentResume::getResumeId)
                .findFirst()
                .orElse(initialResumes.get(0).getResumeId());

        // Upload two temporary versions (tempA and tempB) with distinct BLOB payloads
        byte[] payloadA = "%PDF-1.4 ORACLE_BLOB_PAYLOAD_TEMP_VERSION_A".getBytes(StandardCharsets.UTF_8);
        byte[] payloadB = "%PDF-1.4 ORACLE_BLOB_PAYLOAD_TEMP_VERSION_B".getBytes(StandardCharsets.UTF_8);

        String respA = mockMvc.perform(multipart("/api/v1/resumes/upload")
                        .file(new MockMultipartFile("file", "oracle_temp_a.pdf", "application/pdf", payloadA)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String idA = mapper.readTree(respA).path("data").path("resumeId").asText();

        String respB = mockMvc.perform(multipart("/api/v1/resumes/upload")
                        .file(new MockMultipartFile("file", "oracle_temp_b.pdf", "application/pdf", payloadB)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String idB = mapper.readTree(respB).path("data").path("resumeId").asText();

        try {
            // Currently idB is active ('Y') and idA is inactive ('N')
            assertEquals("Y", resumeRepository.findById(idB).orElseThrow().getIsCurrent());
            assertEquals("N", resumeRepository.findById(idA).orElseThrow().getIsCurrent());
            assertEquals(1L, resumeRepository.countByStudentIdAndIsCurrent(STUDENT_ID, "Y"));

            // Activate idA -> idA becomes 'Y', idB and all others become 'N'
            mockMvc.perform(put("/api/v1/resumes/" + idA + "/activate"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.resumeId").value(idA))
                    .andExpect(jsonPath("$.data.isCurrent").value("Y"))
                    .andExpect(jsonPath("$.data.isActive").value("Y"))
                    .andExpect(jsonPath("$.data.active").value(true));

            assertEquals("Y", resumeRepository.findById(idA).orElseThrow().getIsCurrent());
            assertEquals("N", resumeRepository.findById(idB).orElseThrow().getIsCurrent());
            assertEquals(1L, resumeRepository.countByStudentIdAndIsCurrent(STUDENT_ID, "Y"));

            // Verify fresh read of /student/{studentId}/current returns idA
            mockMvc.perform(get("/api/v1/resumes/student/" + STUDENT_ID + "/current"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.resumeId").value(idA))
                    .andExpect(jsonPath("$.data.isCurrent").value("Y"));

            // Verify downloading current active resume returns payloadA bytes
            byte[] downloadedA = mockMvc.perform(get("/api/v1/resumes/student/" + STUDENT_ID + "/current/download"))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsByteArray();
            assertArrayEquals(payloadA, downloadedA, "Oracle BLOB stream for active resume must match idA");

            // Now activate idB -> idB becomes 'Y', idA becomes 'N'
            mockMvc.perform(put("/api/v1/resumes/" + idB + "/activate"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.resumeId").value(idB))
                    .andExpect(jsonPath("$.data.isCurrent").value("Y"));

            byte[] downloadedB = mockMvc.perform(get("/api/v1/resumes/student/" + STUDENT_ID + "/current/download"))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsByteArray();
            assertArrayEquals(payloadB, downloadedB, "Oracle BLOB stream for active resume must match idB");

            // Non-existent resume activation -> 404 Not Found and idB remains active
            mockMvc.perform(put("/api/v1/resumes/RES_ORACLE_DOES_NOT_EXIST/activate"))
                    .andExpect(status().isNotFound());
            assertEquals("Y", resumeRepository.findById(idB).orElseThrow().getIsCurrent());
            assertEquals(1L, resumeRepository.countByStudentIdAndIsCurrent(STUDENT_ID, "Y"));
        } finally {
            // Restore originalActiveId as active and clean up temporary resumes idA and idB
            mockMvc.perform(put("/api/v1/resumes/" + originalActiveId + "/activate"))
                    .andExpect(status().isOk());
            mockMvc.perform(delete("/api/v1/resumes/" + idA)).andExpect(status().isOk());
            mockMvc.perform(delete("/api/v1/resumes/" + idB)).andExpect(status().isOk());
        }
    }
}

