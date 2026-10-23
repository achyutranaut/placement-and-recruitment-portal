package com.placement.portal.recruitment;

import com.placement.portal.auth.Role;
import com.placement.portal.auth.PortalUser;
import com.placement.portal.auth.UserRepository;
import com.placement.portal.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/drives")
@Tag(name = "Placement Drives", description = "Campus placement drives and eligibility criteria")
public class PlacementDriveController {

    private final PlacementDriveService driveService;
    private final UserRepository userRepository;
    private final com.placement.portal.company.RecruiterAuthorizationService recruiterAuthService;

    public PlacementDriveController(
            PlacementDriveService driveService,
            UserRepository userRepository,
            com.placement.portal.company.RecruiterAuthorizationService recruiterAuthService
    ) {
        this.driveService = driveService;
        this.userRepository = userRepository;
        this.recruiterAuthService = recruiterAuthService;
    }

    @GetMapping
    @Operation(summary = "List all active placement drives")
    public ResponseEntity<ApiResponse<List<PlacementDriveDto>>> getAllDrives(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        List<PlacementDriveDto> drives = driveService.getAllDrives();
        if (page != null || size != null) {
            int pageSize = (size != null) ? Math.min(Math.max(size, 1), 100) : 20;
            int pageNum = (page != null) ? Math.max(page, 0) : 0;
            int fromIndex = pageNum * pageSize;
            if (fromIndex >= drives.size()) {
                return ResponseEntity.ok(ApiResponse.ok(java.util.Collections.emptyList()));
            }
            int toIndex = Math.min(fromIndex + pageSize, drives.size());
            return ResponseEntity.ok(ApiResponse.ok(drives.subList(fromIndex, toIndex)));
        }
        return ResponseEntity.ok(ApiResponse.ok(drives));
    }

    @GetMapping("/eligibility")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN')")
    @Operation(summary = "Evaluate eligibility for all placement drives for the authenticated student")
    public ResponseEntity<ApiResponse<Map<String, DriveEligibilityDto>>> getAllEligibilityForPrincipal(
            Principal principal
    ) {
        if (principal == null) {
            throw new AccessDeniedException("Authentication required to evaluate eligibility");
        }
        PortalUser user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new AccessDeniedException("Authenticated user not found"));

        String studentId = user.getReferenceId();
        if (studentId == null || studentId.isBlank()) {
            throw new AccessDeniedException("No student profile linked to authenticated account");
        }

        Map<String, DriveEligibilityDto> map = driveService.evaluateAllEligibilityForStudent(studentId);
        return ResponseEntity.ok(ApiResponse.ok(map));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get placement drive details by ID")
    public ResponseEntity<ApiResponse<PlacementDriveDto>> getDriveById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(driveService.getDriveById(id)));
    }

    @GetMapping("/{id}/eligibility")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN')")
    @Operation(summary = "Evaluate eligibility for the authenticated student")
    public ResponseEntity<ApiResponse<DriveEligibilityDto>> getEligibilityForPrincipal(
            @PathVariable String id,
            Principal principal
    ) {
        if (principal == null) {
            throw new AccessDeniedException("Authentication required to evaluate eligibility");
        }
        PortalUser user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new AccessDeniedException("Authenticated user not found"));

        String studentId = user.getReferenceId();
        if (studentId == null || studentId.isBlank()) {
            throw new AccessDeniedException("No student profile linked to authenticated account");
        }

        DriveEligibilityDto dto = driveService.evaluateEligibility(id, studentId);
        return ResponseEntity.ok(ApiResponse.ok(dto));
    }

    @GetMapping("/{id}/eligibility/{studentId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'RECRUITER', 'ADMIN')")
    @Operation(summary = "Evaluate student academic and program eligibility for a placement drive")
    public ResponseEntity<ApiResponse<DriveEligibilityDto>> getEligibility(
            @PathVariable String id,
            @PathVariable String studentId,
            Principal principal
    ) {
        if (principal != null) {
            userRepository.findByUsername(principal.getName()).ifPresent(user -> {
                if (user.getRole() == Role.ROLE_STUDENT) {
                    if (user.getReferenceId() == null || !user.getReferenceId().equalsIgnoreCase(studentId)) {
                        throw new AccessDeniedException("Access denied: Students may only evaluate their own eligibility.");
                    }
                } else if (user.getRole() == Role.ROLE_RECRUITER) {
                    recruiterAuthService.requireAuthorizedForStudent(principal, studentId);
                }
            });
        }
        DriveEligibilityDto dto = driveService.evaluateEligibility(id, studentId);
        return ResponseEntity.ok(ApiResponse.ok(dto));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    @Operation(summary = "Create a new placement drive")
    public ResponseEntity<ApiResponse<PlacementDriveDto>> createDrive(
            @RequestBody PlacementDriveDto dto,
            Principal principal
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Drive created successfully", driveService.createDrive(dto)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    @Operation(summary = "Update an existing placement drive")
    public ResponseEntity<ApiResponse<PlacementDriveDto>> updateDrive(
            @PathVariable String id,
            @RequestBody PlacementDriveDto dto,
            Principal principal
    ) {
        if (principal != null) {
            var userOpt = userRepository.findByUsername(principal.getName());
            if (userOpt.isPresent() && userOpt.get().getRole() == Role.ROLE_RECRUITER) {
                recruiterAuthService.requireAuthorizedForDrive(principal, id);
            }
        }
        return ResponseEntity.ok(ApiResponse.ok("Drive updated successfully", driveService.updateDrive(id, dto)));
    }
}
