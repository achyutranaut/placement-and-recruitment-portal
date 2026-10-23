package com.placement.portal.program;

import com.placement.portal.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/batches")
@Tag(name = "Batches", description = "Classroom schedules and physical batches")
public class BatchController {

    private final ProgramService programService;

    public BatchController(ProgramService programService) {
        this.programService = programService;
    }

    @GetMapping
    @Operation(summary = "List all training batches")
    public ResponseEntity<ApiResponse<List<BatchDto>>> getAllBatches() {
        return ResponseEntity.ok(ApiResponse.ok(programService.getAllBatches()));
    }

    @GetMapping("/program/{programId}")
    @Operation(summary = "Get batches for a specific program")
    public ResponseEntity<ApiResponse<List<BatchDto>>> getBatchesByProgram(@PathVariable String programId) {
        return ResponseEntity.ok(ApiResponse.ok(programService.getBatchesByProgram(programId)));
    }
}
