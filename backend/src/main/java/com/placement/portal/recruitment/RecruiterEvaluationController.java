package com.placement.portal.recruitment;

import com.placement.portal.common.ApiResponse;
import com.placement.portal.company.CompanyDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/recruiter")
@Tag(name = "Recruiter Operations", description = "Candidate resume evaluation and job role management")
public class RecruiterEvaluationController {

    private final IRecruiterEvaluationService evaluationService;

    public RecruiterEvaluationController(IRecruiterEvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    @GetMapping("/companies")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    @Operation(summary = "Get companies authorized for authenticated recruiter")
    public ResponseEntity<ApiResponse<List<CompanyDto>>> getAuthorizedCompanies(Principal principal) {
        return ResponseEntity.ok(ApiResponse.ok(evaluationService.getAuthorizedCompanies(principal)));
    }

    @PostMapping("/company/select")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    @Operation(summary = "Establish active company context for recruiter session")
    public ResponseEntity<ApiResponse<Map<String, Object>>> selectActiveCompany(
            @RequestBody Map<String, String> payload,
            Principal principal
    ) {
        String companyId = payload.get("companyId");
        Map<String, Object> result = evaluationService.selectActiveCompany(companyId, principal);
        return ResponseEntity.ok(ApiResponse.ok("Company context established", result));
    }

    @PostMapping("/evaluations")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    @Operation(summary = "Record structured resume evaluation for candidate")
    public ResponseEntity<ApiResponse<ResumeEvaluationDto>> recordEvaluation(
            @RequestBody ResumeEvaluationDto dto,
            Principal principal
    ) {
        ResumeEvaluationDto saved = evaluationService.recordEvaluation(dto, principal);
        return ResponseEntity.ok(ApiResponse.ok("Candidate evaluation saved successfully", saved));
    }

    @GetMapping("/evaluations/application/{applicationId}")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    @Operation(summary = "Get evaluations for application")
    public ResponseEntity<ApiResponse<List<ResumeEvaluationDto>>> getEvaluationsByApplication(@PathVariable String applicationId) {
        return ResponseEntity.ok(ApiResponse.ok(evaluationService.getEvaluationsByApplication(applicationId)));
    }

    @GetMapping("/roles")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    @Operation(summary = "Get recruiter placement roles with stage applicant counts")
    public ResponseEntity<ApiResponse<List<RecruiterRoleStatDto>>> getRecruiterRoleStats(
            @RequestParam(value = "companyId", required = false) String companyId,
            Principal principal
    ) {
        return ResponseEntity.ok(ApiResponse.ok(evaluationService.getRecruiterRoleStats(companyId, principal)));
    }
}
