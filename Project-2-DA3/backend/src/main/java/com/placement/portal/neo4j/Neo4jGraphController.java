package com.placement.portal.neo4j;

import com.placement.portal.auth.PortalUser;
import com.placement.portal.auth.Role;
import com.placement.portal.auth.UserRepository;
import com.placement.portal.common.ApiResponse;
import com.placement.portal.company.RecruiterAuthorizationService;
import com.placement.portal.neo4j.dto.GraphShowcaseDto;
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
@RequestMapping("/api/v1/graph")
@Tag(name = "Neo4j Aura Graph Analytics", description = "Endpoints for graph traversal, projection synchronization, and visualization")
public class Neo4jGraphController {

    private final Neo4jDriverService driverService;
    private final Neo4jProjectionService projectionService;
    private final Neo4jGraphService graphService;
    private final UserRepository userRepository;
    private final RecruiterAuthorizationService recruiterAuthService;

    public Neo4jGraphController(
            Neo4jDriverService driverService,
            Neo4jProjectionService projectionService,
            Neo4jGraphService graphService,
            UserRepository userRepository,
            RecruiterAuthorizationService recruiterAuthService
    ) {
        this.driverService = driverService;
        this.projectionService = projectionService;
        this.graphService = graphService;
        this.userRepository = userRepository;
        this.recruiterAuthService = recruiterAuthService;
    }

    @GetMapping("/status")
    @Operation(summary = "Get Neo4j Aura cloud graph connection status and toggle state")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStatus() {
        return ResponseEntity.ok(ApiResponse.ok(driverService.getStatus()));
    }

    @PostMapping("/reconnect")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Re-attempt connectivity check to Neo4j Aura cloud instance (Admin only)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> reconnect() {
        driverService.reconnect();
        return ResponseEntity.ok(ApiResponse.ok(driverService.getStatus()));
    }

    @PostMapping("/sync")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Project authoritative Oracle transactional data into Neo4j Aura graph (Admin only)")
    public ResponseEntity<ApiResponse<Neo4jProjectionService.ProjectionSummary>> syncFromOracle() {
        Neo4jProjectionService.ProjectionSummary summary = projectionService.syncOracleToNeo4j();
        return ResponseEntity.ok(ApiResponse.ok(summary.message(), summary));
    }

    @GetMapping({"/journey/{studentId}", "/placement-path/{studentId}"})
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN', 'RECRUITER')")
    @Operation(summary = "Traverse student placement journey: Student -> Application -> Drive -> Interview -> Offer")
    public ResponseEntity<ApiResponse<List<GraphShowcaseDto.PlacementPathDto>>> getStudentJourney(
            @PathVariable String studentId,
            Principal principal
    ) {
        validateStudentAccess(principal, studentId);
        return ResponseEntity.ok(ApiResponse.ok(graphService.getPlacementPath(studentId)));
    }

    @GetMapping("/eligible-drives/{studentId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN', 'RECRUITER')")
    @Operation(summary = "Traverse multi-hop eligibility: Student -> Program <- Drive <- Company")
    public ResponseEntity<ApiResponse<List<GraphShowcaseDto.EligibleDriveGraphDto>>> getEligibleDrives(
            @PathVariable String studentId,
            Principal principal
    ) {
        validateStudentAccess(principal, studentId);
        return ResponseEntity.ok(ApiResponse.ok(graphService.getEligibleDrives(studentId)));
    }

    @GetMapping("/recommendations/{studentId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN')")
    @Operation(summary = "Peer recommendations: companies hiring from student's program with similar CGPA profile")
    public ResponseEntity<ApiResponse<List<GraphShowcaseDto.SimilarPlacedRecommendationDto>>> getPeerRecommendations(
            @PathVariable String studentId,
            Principal principal
    ) {
        validateStudentAccess(principal, studentId);
        return ResponseEntity.ok(ApiResponse.ok(graphService.getPeerRecommendations(studentId)));
    }

    @GetMapping("/program-pipeline/{programId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECRUITER', 'STUDENT')")
    @Operation(summary = "Companies recruiting from an academic program via connected graph patterns")
    public ResponseEntity<ApiResponse<List<GraphShowcaseDto.ProgramHiringPipelineDto>>> getProgramPipeline(@PathVariable String programId) {
        return ResponseEntity.ok(ApiResponse.ok(graphService.getProgramPipeline(programId)));
    }

    @GetMapping("/interviewer-workload")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECRUITER')")
    @Operation(summary = "Interviewer workload distribution and outcome ratios across rounds")
    public ResponseEntity<ApiResponse<List<GraphShowcaseDto.InterviewerWorkloadDto>>> getInterviewerWorkload() {
        return ResponseEntity.ok(ApiResponse.ok(graphService.getInterviewerWorkload()));
    }

    @GetMapping("/elements")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get graph nodes and edges for interactive vis-network visualization (Admin only)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getGraphElements(
            @RequestParam(name = "limit", defaultValue = "100") int limit
    ) {
        return ResponseEntity.ok(ApiResponse.ok(graphService.getWholeGraph(limit)));
    }

    private void validateStudentAccess(Principal principal, String studentId) {
        if (principal == null) {
            throw new AccessDeniedException("Access denied: Unauthenticated.");
        }
        PortalUser user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new AccessDeniedException("Access denied: User not found."));

        if (user.getRole() == Role.ROLE_STUDENT) {
            String refId = user.getReferenceId();
            if (refId == null || !refId.equalsIgnoreCase(studentId)) {
                throw new AccessDeniedException("Access denied: Students may only access their own placement graph records.");
            }
        } else if (user.getRole() == Role.ROLE_RECRUITER) {
            recruiterAuthService.requireAuthorizedForStudent(principal, studentId);
        }
    }
}
