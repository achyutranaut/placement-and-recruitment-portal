package com.placement.portal.student;

import com.placement.portal.auth.PortalUser;
import com.placement.portal.auth.Role;
import com.placement.portal.auth.UserRepository;
import com.placement.portal.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/students")
@Tag(name = "Students", description = "Student profile management and academic records")
public class StudentController {

    private final StudentService studentService;
    private final UserRepository userRepository;
    private final com.placement.portal.company.RecruiterAuthorizationService recruiterAuthService;

    public StudentController(
            StudentService studentService,
            UserRepository userRepository,
            com.placement.portal.company.RecruiterAuthorizationService recruiterAuthService
    ) {
        this.studentService = studentService;
        this.userRepository = userRepository;
        this.recruiterAuthService = recruiterAuthService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RECRUITER')")
    @Operation(summary = "List all students", description = "Retrieve list of all registered students with contact info and CGPA")
    public ResponseEntity<ApiResponse<List<StudentDto>>> getAllStudents(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            Principal principal
    ) {
        List<StudentDto> all = studentService.getAllStudents();

        if (principal != null) {
            var userOpt = userRepository.findByUsername(principal.getName());
            if (userOpt.isPresent() && userOpt.get().getRole() == Role.ROLE_RECRUITER) {
                all = all.stream()
                        .filter(s -> recruiterAuthService.isAuthorizedForStudent(principal, s.getStudentId()))
                        .collect(Collectors.toList());
            }
        }

        if (page != null || size != null) {
            int pageSize = (size != null) ? Math.min(Math.max(size, 1), 100) : 20;
            int pageNum = (page != null) ? Math.max(page, 0) : 0;
            int fromIndex = pageNum * pageSize;
            if (fromIndex >= all.size()) {
                return ResponseEntity.ok(ApiResponse.ok(Collections.emptyList()));
            }
            int toIndex = Math.min(fromIndex + pageSize, all.size());
            return ResponseEntity.ok(ApiResponse.ok(all.subList(fromIndex, toIndex)));
        }
        return ResponseEntity.ok(ApiResponse.ok(all));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Get authenticated student profile")
    public ResponseEntity<ApiResponse<StudentDto>> getMyProfile(Principal principal) {
        String studentId = resolveStudentId(principal);
        return ResponseEntity.ok(ApiResponse.ok(studentService.getStudentById(studentId)));
    }

    @PutMapping("/me")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Update authenticated student mutable profile details")
    public ResponseEntity<ApiResponse<StudentDto>> updateMyProfile(
            @Valid @RequestBody StudentUpdateDto updateDto,
            Principal principal
    ) {
        String studentId = resolveStudentId(principal);
        return ResponseEntity.ok(ApiResponse.ok("Profile updated successfully", studentService.updateStudentProfile(studentId, updateDto)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN', 'RECRUITER')")
    @Operation(summary = "Get student profile by ID")
    public ResponseEntity<ApiResponse<StudentDto>> getStudentById(@PathVariable String id, Principal principal) {
        if (principal != null) {
            PortalUser user = userRepository.findByUsername(principal.getName())
                    .orElseThrow(() -> new AccessDeniedException("Authenticated user not found"));
            if (user.getRole() == Role.ROLE_STUDENT) {
                String refId = user.getReferenceId();
                if (refId == null || !refId.equalsIgnoreCase(id)) {
                    throw new AccessDeniedException("Access denied: Students may only access their own profile records.");
                }
            } else if (user.getRole() == Role.ROLE_RECRUITER) {
                recruiterAuthService.requireAuthorizedForStudent(principal, id);
            }
        }
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
    public ResponseEntity<ApiResponse<StudentDto>> updateStudent(
            @PathVariable String id,
            @RequestBody StudentDto dto,
            Principal principal
    ) {
        if (principal != null) {
            PortalUser user = userRepository.findByUsername(principal.getName())
                    .orElseThrow(() -> new AccessDeniedException("User not found"));
            if (user.getRole() == Role.ROLE_STUDENT) {
                String refId = user.getReferenceId();
                if (refId == null || !refId.equalsIgnoreCase(id)) {
                    throw new AccessDeniedException("Access denied: You may only update your own student profile.");
                }
                // Students can only update mutable fields; academic fields are ignored
                StudentUpdateDto updateDto = new StudentUpdateDto(
                        dto.getName(),
                        dto.getStreet(),
                        dto.getCity(),
                        dto.getState(),
                        dto.getPhoneNumbers(),
                        dto.getSkills()
                );
                return ResponseEntity.ok(ApiResponse.ok("Profile updated successfully", studentService.updateStudentProfile(id, updateDto)));
            } else if (user.getRole() == Role.ROLE_ADMIN) {
                return ResponseEntity.ok(ApiResponse.ok("Profile updated successfully", studentService.adminUpdateStudent(id, dto)));
            }
        }
        return ResponseEntity.ok(ApiResponse.ok("Profile updated successfully", studentService.updateProfile(id, dto)));
    }

    private String resolveStudentId(Principal principal) {
        if (principal == null) {
            throw new AccessDeniedException("Authentication required");
        }
        return userRepository.findByUsername(principal.getName())
                .map(PortalUser::getReferenceId)
                .orElseThrow(() -> new AccessDeniedException("Student profile not found for authenticated user"));
    }
}
