package com.placement.portal.offer;

import com.placement.portal.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/offers")
@Tag(name = "Offers", description = "Official candidate employment offers and CTC packages")
public class OfferController {

    private final OfferService offerService;
    private final OfferLetterPdfService pdfService;
    private final com.placement.portal.student.StudentRepository studentRepository;
    private final com.placement.portal.auth.UserRepository userRepository;

    public OfferController(
            OfferService offerService,
            OfferLetterPdfService pdfService,
            com.placement.portal.student.StudentRepository studentRepository,
            com.placement.portal.auth.UserRepository userRepository
    ) {
        this.offerService = offerService;
        this.pdfService = pdfService;
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    @Operation(summary = "Issue job offer (Executes Oracle PL/SQL ISSUE_OFFER and triggers status transition to OFFERED)")
    public ResponseEntity<ApiResponse<OfferDto>> issueOffer(@Valid @RequestBody IssueOfferDto dto, java.security.Principal principal) {
        OfferDto created = offerService.issueOffer(dto, principal);
        return ResponseEntity.ok(ApiResponse.ok("Offer issued successfully via Oracle database", created));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RECRUITER')")
    @Operation(summary = "List all issued offers")
    public ResponseEntity<ApiResponse<List<OfferDto>>> getAllOffers() {
        return ResponseEntity.ok(ApiResponse.ok(offerService.getAllOffers()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get offer details by ID")
    public ResponseEntity<ApiResponse<OfferDto>> getOfferById(@PathVariable String id, java.security.Principal principal) {
        OfferDto dto = offerService.getOfferById(id);
        if (dto != null) {
            validateOfferAccess(dto, principal);
        }
        return ResponseEntity.ok(ApiResponse.ok(dto));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Get offers awarded to the authenticated student")
    public ResponseEntity<ApiResponse<List<OfferDto>>> getMyOffers(java.security.Principal principal) {
        String studentId = resolveStudentId(principal);
        return ResponseEntity.ok(ApiResponse.ok(offerService.getOffersByStudent(studentId)));
    }

    @PostMapping("/{id}/accept")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Accept an employment offer (finalizes placement; atomically declines other active offers)")
    public ResponseEntity<ApiResponse<OfferDto>> acceptOffer(@PathVariable String id, java.security.Principal principal) {
        String studentId = resolveStudentId(principal);
        OfferDto accepted = offerService.acceptOffer(id, studentId);
        return ResponseEntity.ok(ApiResponse.ok("Placement offer accepted successfully. Placement status finalized.", accepted));
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN')")
    @Operation(summary = "Get offers awarded to a student")
    public ResponseEntity<ApiResponse<List<OfferDto>>> getOffersByStudent(@PathVariable String studentId, java.security.Principal principal) {
        validateStudentAccess(studentId, principal);
        return ResponseEntity.ok(ApiResponse.ok(offerService.getOffersByStudent(studentId)));
    }

    @GetMapping("/application/{applicationId}")
    @Operation(summary = "Get offer by application ID")
    public ResponseEntity<ApiResponse<OfferDto>> getOfferByApplicationId(@PathVariable String applicationId) {
        OfferDto offer = offerService.getOfferByApplicationId(applicationId);
        return ResponseEntity.ok(ApiResponse.ok(offer));
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("hasAnyRole('STUDENT', 'RECRUITER', 'ADMIN')")
    @Operation(summary = "Download official institutional offer letter PDF")
    public ResponseEntity<byte[]> downloadOfferPdf(@PathVariable String id, java.security.Principal principal) {
        OfferDto offer = offerService.getOfferById(id);
        if (offer == null) {
            return ResponseEntity.notFound().build();
        }

        validateOfferAccess(offer, principal);

        com.placement.portal.student.Student student = studentRepository.findById(offer.getStudentId()).orElse(null);
        byte[] pdfBytes = pdfService.generateOfferLetterPdf(offer, student);

        String filename = "Offer_Letter_" + offer.getOfferId() + ".pdf";
        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(pdfBytes);
    }

    private void validateOfferAccess(OfferDto offer, java.security.Principal principal) {
        if (principal == null) return;
        userRepository.findByUsername(principal.getName()).ifPresent(user -> {
            if (user.getRole() == com.placement.portal.auth.Role.ROLE_STUDENT) {
                String refId = user.getReferenceId();
                if (refId != null && !refId.equalsIgnoreCase(offer.getStudentId())) {
                    throw new org.springframework.security.access.AccessDeniedException(
                            "Access denied: Students may only access their own offer letters."
                    );
                }
            }
        });
    }

    private void validateStudentAccess(String studentId, java.security.Principal principal) {
        if (principal == null) return;
        userRepository.findByUsername(principal.getName()).ifPresent(user -> {
            if (user.getRole() == com.placement.portal.auth.Role.ROLE_STUDENT) {
                String refId = user.getReferenceId();
                if (refId != null && !refId.equalsIgnoreCase(studentId)) {
                    throw new org.springframework.security.access.AccessDeniedException(
                            "Access denied: Students may only access their own offer records."
                    );
                }
            }
        });
    }

    private String resolveStudentId(java.security.Principal principal) {
        if (principal == null) {
            throw new org.springframework.security.access.AccessDeniedException("Authentication required");
        }
        return userRepository.findByUsername(principal.getName())
                .map(com.placement.portal.auth.PortalUser::getReferenceId)
                .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("Student profile not found for authenticated user"));
    }
}
