package com.placement.portal.student;

import com.placement.portal.auth.UserRepository;
import com.placement.portal.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;
import java.util.List;

import com.placement.portal.auth.IUserContextService;
import com.placement.portal.auth.PortalUserDto;
import com.placement.portal.company.RecruiterAuthorizationService;

@RestController
@RequestMapping("/api/v1/resumes")
@Tag(name = "Resumes", description = "Student resume binary upload, versioning, and download")
public class ResumeController {

    private final IResumeService resumeService;
    private final IUserContextService userContextService;
    private final IStudentService studentService;
    private final RecruiterAuthorizationService recruiterAuthService;

    public ResumeController(
            IResumeService resumeService,
            IUserContextService userContextService,
            IStudentService studentService,
            RecruiterAuthorizationService recruiterAuthService) {
        this.resumeService = resumeService;
        this.userContextService = userContextService;
        this.studentService = studentService;
        this.recruiterAuthService = recruiterAuthService;
    }

    private String sanitizeStudentId(String studentId) {
        if (studentId == null || studentId.isBlank()) {
            return null;
        }
        if (studentId.contains(",")) {
            String[] parts = studentId.split(",");
            String first = parts[0].trim();
            for (String part : parts) {
                if (!part.trim().equalsIgnoreCase(first)) {
                    throw new IllegalArgumentException("Multiple conflicting student IDs provided in request: " + studentId);
                }
            }
            return first;
        }
        return studentId.trim();
    }

