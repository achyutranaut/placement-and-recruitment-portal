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
}
