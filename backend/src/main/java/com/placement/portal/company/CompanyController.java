package com.placement.portal.company;

import com.placement.portal.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/companies")
@Tag(name = "Companies", description = "Partner companies and recruitment accounts")
public class CompanyController {

    private final CompanyService companyService;

    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @GetMapping
    @Operation(summary = "List all registered companies")
    public ResponseEntity<ApiResponse<List<CompanyDto>>> getAllCompanies() {
        return ResponseEntity.ok(ApiResponse.ok(companyService.getAllCompanies()));
    }

    @GetMapping("/my")
    @Operation(summary = "Get companies authorized for authenticated user")
    public ResponseEntity<ApiResponse<List<CompanyDto>>> getMyAuthorizedCompanies(java.security.Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Authentication required"));
        }
        return ResponseEntity.ok(ApiResponse.ok(companyService.getAuthorizedCompanies(principal.getName())));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get company details by ID")
    public ResponseEntity<ApiResponse<CompanyDto>> getCompanyById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(companyService.getCompanyById(id)));
    }

    @GetMapping("/{id}/jobs")
    @Operation(summary = "Get jobs posted by company")
    public ResponseEntity<ApiResponse<List<JobDto>>> getJobsByCompany(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(companyService.getJobsByCompany(id)));
    }
}
