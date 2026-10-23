package com.placement.portal.offer;

import com.placement.portal.auth.Role;
import com.placement.portal.auth.UserRepository;
import com.placement.portal.common.ApiResponse;
import com.placement.portal.company.RecruiterAuthorizationService;
import com.placement.portal.student.Student;
import com.placement.portal.student.StudentRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/offers")
@Tag(name = "Offers", description = "Official candidate employment offers and CTC packages")
public class OfferController {

    private final OfferService offerService;
    private final OfferLetterPdfService pdfService;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final RecruiterAuthorizationService recruiterAuthService;

    public OfferController(
            OfferService offerService,
            OfferLetterPdfService pdfService,
            StudentRepository studentRepository,
            UserRepository userRepository,
            RecruiterAuthorizationService recruiterAuthService
    ) {
        this.offerService = offerService;
        this.pdfService = pdfService;
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.recruiterAuthService = recruiterAuthService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    @Operation(summary = "Issue job offer (Executes Oracle PL/SQL ISSUE_OFFER and triggers status transition to OFFERED)")
    public ResponseEntity<ApiResponse<OfferDto>> issueOffer(@Valid @RequestBody IssueOfferDto dto, Principal principal) {
        OfferDto created = offerService.issueOffer(dto, principal);
        return ResponseEntity.ok(ApiResponse.ok("Offer issued successfully via Oracle database", created));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RECRUITER')")
    @Operation(summary = "List all issued offers")
    public ResponseEntity<ApiResponse<List<OfferDto>>> getAllOffers(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            Principal principal
    ) {
        List<OfferDto> offers = offerService.getAllOffers();

        if (principal != null) {
            var userOpt = userRepository.findByUsername(principal.getName());
            if (userOpt.isPresent() && userOpt.get().getRole() == Role.ROLE_RECRUITER) {
                Set<String> authorizedDrives = recruiterAuthService.getAuthorizedDriveIds(principal);
                offers = offers.stream()
                        .filter(o -> authorizedDrives.contains(o.getDriveId()))
                        .collect(Collectors.toList());
            }
        }

        if (page != null || size != null) {
            int pageSize = (size != null) ? Math.min(Math.max(size, 1), 100) : 20;
            int pageNum = (page != null) ? Math.max(page, 0) : 0;
            int fromIndex = pageNum * pageSize;
            if (fromIndex >= offers.size()) {
                return ResponseEntity.ok(ApiResponse.ok(Collections.emptyList()));
            }
            int toIndex = Math.min(fromIndex + pageSize, offers.size());
            return ResponseEntity.ok(ApiResponse.ok(offers.subList(fromIndex, toIndex)));
        }

        return ResponseEntity.ok(ApiResponse.ok(offers));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Get offers awarded to the authenticated student")
    public ResponseEntity<ApiResponse<List<OfferDto>>> getMyOffers(Principal principal) {
        String studentId = resolveStudentId(principal);
        return ResponseEntity.ok(ApiResponse.ok(offerService.getOffersByStudent(studentId)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDENT', 'RECRUITER', 'ADMIN')")
    @Operation(summary = "Get offer details by ID")
    public ResponseEntity<ApiResponse<OfferDto>> getOfferById(@PathVariable String id, Principal principal) {
        OfferDto dto = offerService.getOfferById(id);
        if (dto != null) {
            validateOfferAccess(dto, principal);
        }
        return ResponseEntity.ok(ApiResponse.ok(dto));
    }

    @PostMapping("/{id}/accept")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Accept an employment offer (finalizes placement; atomically declines other active offers)")
    public ResponseEntity<ApiResponse<OfferDto>> acceptOffer(@PathVariable String id, Principal principal) {
        String studentId = resolveStudentId(principal);
        OfferDto accepted = offerService.acceptOffer(id, studentId);
        return ResponseEntity.ok(ApiResponse.ok("Placement offer accepted successfully. Placement status finalized.", accepted));
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN')")
    @Operation(summary = "Get offers awarded to a student")
    public ResponseEntity<ApiResponse<List<OfferDto>>> getOffersByStudent(@PathVariable String studentId, Principal principal) {
        validateStudentAccess(studentId, principal);
        return ResponseEntity.ok(ApiResponse.ok(offerService.getOffersByStudent(studentId)));
    }

    @GetMapping("/application/{applicationId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'RECRUITER', 'ADMIN')")
    @Operation(summary = "Get offer by application ID")
    public ResponseEntity<ApiResponse<OfferDto>> getOfferByApplicationId(
            @PathVariable String applicationId,
            Principal principal
    ) {
        OfferDto offer = offerService.getOfferByApplicationId(applicationId);
        if (offer != null) {
            validateOfferAccess(offer, principal);
        }
        return ResponseEntity.ok(ApiResponse.ok(offer));
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("hasAnyRole('STUDENT', 'RECRUITER', 'ADMIN')")
    @Operation(summary = "Download official institutional offer letter PDF")
    public ResponseEntity<byte[]> downloadOfferPdf(@PathVariable String id, Principal principal) {
        OfferDto offer = offerService.getOfferById(id);
        if (offer == null) {
            return ResponseEntity.notFound().build();
        }

        validateOfferAccess(offer, principal);

        Student student = studentRepository.findById(offer.getStudentId()).orElse(null);
        byte[] pdfBytes = pdfService.generateOfferLetterPdf(offer, student);

        String filename = "Offer_Letter_" + offer.getOfferId() + ".pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(pdfBytes);
    }

    private void validateOfferAccess(OfferDto offer, Principal principal) {
        if (principal == null) return;
        userRepository.findByUsername(principal.getName()).ifPresent(user -> {
            if (user.getRole() == Role.ROLE_STUDENT) {
                String refId = user.getReferenceId();
                if (refId == null || !refId.equalsIgnoreCase(offer.getStudentId())) {
                    throw new AccessDeniedException(
                            "Access denied: Students may only access their own offer letters."
                    );
                }
            } else if (user.getRole() == Role.ROLE_RECRUITER) {
                recruiterAuthService.requireAuthorizedForApplication(principal, offer.getApplicationId());
            }
        });
    }

    private void validateStudentAccess(String studentId, Principal principal) {
        if (principal == null) return;
        userRepository.findByUsername(principal.getName()).ifPresent(user -> {
            if (user.getRole() == Role.ROLE_STUDENT) {
                String refId = user.getReferenceId();
                if (refId == null || !refId.equalsIgnoreCase(studentId)) {
                    throw new AccessDeniedException(
                            "Access denied: Students may only access their own offer records."
                    );
                }
            } else if (user.getRole() == Role.ROLE_RECRUITER) {
                recruiterAuthService.requireAuthorizedForStudent(principal, studentId);
            }
        });
    }

    private String resolveStudentId(Principal principal) {
        if (principal == null) {
            throw new AccessDeniedException("Authentication required");
        }
        return userRepository.findByUsername(principal.getName())
                .map(com.placement.portal.auth.PortalUser::getReferenceId)
                .orElseThrow(() -> new AccessDeniedException("Student profile not found for authenticated user"));
    }
}
