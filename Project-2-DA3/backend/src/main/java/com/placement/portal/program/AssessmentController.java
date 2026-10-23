package com.placement.portal.program;

import com.placement.portal.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/assessments")
@Tag(name = "Assessments", description = "Batch-level skill assessments and criteria")
public class AssessmentController {

    private final ProgramService programService;

    public AssessmentController(ProgramService programService) {
        this.programService = programService;
    }

    @GetMapping
    @Operation(summary = "List all assessments")
    public ResponseEntity<ApiResponse<List<AssessmentDto>>> getAllAssessments() {
        return ResponseEntity.ok(ApiResponse.ok(programService.getAllAssessments()));
    }

    @GetMapping("/batch/{batchNo}")
    @Operation(summary = "Get assessments for a specific batch")
    public ResponseEntity<ApiResponse<List<AssessmentDto>>> getAssessmentsByBatch(@PathVariable String batchNo) {
        return ResponseEntity.ok(ApiResponse.ok(programService.getAssessmentsByBatch(batchNo)));
    }
}
