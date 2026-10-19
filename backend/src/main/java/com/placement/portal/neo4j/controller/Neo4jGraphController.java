package com.placement.portal.neo4j.controller;

import com.placement.portal.common.ApiResponse;
import com.placement.portal.neo4j.dto.GraphMutationDto.*;
import com.placement.portal.neo4j.dto.GraphShowcaseDto;
import com.placement.portal.neo4j.service.Neo4jGraphAnalyticsService;
import com.placement.portal.neo4j.service.Neo4jGraphMutationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/graph")
@Profile("neo4j")
@Tag(name = "Graph Analytics & Management", description = "Neo4j connected-data traversals and authorized administrator graph mutations")
public class Neo4jGraphController {

    private final Neo4jGraphAnalyticsService analyticsService;
    private final Neo4jGraphMutationService mutationService;

    public Neo4jGraphController(
            Neo4jGraphAnalyticsService analyticsService,
            Neo4jGraphMutationService mutationService
    ) {
        this.analyticsService = analyticsService;
        this.mutationService = mutationService;
    }

    // ==========================================
    // READ / TRAVERSAL ENDPOINTS
    // ==========================================

    @GetMapping("/eligible-drives/{studentId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN', 'RECRUITER')")
    @Operation(summary = "Eligible drives for student via multi-hop graph traversal (Student->Program<-Drive)")
    public ResponseEntity<ApiResponse<List<GraphShowcaseDto.EligibleDriveGraphDto>>> getEligibleDrives(
            @PathVariable String studentId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(analyticsService.getEligibleDrivesForStudent(studentId)));
    }

    @GetMapping({"/placement-path/{studentId}", "/journey/{studentId}"})
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN', 'RECRUITER')")
    @Operation(summary = "Full placement path of student: Student -> Application -> Interview -> Offer -> Company")
    public ResponseEntity<ApiResponse<List<GraphShowcaseDto.PlacementPathDto>>> getPlacementPath(
            @PathVariable String studentId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(analyticsService.getFullPlacementPath(studentId)));
    }

    @GetMapping("/program-pipeline/{programId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECRUITER', 'STUDENT')")
    @Operation(summary = "Companies recruiting from an academic program via connected graph patterns")
    public ResponseEntity<ApiResponse<List<GraphShowcaseDto.ProgramHiringPipelineDto>>> getProgramPipeline(
            @PathVariable String programId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(analyticsService.getCompaniesRecruitingFromProgram(programId)));
    }

    @GetMapping("/recommendations/{studentId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN')")
    @Operation(summary = "Similar students placed recommendation (peer traversal within program and CGPA delta)")
    public ResponseEntity<ApiResponse<List<GraphShowcaseDto.SimilarPlacedRecommendationDto>>> getRecommendations(
            @PathVariable String studentId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(analyticsService.getSimilarStudentsPlaced(studentId)));
    }

    @GetMapping("/interviewer-workload")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECRUITER')")
    @Operation(summary = "Interviewer workload distribution and outcome ratios across rounds")
    public ResponseEntity<ApiResponse<List<GraphShowcaseDto.InterviewerWorkloadDto>>> getInterviewerWorkload() {
        return ResponseEntity.ok(ApiResponse.ok(analyticsService.getInterviewerWorkloads()));
    }

    @GetMapping("/entities/{label}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STUDENT', 'RECRUITER')")
    @Operation(summary = "List graph entities of a specific label with limit")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getEntities(
            @PathVariable String label,
            @RequestParam(name = "limit", defaultValue = "50") int limit
    ) {
        return ResponseEntity.ok(ApiResponse.ok(mutationService.getEntitiesByLabel(label, limit)));
    }

    // ==========================================
    // MUTATION ENDPOINTS (ADMINISTRATOR ONLY)
    // ==========================================

    @PostMapping("/entities")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a graph entity node (Admin only)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createEntity(
            @RequestBody CreateEntityRequestDto request,
            Principal principal
    ) {
        String adminUser = principal != null ? principal.getName() : "ADMIN";
        Map<String, Object> created = mutationService.createEntity(request, adminUser);
        return ResponseEntity.ok(ApiResponse.ok("Graph node successfully created.", created));
    }

    @PutMapping("/entities/{label}/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update permitted properties of a graph entity node (Admin only)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateEntity(
            @PathVariable String label,
            @PathVariable String id,
            @RequestBody UpdateEntityRequestDto request,
            Principal principal
    ) {
        String adminUser = principal != null ? principal.getName() : "ADMIN";
        Map<String, Object> updated = mutationService.updateEntity(label, id, request, adminUser);
        return ResponseEntity.ok(ApiResponse.ok("Graph node successfully updated.", updated));
    }

    @DeleteMapping("/entities/{label}/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Safely delete a graph entity node with dependency verification (Admin only)")
    public ResponseEntity<ApiResponse<Void>> deleteEntity(
            @PathVariable String label,
            @PathVariable String id,
            Principal principal
    ) {
        String adminUser = principal != null ? principal.getName() : "ADMIN";
        mutationService.deleteEntity(label, id, adminUser);
        return ResponseEntity.ok(ApiResponse.ok("Graph node " + id + " successfully deleted from database.", null));
    }

    @PostMapping("/relationships")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create an allowlisted relationship between two graph nodes (Admin only)")
    public ResponseEntity<ApiResponse<Void>> createRelationship(
            @RequestBody CreateRelationshipRequestDto request,
            Principal principal
    ) {
        String adminUser = principal != null ? principal.getName() : "ADMIN";
        mutationService.createRelationship(request, adminUser);
        return ResponseEntity.ok(ApiResponse.ok("Relationship [" + request.getType() + "] established successfully.", null));
    }

    @DeleteMapping("/relationships")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a relationship between two graph nodes (Admin only)")
    public ResponseEntity<ApiResponse<Void>> deleteRelationship(
            @RequestBody DeleteRelationshipRequestDto request,
            Principal principal
    ) {
        String adminUser = principal != null ? principal.getName() : "ADMIN";
        mutationService.deleteRelationship(request, adminUser);
        return ResponseEntity.ok(ApiResponse.ok("Relationship [" + request.getType() + "] removed successfully.", null));
    }
}
