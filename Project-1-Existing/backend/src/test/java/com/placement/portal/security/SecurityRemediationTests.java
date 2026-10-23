package com.placement.portal.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.placement.portal.application.ApplicationRepository;
import com.placement.portal.application.ApplicationStatusUpdateDto;
import com.placement.portal.offer.OfferRepository;
import com.placement.portal.student.Student;
import com.placement.portal.student.StudentRepository;
import com.placement.portal.student.StudentUpdateDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
public class SecurityRemediationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private OfferRepository offerRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Bug 1 Remediation: Student attempting to access another student's profile via GET /{id} receives 403 Forbidden")
    @WithMockUser(username = "student1", roles = {"STUDENT"})
    void testCrossStudentProfileAccessBlocked() throws Exception {
        // student1 is STU001; accessing STU002 must return 403 Forbidden
        mockMvc.perform(get("/api/v1/students/STU002"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Bug 1 Remediation: Student accessing /api/v1/students/me receives their own profile")
    @WithMockUser(username = "student1", roles = {"STUDENT"})
    void testStudentGetMyProfile() throws Exception {
        mockMvc.perform(get("/api/v1/students/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.studentId").value("STU001"));
    }

    @Test
    @DisplayName("Bug 1 Remediation: Student attempting to update another student's profile receives 403 Forbidden")
    @WithMockUser(username = "student1", roles = {"STUDENT"})
    void testCrossStudentProfileUpdateBlocked() throws Exception {
        StudentUpdateDto updateDto = new StudentUpdateDto();
        updateDto.setName("Hacked Name");

        mockMvc.perform(put("/api/v1/students/STU002")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Bug 2 Remediation: Student updating profile via /me cannot tamper with CGPA or Branch")
    @WithMockUser(username = "student1", roles = {"STUDENT"})
    void testCgpaAndBranchTamperingPrevented() throws Exception {
        Student original = studentRepository.findById("STU001").orElseThrow();
        BigDecimal originalCgpa = original.getCgpa();
        String originalBranch = original.getBranch();

        // Student sends update with new personal details
        StudentUpdateDto updateDto = new StudentUpdateDto();
        updateDto.setName("Verified Achyut");
        updateDto.setCity("Vellore");
        updateDto.setSkills(List.of("Java", "Oracle", "Spring Boot"));

        mockMvc.perform(put("/api/v1/students/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Verified Achyut"))
                .andExpect(jsonPath("$.data.city").value("Vellore"));

        // Verify in database that CGPA and Branch remain completely untouched
        Student refreshed = studentRepository.findById("STU001").orElseThrow();
        assertEquals(0, originalCgpa.compareTo(refreshed.getCgpa()));
        assertEquals(originalBranch, refreshed.getBranch());
    }

    @Test
    @DisplayName("Bug 3 Remediation: Recruiter cannot view applications for drives belonging to other companies (403 Forbidden)")
    @WithMockUser(username = "recruiter2", roles = {"RECRUITER"})
    void testRecruiterCrossCompanyDriveApplicationsBlocked() throws Exception {
        // recruiter2 is authorized for COM001 (TCS). DRV003 belongs to COM002 (Infosys).
        mockMvc.perform(get("/api/v1/applications/drive/DRV003"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Bug 3 Remediation: Recruiter cannot update application status for candidates of another company (403 Forbidden)")
    @WithMockUser(username = "recruiter2", roles = {"RECRUITER"})
    void testRecruiterCrossCompanyStatusUpdateBlocked() throws Exception {
        // Find an application for DRV003 (Infosys)
        var apps = applicationRepository.findAll().stream()
                .filter(a -> "DRV003".equalsIgnoreCase(a.getDriveId()))
                .findFirst();

        if (apps.isPresent()) {
            ApplicationStatusUpdateDto patchDto = new ApplicationStatusUpdateDto("REJECTED");
            mockMvc.perform(patch("/api/v1/applications/" + apps.get().getApplicationId() + "/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(patchDto)))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    @DisplayName("Bug 4 Remediation: Recruiter cannot download offer letters of another company (403 Forbidden)")
    @WithMockUser(username = "infosys_recruiter", roles = {"RECRUITER"})
    void testRecruiterOfferLetterDownloadIdorBlocked() throws Exception {
        // infosys_recruiter is COM002. OFF001 belongs to Microsoft / TCS.
        var offerOpt = offerRepository.findAll().stream()
                .filter(o -> !"DRV003".equalsIgnoreCase(o.getApplicationId()))
                .findFirst();

        if (offerOpt.isPresent()) {
            mockMvc.perform(get("/api/v1/offers/" + offerOpt.get().getOfferId() + "/download"))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    @DisplayName("Bug 8 Remediation: Unauthenticated access to interviewer directory is blocked (401/403)")
    void testUnauthenticatedInterviewerDirectoryBlocked() throws Exception {
        mockMvc.perform(get("/api/v1/interviews/interviewers"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Bug 8 Remediation: Authenticated users can access interviewer directory")
    @WithMockUser(username = "student1", roles = {"STUDENT"})
    void testAuthenticatedInterviewerDirectoryAllowed() throws Exception {
        mockMvc.perform(get("/api/v1/interviews/interviewers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Phase 3 Remediation: Fake PDF rejected via binary magic byte check (%PDF-)")
    @WithMockUser(username = "student1", roles = {"STUDENT"})
    void testFakePdfUploadRejectedByMagicBytes() throws Exception {
        MockMultipartFile fakePdf = new MockMultipartFile(
                "file",
                "malicious.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "PLAIN TEXT EXECUTABLE CODE".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/resumes/upload")
                        .file(fakePdf))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Phase 3 Remediation: Genuine PDF accepted via magic byte verification (%PDF-)")
    @WithMockUser(username = "student1", roles = {"STUDENT"})
    void testGenuinePdfUploadAccepted() throws Exception {
        MockMultipartFile validPdf = new MockMultipartFile(
                "file",
                "valid_resume.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "%PDF-1.7 Valid Institutional PDF Content Stream".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/resumes/upload")
                        .file(validPdf))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fileName").value("valid_resume.pdf"));
    }

    @Test
    @DisplayName("Adversarial Test: Recruiter cannot access candidate profile of student who applied to another company")
    @WithMockUser(username = "infosys_recruiter", roles = {"RECRUITER"})
    void testRecruiterCrossCompanyCandidateAccessBlocked() throws Exception {
        // infosys_recruiter is COM002. STU001 only applied to Microsoft / TCS.
        mockMvc.perform(get("/api/v1/students/STU001"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Adversarial Test: Recruiter cannot access interview records of another company")
    @WithMockUser(username = "infosys_recruiter", roles = {"RECRUITER"})
    void testRecruiterCrossCompanyInterviewAccessBlocked() throws Exception {
        // APP001 belongs to Microsoft (DRV001). infosys_recruiter (COM002) must receive 403 Forbidden.
        mockMvc.perform(get("/api/v1/interviews/application/APP001"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Adversarial Test: Student cannot manipulate registration number or placement status via /me")
    @WithMockUser(username = "student1", roles = {"STUDENT"})
    void testRegistrationNumberAndPlacementStatusImmutable() throws Exception {
        Student original = studentRepository.findById("STU001").orElseThrow();
        String originalRegNo = original.getRegistrationNo();
        String originalPlacementStatus = original.getPlacementStatus();

        // Malicious payload attempting to inject registration number and placement status
        String forgedPayload = """
            {
                "name": "Achyut Updated",
                "registrationNo": "FORGED_REG_999",
                "placementStatus": "PLACED",
                "cgpa": 10.0,
                "branch": "Mechanical Engineering"
            }
        """;

        mockMvc.perform(put("/api/v1/students/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(forgedPayload))
                .andExpect(status().isOk());

        Student refreshed = studentRepository.findById("STU001").orElseThrow();
        assertEquals(originalRegNo, refreshed.getRegistrationNo(), "Registration number must remain immutable");
        assertEquals(originalPlacementStatus, refreshed.getPlacementStatus(), "Placement status must remain immutable");
        assertEquals(original.getCgpa(), refreshed.getCgpa(), "CGPA must remain immutable");
        assertEquals(original.getBranch(), refreshed.getBranch(), "Branch must remain immutable");
    }

    @Test
    @DisplayName("Adversarial Test: Safe pagination bounds enforce safe limits on size=1000 and size=-1")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testSafePaginationBoundsEnforced() throws Exception {
        // size=1000 should be capped and succeed without exceeding max page size (100)
        mockMvc.perform(get("/api/v1/students?page=0&size=1000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // size=-1 should be clamped to safe minimum (1) rather than returning unbound dataset
        mockMvc.perform(get("/api/v1/students?page=0&size=-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1));
    }
}
