package com.placement.portal.interview;

import com.placement.portal.application.Application;

import com.placement.portal.application.ApplicationRepository;
import com.placement.portal.auth.PortalUser;
import com.placement.portal.auth.Role;
import com.placement.portal.auth.UserRepository;
import com.placement.portal.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.placement.portal.company.RecruiterAuthorizationService;
import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/interviews")
@Tag(name = "Interviews", description = "Multi-round candidate interviews, evaluation scores, and results")
public class InterviewController {

    private final InterviewService interviewService;
    private final UserRepository userRepository;
    private final ApplicationRepository applicationRepository;
    private final RecruiterAuthorizationService recruiterAuthService;

    public InterviewController(
            InterviewService interviewService,
            UserRepository userRepository,
            ApplicationRepository applicationRepository,
            RecruiterAuthorizationService recruiterAuthService
    ) {
        this.interviewService = interviewService;
        this.userRepository = userRepository;
        this.applicationRepository = applicationRepository;
        this.recruiterAuthService = recruiterAuthService;
    }

    @PostMapping("/schedule")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    @Operation(summary = "Schedule interview round (Executes Oracle PL/SQL SCHEDULE_INTERVIEW)")
    public ResponseEntity<ApiResponse<Void>> scheduleInterview(
            @Valid @RequestBody ScheduleInterviewDto dto,
            Principal principal
    ) {
        if (principal != null) {
            recruiterAuthService.requireAuthorizedForApplication(principal, dto.getApplicationId());
        }
        interviewService.scheduleInterview(dto);
        return ResponseEntity.ok(ApiResponse.ok("Interview scheduled successfully via PL/SQL procedure", null));
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN')")
    @Operation(summary = "Get interview rounds for the currently authenticated student")
    public ResponseEntity<ApiResponse<List<InterviewDto>>> getMyInterviews(Principal principal) {
        if (principal == null) {
            throw new AccessDeniedException("Authentication required");
        }
        PortalUser user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new AccessDeniedException("User not found"));
        String studentId = user.getReferenceId();
        if (studentId == null || studentId.isBlank()) {
            throw new AccessDeniedException("No student profile linked");
        }
        return ResponseEntity.ok(ApiResponse.ok(interviewService.getInterviewsByStudent(studentId)));
    }

    @GetMapping("/application/{applicationId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'RECRUITER', 'ADMIN')")
    @Operation(summary = "Get interviews for an application")
    public ResponseEntity<ApiResponse<List<InterviewDto>>> getInterviewsByApplication(
            @PathVariable String applicationId,
            Principal principal
    ) {
        if (principal != null) {
            var userOpt = userRepository.findByUsername(principal.getName());
            if (userOpt.isPresent()) {
                PortalUser user = userOpt.get();
                if (user.getRole() == Role.ROLE_STUDENT) {
                    Application app = applicationRepository.findById(applicationId).orElse(null);
                    if (app != null && (user.getReferenceId() == null || !user.getReferenceId().equalsIgnoreCase(app.getStudentId()))) {
                        throw new AccessDeniedException("Access denied: You may only view interviews for your own applications.");
                    }
                } else if (user.getRole() == Role.ROLE_RECRUITER) {
                    recruiterAuthService.requireAuthorizedForApplication(principal, applicationId);
                }
            }
        }
        return ResponseEntity.ok(ApiResponse.ok(interviewService.getInterviewsByApplication(applicationId)));
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'RECRUITER', 'ADMIN')")
    @Operation(summary = "Get interviews for a student's applications")
    public ResponseEntity<ApiResponse<List<InterviewDto>>> getInterviewsByStudent(
            @PathVariable String studentId,
            Principal principal
    ) {
        if (principal != null) {
            var userOpt = userRepository.findByUsername(principal.getName());
            if (userOpt.isPresent()) {
                PortalUser user = userOpt.get();
                if (user.getRole() == Role.ROLE_STUDENT) {
                    if (user.getReferenceId() == null || !user.getReferenceId().equalsIgnoreCase(studentId)) {
                        throw new AccessDeniedException("Access denied: Students may only access their own interview evaluations.");
                    }
                } else if (user.getRole() == Role.ROLE_RECRUITER) {
                    recruiterAuthService.requireAuthorizedForStudent(principal, studentId);
                }
            }
        }
        List<InterviewDto> ivs = interviewService.getInterviewsByStudent(studentId);
        if (principal != null) {
            var userOpt = userRepository.findByUsername(principal.getName());
            if (userOpt.isPresent() && userOpt.get().getRole() == Role.ROLE_RECRUITER) {
                ivs = ivs.stream()
                        .filter(iv -> recruiterAuthService.isAuthorizedForApplication(principal, iv.getApplicationId()))
                        .collect(Collectors.toList());
            }
        }
        return ResponseEntity.ok(ApiResponse.ok(ivs));
    }

    @GetMapping("/interviewers")
    @PreAuthorize("hasAnyRole('STUDENT', 'RECRUITER', 'ADMIN')")
    @Operation(summary = "List all pinned interviewers and their designated round numbers")
    public ResponseEntity<ApiResponse<List<InterviewerRound>>> getAllInterviewers() {
        return ResponseEntity.ok(ApiResponse.ok(interviewService.getAllInterviewerRounds()));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RECRUITER')")
    @Operation(summary = "List all scheduled interviews across candidates")
    public ResponseEntity<ApiResponse<List<InterviewDto>>> getAllInterviews(Principal principal) {
        List<InterviewDto> list = interviewService.getAllInterviews();
        if (principal != null) {
            var userOpt = userRepository.findByUsername(principal.getName());
            if (userOpt.isPresent() && userOpt.get().getRole() == Role.ROLE_RECRUITER) {
                list = list.stream()
                        .filter(iv -> recruiterAuthService.isAuthorizedForApplication(principal, iv.getApplicationId()))
                        .collect(Collectors.toList());
            }
        }
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @PatchMapping("/{applicationId}/{interviewerName}/result")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    @Operation(summary = "Record interview evaluation scores (OA, GD, HR) and mark result")
    public ResponseEntity<ApiResponse<InterviewDto>> recordResult(
            @PathVariable String applicationId,
            @PathVariable String interviewerName,
            @Valid @RequestBody InterviewResultDto resultDto,
            Principal principal
    ) {
        if (principal != null) {
            recruiterAuthService.requireAuthorizedForApplication(principal, applicationId);
        }
        InterviewDto dto = interviewService.recordInterviewResult(applicationId, interviewerName, resultDto);
        return ResponseEntity.ok(ApiResponse.ok("Interview scores recorded", dto));
    }
}