    @PostMapping(value = "/upload", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, "*/*"})
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN')")
    @Operation(summary = "Upload student resume (BLOB storage)")
    public ResponseEntity<ApiResponse<ResumeDto>> uploadResume(
            @RequestParam(value = "studentId", required = false) String studentId,
            @RequestParam("file") MultipartFile file,
            Principal principal) throws IOException {

        String sanitizedParamStudentId = sanitizeStudentId(studentId);
        String resolvedStudentId = null;

        if (principal != null) {
            String username = principal.getName();
            var userOpt = userContextService.findByUsername(username);
            if (userOpt.isPresent()) {
                var user = userOpt.get();
                if (user.getRole() == com.placement.portal.auth.Role.ROLE_STUDENT) {
                    String authenticatedStudentId = user.getReferenceId();
                    if (authenticatedStudentId == null || authenticatedStudentId.isBlank()) {
                        authenticatedStudentId = username;
                    }
                    // A student may only upload their own resume - enforce strict ownership!
                    if (sanitizedParamStudentId != null && !sanitizedParamStudentId.equalsIgnoreCase(authenticatedStudentId)) {
                        throw new org.springframework.security.access.AccessDeniedException(
                                "Access denied: You may only upload a resume for your own student profile (" + authenticatedStudentId + ")."
                        );
                    }
                    resolvedStudentId = authenticatedStudentId;
                } else if (user.getRole() == com.placement.portal.auth.Role.ROLE_ADMIN) {
                    resolvedStudentId = sanitizedParamStudentId != null ? sanitizedParamStudentId : user.getReferenceId();
                }
            }
        }

        if (resolvedStudentId == null || resolvedStudentId.isBlank()) {
            resolvedStudentId = sanitizedParamStudentId;
        }

        if (resolvedStudentId == null || resolvedStudentId.isBlank()) {
            throw new IllegalArgumentException("Student ID must be specified or authenticated.");
        }

        // Verify student existence
        try {
            studentService.getStudentById(resolvedStudentId);
        } catch (Exception e) {
            throw new IllegalArgumentException("Authenticated student record not found: " + resolvedStudentId);
        }

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded resume file cannot be empty.");
        }

        if (file.getSize() > 10 * 1024 * 1024) {
            throw new IllegalArgumentException("Resume file size exceeds maximum limit of 10MB.");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || (!filename.toLowerCase().endsWith(".pdf") && !filename.toLowerCase().endsWith(".docx") && !filename.toLowerCase().endsWith(".doc"))) {
            throw new IllegalArgumentException("Invalid file format. Please upload a PDF or Word document (.pdf, .docx).");
        }

        // Binary magic byte validation: inspect binary header
        byte[] header = new byte[8];
        try (java.io.InputStream is = file.getInputStream()) {
            int read = is.read(header);
            if (read < 4) {
                throw new IllegalArgumentException("Corrupt file: File content is too small to determine valid document type.");
            }
        }
        if (filename.toLowerCase().endsWith(".pdf")) {
            // PDF binary signature: %PDF- (0x25, 0x50, 0x44, 0x46, 0x2D)
            boolean isPdfMagic = header[0] == 0x25 && header[1] == 0x50 && header[2] == 0x44 && header[3] == 0x46 && header[4] == 0x2D;
            if (!isPdfMagic) {
                throw new IllegalArgumentException("Invalid file format: Uploaded file does not match expected PDF binary signature (%PDF-).");
            }
        } else if (filename.toLowerCase().endsWith(".docx")) {
            // DOCX is a ZIP container starting with PK (0x50, 0x4B)
            boolean isZipMagic = header[0] == 0x50 && header[1] == 0x4B;
            if (!isZipMagic) {
                throw new IllegalArgumentException("Invalid file format: Uploaded file does not match expected DOCX binary signature.");
            }
        }

        ResumeDto dto = resumeService.uploadResume(resolvedStudentId, file);
        return ResponseEntity.ok(ApiResponse.ok("Resume uploaded successfully (Version " + dto.getVersionNo() + ")", dto));
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'RECRUITER', 'ADMIN')")
    @Operation(summary = "Get all resume versions for a student")
    public ResponseEntity<ApiResponse<List<ResumeDto>>> getResumesByStudent(@PathVariable String studentId, Principal principal) {
        validateStudentAccess(studentId, principal);
        return ResponseEntity.ok(ApiResponse.ok(resumeService.getResumesByStudent(studentId)));
    }

    @GetMapping("/student/{studentId}/current")
    @PreAuthorize("hasAnyRole('STUDENT', 'RECRUITER', 'ADMIN')")
    @Operation(summary = "Get current active resume for student")
    public ResponseEntity<ApiResponse<ResumeDto>> getCurrentResume(@PathVariable String studentId, Principal principal) {
        validateStudentAccess(studentId, principal);
        ResumeDto current = resumeService.getCurrentResume(studentId);
        return ResponseEntity.ok(ApiResponse.ok(current));
    }

    @GetMapping("/{resumeId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'RECRUITER', 'ADMIN')")
    @Operation(summary = "Get resume metadata by ID")
    public ResponseEntity<ApiResponse<ResumeDto>> getResumeById(@PathVariable String resumeId, Principal principal) {
        ResumeDto dto = resumeService.getResumeById(resumeId);
        if (dto != null) {
            validateStudentAccess(dto.getStudentId(), principal);
        }
        return ResponseEntity.ok(ApiResponse.ok(dto));
    }

    @GetMapping("/{resumeId}/download")
    @PreAuthorize("hasAnyRole('STUDENT', 'RECRUITER', 'ADMIN')")
    @Operation(summary = "Stream/Download resume binary BLOB")
    public ResponseEntity<byte[]> downloadResume(@PathVariable String resumeId, Principal principal) {
        ResumeDto resume = resumeService.getResumeById(resumeId);
        validateStudentAccess(resume.getStudentId(), principal);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(resumeService.getResumeContentType(resumeId)))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resumeService.getResumeFileName(resumeId) + "\"")
                .body(resumeService.getResumeBytes(resumeId));
    }

    @GetMapping("/student/{studentId}/current/download")
    @PreAuthorize("hasAnyRole('STUDENT', 'RECRUITER', 'ADMIN')")
    @Operation(summary = "Stream/Download current active resume for student")
    public ResponseEntity<byte[]> downloadCurrentResume(@PathVariable String studentId, Principal principal) {
        validateStudentAccess(studentId, principal);
        ResumeDto current = resumeService.getCurrentResume(studentId);
        if (current == null) {
            return ResponseEntity.notFound().build();
        }
        return downloadResume(current.getResumeId(), principal);
    }

    @DeleteMapping("/{resumeId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN')")
    @Operation(summary = "Delete student resume version permanently (purges Oracle row and BLOB)")
    public ResponseEntity<ApiResponse<ResumeDto>> deleteResume(
            @PathVariable String resumeId,
            Principal principal) {

        if (principal == null) {
            throw new org.springframework.security.access.AccessDeniedException("Authentication required.");
        }

        var userOpt = userContextService.findByUsername(principal.getName());
        if (userOpt.isEmpty()) {
            throw new org.springframework.security.access.AccessDeniedException("Authenticated user not found.");
        }

        var user = userOpt.get();
        boolean isAdmin = user.getRole() == com.placement.portal.auth.Role.ROLE_ADMIN;
        String authenticatedStudentId = null;

        if (user.getRole() == com.placement.portal.auth.Role.ROLE_STUDENT) {
            authenticatedStudentId = user.getReferenceId();
            if (authenticatedStudentId == null || authenticatedStudentId.isBlank()) {
                authenticatedStudentId = user.getUsername();
            }
        }

        ResumeDto deleted = resumeService.deleteResume(authenticatedStudentId, resumeId, isAdmin);
        return ResponseEntity.ok(ApiResponse.ok(
                "Resume version " + deleted.getVersionNo() + " (" + deleted.getFileName() + ") permanently deleted.",
                deleted
        ));
    }

    @PutMapping("/{resumeId}/activate")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN')")
    @Operation(summary = "Set a resume version as active")
    public ResponseEntity<ApiResponse<ResumeDto>> activateResume(
            @PathVariable String resumeId,
            Principal principal) {

        if (principal == null) {
            throw new org.springframework.security.access.AccessDeniedException("Authentication required.");
        }

        var userOpt = userContextService.findByUsername(principal.getName());
        if (userOpt.isEmpty()) {
            throw new org.springframework.security.access.AccessDeniedException("Authenticated user not found.");
        }

        var user = userOpt.get();
        boolean isAdmin = user.getRole() == com.placement.portal.auth.Role.ROLE_ADMIN;
        String authenticatedStudentId = null;

        if (user.getRole() == com.placement.portal.auth.Role.ROLE_STUDENT) {
            authenticatedStudentId = user.getReferenceId();
            if (authenticatedStudentId == null || authenticatedStudentId.isBlank()) {
                authenticatedStudentId = user.getUsername();
            }
        }

        ResumeDto activated = resumeService.activateResume(authenticatedStudentId, resumeId, isAdmin);
        return ResponseEntity.ok(ApiResponse.ok(
                "Resume version " + activated.getVersionNo() + " is now your active resume.",
                activated
        ));
    }

    private void validateStudentAccess(String targetStudentId, Principal principal) {
        if (principal == null) return;
        userContextService.findByUsername(principal.getName()).ifPresent(user -> {
            if (user.getRole() == com.placement.portal.auth.Role.ROLE_STUDENT) {
                String refId = user.getReferenceId();
                if (refId == null || !refId.equalsIgnoreCase(targetStudentId)) {
                    throw new org.springframework.security.access.AccessDeniedException(
                            "Access denied: Students may only access their own resume records."
                    );
                }
            } else if (user.getRole() == com.placement.portal.auth.Role.ROLE_RECRUITER) {
                recruiterAuthService.requireAuthorizedForStudent(principal, targetStudentId);
            }
        });
    }
}
