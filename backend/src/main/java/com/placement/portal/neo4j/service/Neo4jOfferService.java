package com.placement.portal.neo4j.service;

import com.placement.portal.application.IApplicationService;
import com.placement.portal.application.SelectionEligibility;
import com.placement.portal.neo4j.model.*;
import com.placement.portal.neo4j.repository.*;
import com.placement.portal.offer.IOfferService;
import com.placement.portal.offer.IssueOfferDto;
import com.placement.portal.offer.OfferDto;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Profile("neo4j")
@Primary
public class Neo4jOfferService implements IOfferService {

    private final Neo4jOfferRepository offerRepository;
    private final Neo4jApplicationRepository applicationRepository;
    private final Neo4jStudentRepository studentRepository;
    private final Neo4jDriveRepository driveRepository;
    private final Neo4jAuditEventRepository auditRepository;
    private final Neo4jUserRepository userRepository;
    private final IApplicationService applicationService;

    public Neo4jOfferService(
            Neo4jOfferRepository offerRepository,
            Neo4jApplicationRepository applicationRepository,
            Neo4jStudentRepository studentRepository,
            Neo4jDriveRepository driveRepository,
            Neo4jAuditEventRepository auditRepository,
            Neo4jUserRepository userRepository,
            IApplicationService applicationService
    ) {
        this.offerRepository = offerRepository;
        this.applicationRepository = applicationRepository;
        this.studentRepository = studentRepository;
        this.driveRepository = driveRepository;
        this.auditRepository = auditRepository;
        this.userRepository = userRepository;
        this.applicationService = applicationService;
    }

    @Override
    @Transactional
    public OfferDto issueOffer(IssueOfferDto dto) {
        return issueOffer(dto, null);
    }

    /**
     * Ported PL/SQL Procedure: ISSUE_OFFER + Triggers
     * Enforces:
     * 1. Application exists and candidate in SELECTED state.
     * 2. All mandatory rounds cleared via applicationService.evaluateSelectionEligibility.
     * 3. Prevents duplicate offer on same application.
     * 4. Derives canonical CTC from Drive or request.
     * 5. Atomically creates OfferNode, links (Application)-[:RECEIVED_OFFER]->(Offer)
     * 6. Application status -> OFFERED.
     * 7. Student placement status -> OFFERED (if previously NOT_PLACED).
     * 8. Writes audit log event.
     */
    @Override
    @Transactional
    public OfferDto issueOffer(IssueOfferDto dto, Principal principal) {
        ApplicationNode app = applicationRepository.findByApplicationId(dto.getApplicationId())
                .orElseThrow(() -> new IllegalArgumentException("Application not found: " + dto.getApplicationId()));

        if (offerRepository.findByApplicationId(dto.getApplicationId()).isPresent()) {
            throw new IllegalStateException("An employment offer has already been issued for application: " + dto.getApplicationId());
        }

        if (!"SELECTED".equalsIgnoreCase(app.getStatus())) {
            throw new IllegalStateException("Candidate must be in SELECTED state to receive an offer. Current status: " + app.getStatus());
        }

        SelectionEligibility readiness = applicationService.evaluateSelectionEligibility(dto.getApplicationId());
        if (!readiness.isEligible()) {
            throw new IllegalStateException("Cannot issue offer: " + readiness.getReason());
        }

        String driveId = app.getDriveId();
        BigDecimal canonicalCtc = null;
        if (driveId != null) {
            canonicalCtc = driveRepository.findByDriveId(driveId).map(DriveNode::getCtc).orElse(null);
        }
        if (canonicalCtc == null || canonicalCtc.compareTo(BigDecimal.ZERO) <= 0) {
            canonicalCtc = dto.getCtcLpa();
        }
        if (canonicalCtc == null || canonicalCtc.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Unable to determine canonical compensation (CTC) for drive: " + driveId);
        }

        long count = offerRepository.count();
        String offerId = "OFF" + String.format("%03d", count + 1);
        while (offerRepository.findByOfferId(offerId).isPresent()) {
            count++;
            offerId = "OFF" + String.format("%03d", count + 1);
        }

        LocalDate date = dto.getOfferDate() != null ? dto.getOfferDate() : LocalDate.now();
        OfferNode offer = new OfferNode(offerId, dto.getApplicationId(), date, canonicalCtc, "OFFERED");
        offerRepository.save(offer);

        // Update student status to OFFERED
        studentRepository.findByStudentId(app.getStudentId()).ifPresent(student -> {
            if ("NOT_PLACED".equalsIgnoreCase(student.getPlacementStatus())) {
                student.setPlacementStatus("OFFERED");
                studentRepository.save(student);
            }
        });

        // Trigger simulation: Application -> OFFERED
        String oldAppStatus = app.getStatus();
        app.setStatus("OFFERED");
        app.setOffer(offer);
        applicationRepository.save(app);

        // Audit journal write
        AuditEventNode audit = new AuditEventNode(
                "AUD_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                app.getApplicationId(),
                oldAppStatus,
                "OFFERED",
                principal != null ? principal.getName() : "RECRUITER"
        );
        auditRepository.save(audit);

        return toDto(offer);
    }

