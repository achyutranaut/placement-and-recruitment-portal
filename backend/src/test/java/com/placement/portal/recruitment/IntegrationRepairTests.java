package com.placement.portal.recruitment;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class IntegrationRepairTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(username = "student1", roles = {"STUDENT"})
    void testGetDriveByIdDRV001() throws Exception {
        mockMvc.perform(get("/api/v1/drives/DRV001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.driveId").value("DRV001"))
                .andExpect(jsonPath("$.data.ctc").isNumber())
                .andExpect(jsonPath("$.data.packageLpa").isNumber())
                .andExpect(jsonPath("$.data.jobTitle").value("Software Development Engineer I"));
    }

    @Test
    @WithMockUser(username = "student1", roles = {"STUDENT"})
    void testUploadResumeWithParam() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test_resume.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "%PDF-1.4 Mock PDF Content".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/resumes/upload")
                        .file(file)
                        .param("studentId", "STU001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fileName").value("test_resume.pdf"))
                .andExpect(jsonPath("$.data.versionNo").isNumber());
    }

    @Test
    @WithMockUser(username = "student1", roles = {"STUDENT"})
    void testUploadResumeWithPrincipalResolution() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "principal_resume.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "PK mock word doc".getBytes()
        );

        // Upload without studentId param -> resolved from student1 (referenceId STU001)
        mockMvc.perform(multipart("/api/v1/resumes/upload")
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.studentId").value("STU001"));
    }

    @Test
    @WithMockUser(username = "student1", roles = {"STUDENT"})
    void testUploadResumeInvalidFileTypeRejected() throws Exception {
        MockMultipartFile invalidFile = new MockMultipartFile(
                "file",
                "malicious.exe",
                "application/x-msdownload",
                "MZ binary".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/resumes/upload")
                        .file(invalidFile)
                        .param("studentId", "STU001"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(username = "student1", roles = {"STUDENT"})
    void testUploadResumeWithDuplicateParam_SanitizedSuccessfully() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "duplicate_param_resume.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "%PDF-1.4 Mock PDF Content".getBytes()
        );

        // Simulate duplicate studentId parameter (e.g. sent in both query and body or duplicate param: "STU001,STU001")
        mockMvc.perform(multipart("/api/v1/resumes/upload")
                        .file(file)
                        .param("studentId", "STU001,STU001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.studentId").value("STU001"))
                .andExpect(jsonPath("$.data.fileName").value("duplicate_param_resume.pdf"));
    }

    @Test
    @WithMockUser(username = "student1", roles = {"STUDENT"})
    void testUploadResumeVersioningAndActiveToggle() throws Exception {
        MockMultipartFile file1 = new MockMultipartFile(
                "file",
                "resume_v1.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "%PDF-1.4 Initial Version".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/resumes/upload").file(file1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fileName").value("resume_v1.pdf"))
                .andExpect(jsonPath("$.data.isCurrent").value("Y"));

        MockMultipartFile file2 = new MockMultipartFile(
                "file",
                "resume_v2.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "%PDF-1.4 Second Version".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/resumes/upload").file(file2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fileName").value("resume_v2.pdf"))
                .andExpect(jsonPath("$.data.isCurrent").value("Y"));

        // Verify active resume is now v2
        mockMvc.perform(get("/api/v1/resumes/student/STU001/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fileName").value("resume_v2.pdf"))
                .andExpect(jsonPath("$.data.isCurrent").value("Y"));

        // Verify binary download of active resume succeeds
        mockMvc.perform(get("/api/v1/resumes/student/STU001/current/download"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("resume_v2.pdf")));
    }

    @Test
    @WithMockUser(username = "student1", roles = {"STUDENT"})
    void testUploadResumeCrossStudentTampering_Forbidden() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "tampered_resume.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "%PDF-1.4 Mock PDF Content".getBytes()
        );

        // Authenticated user student1 (STU001) attempting to upload for STU002
        mockMvc.perform(multipart("/api/v1/resumes/upload")
                        .file(file)
                        .param("studentId", "STU002"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "student1", roles = {"STUDENT"})
    void testResumeDownloadCrossStudentAccess_Forbidden() throws Exception {
        // Authenticated user student1 (STU001) attempting to access STU002's resume records
        mockMvc.perform(get("/api/v1/resumes/student/STU002"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/resumes/student/STU002/current"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/resumes/student/STU002/current/download"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "adminUser", roles = {"ADMIN"})
    void testUploadResumeNonExistentStudent_ThrowsNotFound() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "admin_test.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "%PDF-1.4 Mock PDF Content".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/resumes/upload")
                        .file(file)
                        .param("studentId", "STU999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Authenticated student record not found")));
    }

    @Test
    void testStudentRegistrationSuccess() throws Exception {
        String json = """
                {
                    "name": "Kavya Ramesh",
                    "email": "kavya.ramesh2021@vitstudent.ac.in",
                    "registrationNo": "21BCE9999",
                    "branch": "Computer Science & Engineering",
                    "password": "strongPassword123",
                    "cgpa": 9.10,
                    "dob": "2003-05-12",
                    "phone": "+91-9876543210"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/student/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").isString())
                .andExpect(jsonPath("$.data.role").value("ROLE_STUDENT"))
                .andExpect(jsonPath("$.data.username").value("21bce9999"));
    }

    @Test
    void testStudentRegistrationInvalidDomainRejected() throws Exception {
        String json = """
                {
                    "name": "Invalid Email",
                    "email": "user@gmail.com",
                    "registrationNo": "21BCE8888",
                    "branch": "Computer Science & Engineering",
                    "password": "password123",
                    "cgpa": 8.50,
                    "dob": "2003-01-01",
                    "phone": "+91-9876543210"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/student/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void testStudentRegistrationDepartmentMismatchRejected() throws Exception {
        String json = """
                {
                    "name": "Mismatch Dept",
                    "email": "mismatch@vitstudent.ac.in",
                    "registrationNo": "21BME7777",
                    "branch": "Computer Science & Engineering",
                    "password": "password123",
                    "cgpa": 8.50,
                    "dob": "2003-01-01",
                    "phone": "+91-9876543210"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/student/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(username = "student1", roles = {"STUDENT"})
    void testOfferLetterPdfDownload() throws Exception {
        mockMvc.perform(get("/api/v1/offers/OFF001/download"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().exists("Content-Disposition"));
    }

    @Test
    @WithMockUser(username = "student1", roles = {"STUDENT"})
    void testStudentInterviewsScopedEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/interviews/student/STU001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @WithMockUser(username = "student1", roles = {"STUDENT"})
    void testResumeDownloadOwnResumeSuccess() throws Exception {
        mockMvc.perform(get("/api/v1/resumes/RES_STU001_V2/download"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"));
    }

    @Test
    @WithMockUser(username = "student1", roles = {"STUDENT"})
    void testResumeDownloadOtherStudentForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/resumes/RES_STU002_V1/download"))
                .andExpect(status().isForbidden());
    }
}
