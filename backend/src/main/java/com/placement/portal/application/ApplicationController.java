package com.placement.portal.application;

import com.placement.portal.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.placement.portal.auth.Role;
import com.placement.portal.auth.PortalUser;
import com.placement.portal.auth.UserRepository;
import org.springframework.security.access.AccessDeniedException;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/applications")
@Tag(name = "Applications", description = "Student placement drive applications and status tracking")
public class ApplicationController {

    private final ApplicationService applicationService;
    private final UserRepository userRepository;

    public ApplicationController(ApplicationService applicationService, UserRepository userRepository) {
        this.applicationService = applicationService;
        this.userRepository = userRepository;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN')")
    @Operation(summary = "Apply for a placement drive (Executes Oracle PL/SQL APPLY_FOR_DRIVE)")
    public ResponseEntity<ApiResponse<ApplicationDto>> applyForDrive(
            @Valid @RequestBody ApplyDriveRequestDto request,
            Principal principal
    ) {
        if (principal != null) {
            userRepository.findByUsername(principal.getName()).ifPresent(user -> {
                if (user.getRole() == Role.ROLE_STUDENT) {
                    if (user.getReferenceId() != null && !user.getReferenceId().equalsIgnoreCase(request.getStudentId())) {
                        throw new AccessDeniedException("Access denied: You cannot submit applications for another student.");
                    }
                }
            });
        }

        // Enforce student hasn't already applied for this placement drive
        boolean alreadyApplied = applicationService.getApplicationsByStudent(request.getStudentId()).stream()
                .anyMatch(a -> request.getDriveId().equalsIgnoreCase(a.getDriveId()));
        if (alreadyApplied) {
            throw new IllegalArgumentException("You have already submitted an application for placement drive " + request.getDriveId() + ".");
        }

        ApplicationDto dto = applicationService.applyForDrive(request.getStudentId(), request.getDriveId(), request.getApplyDate());
        return ResponseEntity.ok(ApiResponse.ok("Applied successfully via PL/SQL procedure", dto));
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN')")
    @Operation(summary = "Get applications submitted by the authenticated student")
    public ResponseEntity<ApiResponse<List<ApplicationDto>>> getMyApplications(Principal principal) {
        if (principal == null) {
            throw new AccessDeniedException("Authentication required to access application pipeline");
        }
        PortalUser user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new AccessDeniedException("Authenticated user not found"));

        String studentId = user.getReferenceId();
        if (studentId == null || studentId.isBlank()) {
            throw new AccessDeniedException("No student profile linked to authenticated account");
        }

        return ResponseEntity.ok(ApiResponse.ok(applicationService.getApplicationsByStudent(studentId)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RECRUITER')")
    @Operation(summary = "List all applications")
    public ResponseEntity<ApiResponse<List<ApplicationDto>>> getAllApplications() {
        return ResponseEntity.ok(ApiResponse.ok(applicationService.getAllApplications()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDENT', 'RECRUITER', 'ADMIN')")
    @Operation(summary = "Get application details by ID with scores and evaluation metrics")
    public ResponseEntity<ApiResponse<ApplicationDto>> getApplicationById(
            @PathVariable String id,
            Principal principal
    ) {
        ApplicationDto dto = applicationService.getApplicationById(id);
        if (principal != null) {
            userRepository.findByUsername(principal.getName()).ifPresent(user -> {
                if (user.getRole() == Role.ROLE_STUDENT) {
                    if (user.getReferenceId() != null && !user.getReferenceId().equalsIgnoreCase(dto.getStudentId())) {
                        throw new AccessDeniedException("Access denied: You may only view your own application records.");
                    }
                }
            });
        }
        return ResponseEntity.ok(ApiResponse.ok(dto));
    }

    @GetMapping("/{id}/scores")
    @PreAuthorize("hasAnyRole('STUDENT', 'RECRUITER', 'ADMIN')")
    @Operation(summary = "Get candidate interview scores and overall evaluation for an application")
    public ResponseEntity<ApiResponse<ApplicationScoresDto>> getApplicationScores(
            @PathVariable String id,
            Principal principal
    ) {
        ApplicationDto dto = applicationService.getApplicationById(id);
        if (principal != null) {
            userRepository.findByUsername(principal.getName()).ifPresent(user -> {
                if (user.getRole() == Role.ROLE_STUDENT) {
                    if (user.getReferenceId() != null && !user.getReferenceId().equalsIgnoreCase(dto.getStudentId())) {
                        throw new AccessDeniedException("Access denied: You may only view your own evaluation scores.");
                    }
                }
            });
        }
        return ResponseEntity.ok(ApiResponse.ok(dto.getScores()));
    }


    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN')")
    @Operation(summary = "Get applications submitted by student")
    public ResponseEntity<ApiResponse<List<ApplicationDto>>> getApplicationsByStudent(
            @PathVariable String studentId,
            Principal principal
    ) {
        if (principal != null) {
            userRepository.findByUsername(principal.getName()).ifPresent(user -> {
                if (user.getRole() == Role.ROLE_STUDENT) {
                    if (user.getReferenceId() != null && !user.getReferenceId().equalsIgnoreCase(studentId)) {
                        throw new AccessDeniedException("Access denied: Students may only access their own applications.");
                    }
                }
            });
        }
        return ResponseEntity.ok(ApiResponse.ok(applicationService.getApplicationsByStudent(studentId)));
    }

    @GetMapping("/drive/{driveId}")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    @Operation(summary = "Get applications for a specific drive")
    public ResponseEntity<ApiResponse<List<ApplicationDto>>> getApplicationsByDrive(@PathVariable String driveId) {
        return ResponseEntity.ok(ApiResponse.ok(applicationService.getApplicationsByDrive(driveId)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    @Operation(summary = "Update application status (SHORTLISTED, INTERVIEWING, SELECTED, REJECTED)")
    public ResponseEntity<ApiResponse<ApplicationDto>> updateStatus(
            @PathVariable String id,
            @Valid @RequestBody ApplicationStatusUpdateDto request
    ) {
        ApplicationDto dto = applicationService.updateStatus(id, request.getStatus(), "RECRUITER");
        return ResponseEntity.ok(ApiResponse.ok("Status updated", dto));
    }
}