    @Override
    public List<OfferDto> getAllOffers() {
        return offerRepository.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    public OfferDto getOfferById(String offerId) {
        OfferNode ol = offerRepository.findByOfferId(offerId)
                .orElseThrow(() -> new IllegalArgumentException("Offer not found: " + offerId));
        return toDto(ol);
    }

    @Override
    public OfferDto getOfferByApplicationId(String applicationId) {
        return offerRepository.findByApplicationId(applicationId)
                .map(this::toDto)
                .orElse(null);
    }

    @Override
    public List<OfferDto> getOffersByStudent(String studentId) {
        List<ApplicationNode> apps = applicationRepository.findByStudentId(studentId);
        List<OfferDto> list = new ArrayList<>();
        for (ApplicationNode a : apps) {
            offerRepository.findByApplicationId(a.getApplicationId()).ifPresent(o -> list.add(toDto(o)));
        }
        return list;
    }

    /**
     * Ported Trigger / Atomic acceptance:
     * 1. Verifies offer in OFFERED status.
     * 2. Verifies student hasn't already accepted another offer.
     * 3. Sets selected offer to ACCEPTED, application to ACCEPTED.
     * 4. Atomically declines all competing active offers for this student.
     * 5. Sets student placementStatus to PLACED.
     * 6. Writes full audit trail.
     */
    @Override
    @Transactional
    public OfferDto acceptOffer(String offerId, String studentId) {
        StudentNode student = studentRepository.findByStudentId(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found: " + studentId));

        OfferNode offer = offerRepository.findByOfferId(offerId)
                .orElseThrow(() -> new IllegalArgumentException("Offer not found: " + offerId));

        ApplicationNode app = applicationRepository.findByApplicationId(offer.getApplicationId())
                .orElseThrow(() -> new IllegalStateException("Application record not found for offer: " + offerId));

        if (!app.getStudentId().equalsIgnoreCase(studentId)) {
            throw new AccessDeniedException("Access denied: You may only accept offers awarded directly to your student profile.");
        }

        if (!"OFFERED".equalsIgnoreCase(offer.getStatus())) {
            throw new IllegalStateException("Offer " + offerId + " cannot be accepted because its status is '" + offer.getStatus() + "'.");
        }

        List<OfferDto> studentOffers = getOffersByStudent(studentId);
        boolean alreadyAccepted = studentOffers.stream().anyMatch(o -> "ACCEPTED".equalsIgnoreCase(o.getStatus()));
        if (alreadyAccepted) {
            throw new IllegalStateException("Placement already finalized: You have already accepted another placement offer. Accepting multiple offers is strictly prohibited.");
        }

        // 1. Mark this offer ACCEPTED
        offer.setStatus("ACCEPTED");
        offer.setAcceptedAt(LocalDateTime.now());
        offerRepository.save(offer);

        // 2. Mark this application ACCEPTED
        String oldAppStatus = app.getStatus();
        app.setStatus("ACCEPTED");
        applicationRepository.save(app);

        AuditEventNode audit = new AuditEventNode(
                "AUD_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                app.getApplicationId(),
                oldAppStatus,
                "ACCEPTED",
                "STUDENT: " + studentId
        );
        auditRepository.save(audit);

        // 3. Atomically decline all other competing offers
        for (OfferDto other : studentOffers) {
            if (!other.getOfferId().equalsIgnoreCase(offerId) && "OFFERED".equalsIgnoreCase(other.getStatus())) {
                offerRepository.findByOfferId(other.getOfferId()).ifPresent(o -> {
                    o.setStatus("DECLINED");
                    offerRepository.save(o);
                });
                applicationRepository.findByApplicationId(other.getApplicationId()).ifPresent(oa -> {
                    String oOld = oa.getStatus();
                    oa.setStatus("DECLINED");
                    applicationRepository.save(oa);

                    AuditEventNode declineAudit = new AuditEventNode(
                            "AUD_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                            oa.getApplicationId(),
                            oOld,
                            "DECLINED",
                            "STUDENT_OFFER_SELECTION_MUTEX"
                    );
                    auditRepository.save(declineAudit);
                });
            }
        }

        // 4. Update student to PLACED
        student.setPlacementStatus("PLACED");
        studentRepository.save(student);

        return toDto(offer);
    }

    private OfferDto toDto(OfferNode ol) {
        String studentId = "";
        String studentName = "Student";
        String driveId = "";
        String jobTitle = "Campus Placement Position";
        String companyName = "Corporate Recruiter";

        var appOpt = applicationRepository.findByApplicationId(ol.getApplicationId());
        if (appOpt.isPresent()) {
            ApplicationNode app = appOpt.get();
            studentId = app.getStudentId();
            studentName = studentRepository.findByStudentId(studentId).map(StudentNode::getName).orElse("Student");
            driveId = app.getDriveId() != null ? app.getDriveId() : "";
            if (!driveId.isBlank()) {
                var drvOpt = driveRepository.findByDriveId(driveId);
                if (drvOpt.isPresent()) {
                    DriveNode drv = drvOpt.get();
                    jobTitle = drv.getJobTitle();
                    companyName = drv.getCompanyName() != null ? drv.getCompanyName() : "Corporate Recruiter";
                }
            }
        }

        return new OfferDto(
                ol.getOfferId(),
                ol.getApplicationId(),
                studentId,
                studentName,
                jobTitle,
                companyName,
                ol.getOfferDate(),
                ol.getCtcLpa(),
                ol.getStatus() != null ? ol.getStatus() : "OFFERED",
                ol.getAcceptedAt(),
                driveId
        );
    }
}
