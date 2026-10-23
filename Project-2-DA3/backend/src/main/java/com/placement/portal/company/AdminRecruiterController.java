package com.placement.portal.company;

import com.placement.portal.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/recruiters")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Recruiter Management", description = "Endpoints for administrator to assign and revoke recruiter company authorizations")
public class AdminRecruiterController {

    private final CompanyService companyService;

    public AdminRecruiterController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @GetMapping
    @Operation(summary = "List all recruiters with their authorized companies")
    public ResponseEntity<ApiResponse<List<RecruiterAuthorizationDto>>> getAllRecruiters() {
        return ResponseEntity.ok(ApiResponse.ok(companyService.getAllRecruitersWithAuthorizations()));
    }

    @PostMapping("/{userId}/companies/{companyId}")
    @Operation(summary = "Assign company authorization to recruiter")
    public ResponseEntity<ApiResponse<String>> assignCompany(
            @PathVariable String userId,
            @PathVariable String companyId
    ) {
        companyService.assignCompanyToRecruiter(userId, companyId);
        return ResponseEntity.ok(ApiResponse.ok("Company " + companyId + " authorized for recruiter successfully."));
    }

    @DeleteMapping("/{userId}/companies/{companyId}")
    @Operation(summary = "Revoke company authorization from recruiter")
    public ResponseEntity<ApiResponse<String>> revokeCompany(
            @PathVariable String userId,
            @PathVariable String companyId
    ) {
        companyService.revokeCompanyFromRecruiter(userId, companyId);
        return ResponseEntity.ok(ApiResponse.ok("Company " + companyId + " authorization revoked."));
    }
}
