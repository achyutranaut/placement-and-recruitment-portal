package com.placement.portal.program;

import com.placement.portal.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/programs")
@Tag(name = "Programs", description = "Training curriculum programs, registrations, and tracks")
public class ProgramController {

    private final ProgramService programService;

    public ProgramController(ProgramService programService) {
        this.programService = programService;
    }

    @GetMapping
    @Operation(summary = "List all training programs")
    public ResponseEntity<ApiResponse<List<ProgramDto>>> getAllPrograms() {
        return ResponseEntity.ok(ApiResponse.ok(programService.getAllPrograms()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get training program by ID")
    public ResponseEntity<ApiResponse<ProgramDto>> getProgramById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(programService.getProgramById(id)));
    }

    @PostMapping("/register")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN')")
    @Operation(summary = "Register student for a program (executes Oracle PL/SQL REGISTER_STUDENT_PROGRAM)")
    public ResponseEntity<ApiResponse<Void>> registerStudent(@Valid @RequestBody RegistrationRequestDto request) {
        programService.registerStudent(request.getStudentId(), request.getProgramId(), request.getRegDate());
        return ResponseEntity.ok(ApiResponse.ok("Registration successful via PL/SQL transaction", null));
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN')")
    @Operation(summary = "List programs registered by student")
    public ResponseEntity<ApiResponse<List<ProgramDto>>> getStudentRegisteredPrograms(@PathVariable String studentId) {
        return ResponseEntity.ok(ApiResponse.ok(programService.getStudentRegisteredPrograms(studentId)));
    }
}
