package com.placement.portal.company;

import com.placement.portal.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/jobs")
@Tag(name = "Jobs", description = "Company job designations and openings")
public class JobController {

    private final CompanyService companyService;

    public JobController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @GetMapping
    @Operation(summary = "List all company job designations")
    public ResponseEntity<ApiResponse<List<JobDto>>> getAllJobs() {
        return ResponseEntity.ok(ApiResponse.ok(companyService.getAllJobs()));
    }
}
