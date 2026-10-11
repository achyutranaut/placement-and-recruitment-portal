package com.placement.portal.neo4j.controller;

import com.placement.portal.common.ApiResponse;
import com.placement.portal.neo4j.dto.GraphShowcaseDto;
import com.placement.portal.neo4j.service.Neo4jGraphAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/graph")
@Profile("neo4j")
@Tag(name = "Graph Analytics", description = "Neo4j connected-data traversals and graph showcase endpoints")
public class Neo4jGraphController {

    private final Neo4jGraphAnalyticsService analyticsService;

    public Neo4jGraphController(Neo4jGraphAnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/eligible-drives/{studentId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN', 'RECRUITER')")
    @Operation(summary = "Eligible drives for student via multi-hop graph traversal (Student->Program<-Drive)")
    public ResponseEntity<ApiResponse<List<GraphShowcaseDto.EligibleDriveGraphDto>>> getEligibleDrives(
            @PathVariable String studentId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(analyticsService.getEligibleDrivesForStudent(studentId)));
    }

    @GetMapping("/placement-path/{studentId}")
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
}
