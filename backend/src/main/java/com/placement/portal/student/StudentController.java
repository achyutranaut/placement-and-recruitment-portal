package com.placement.portal.student;

import com.placement.portal.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/students")
@Tag(name = "Students", description = "Student profile management and academic records")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RECRUITER')")
    @Operation(summary = "List all students", description = "Retrieve list of all registered students with contact info and CGPA")
    public ResponseEntity<ApiResponse<List<StudentDto>>> getAllStudents() {
        return ResponseEntity.ok(ApiResponse.ok(studentService.getAllStudents()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN', 'RECRUITER')")
    @Operation(summary = "Get student profile by ID")
    public ResponseEntity<ApiResponse<StudentDto>> getStudentById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(studentService.getStudentById(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create new student profile")
    public ResponseEntity<ApiResponse<StudentDto>> createStudent(@RequestBody StudentDto dto) {
        return ResponseEntity.ok(ApiResponse.ok("Student created successfully", studentService.createStudent(dto)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN')")
    @Operation(summary = "Update student profile details")
    public ResponseEntity<ApiResponse<StudentDto>> updateStudent(@PathVariable String id, @RequestBody StudentDto dto) {
        return ResponseEntity.ok(ApiResponse.ok("Profile updated successfully", studentService.updateProfile(id, dto)));
    }
}
