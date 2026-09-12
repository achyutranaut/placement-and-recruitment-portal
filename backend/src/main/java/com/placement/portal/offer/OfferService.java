package com.placement.portal.offer;

import com.placement.portal.application.Application;
import com.placement.portal.application.ApplicationRepository;
import com.placement.portal.application.DailyRoutineRepository;
import com.placement.portal.company.CompanyRepository;
import com.placement.portal.company.EmailCompany;
import com.placement.portal.company.EmailCompanyRepository;
import com.placement.portal.company.JobCompanyRepository;
import com.placement.portal.application.ApplicationAudit;
import com.placement.portal.application.ApplicationAuditRepository;
import com.placement.portal.recruitment.PlacementDrive;
import com.placement.portal.recruitment.PlacementDriveRepository;
import com.placement.portal.student.Student;
import com.placement.portal.student.StudentRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OfferService {

    private final OfferRepository offerRepository;
    private final OfferJdbcDao offerJdbcDao;
    private final ApplicationRepository applicationRepository;
    private final ApplicationAuditRepository auditRepository;
    private final DailyRoutineRepository dailyRoutineRepository;
    private final StudentRepository studentRepository;
    private final PlacementDriveRepository driveRepository;
    private final JobCompanyRepository jobCompanyRepository;
    private final CompanyRepository companyRepository;
    private final EmailCompanyRepository emailCompanyRepository;
    private final com.placement.portal.auth.UserRepository userRepository;
    private final com.placement.portal.company.RecruiterCompanyRepository recruiterCompanyRepository;

    public OfferService(
            OfferRepository offerRepository,
            OfferJdbcDao offerJdbcDao,
            ApplicationRepository applicationRepository,
            ApplicationAuditRepository auditRepository,
            DailyRoutineRepository dailyRoutineRepository,
            StudentRepository studentRepository,
            PlacementDriveRepository driveRepository,
            JobCompanyRepository jobCompanyRepository,
            CompanyRepository companyRepository,
            EmailCompanyRepository emailCompanyRepository,
            com.placement.portal.auth.UserRepository userRepository,
            com.placement.portal.company.RecruiterCompanyRepository recruiterCompanyRepository
    ) {
        this.offerRepository = offerRepository;
        this.offerJdbcDao = offerJdbcDao;
        this.applicationRepository = applicationRepository;
        this.auditRepository = auditRepository;
        this.dailyRoutineRepository = dailyRoutineRepository;
        this.studentRepository = studentRepository;
        this.driveRepository = driveRepository;
        this.jobCompanyRepository = jobCompanyRepository;
        this.companyRepository = companyRepository;
        this.emailCompanyRepository = emailCompanyRepository;
        this.userRepository = userRepository;
        this.recruiterCompanyRepository = recruiterCompanyRepository;
    }

    @Transactional
    public OfferDto issueOffer(IssueOfferDto dto) {
        return issueOffer(dto, null);
    }

    @Transactional
    public OfferDto issueOffer(IssueOfferDto dto, java.security.Principal principal) {
        Application app = applicationRepository.findById(dto.getApplicationId())
                .orElseThrow(() -> new IllegalArgumentException("Application not found: " + dto.getApplicationId()));

        // Prevent duplicate offers
        if (offerRepository.findByApplicationId(dto.getApplicationId()).isPresent()) {
            throw new IllegalStateException("An employment offer has already been issued for application: " + dto.getApplicationId());
        }

        if (!"SELECTED".equalsIgnoreCase(app.getStatus()) && !"INTERVIEWING".equalsIgnoreCase(app.getStatus())) {
            throw new IllegalStateException("Candidate must be in SELECTED or INTERVIEWING state to receive an offer. Current status: " + app.getStatus());
        }

        String resolvedDriveId = app.getDriveId();
        if (resolvedDriveId == null || resolvedDriveId.isBlank()) {
            var routineOpt = dailyRoutineRepository.findByStudentIdAndApplyDate(app.getStudentId(), app.getApplyDate());
            if (routineOpt.isPresent()) {
                resolvedDriveId = routineOpt.get().getDriveId();
            }
        }
        final String driveId = resolvedDriveId;

        // Validate recruiter authorization for this drive's company
        if (principal != null) {
            userRepository.findByUsername(principal.getName()).ifPresent(user -> {
                if (user.getRole() == com.placement.portal.auth.Role.ROLE_RECRUITER) {
                    String recruiterCompId = user.getReferenceId();
                    if (driveId != null) {
                        driveRepository.findById(driveId).ifPresent(drv -> {
                            jobCompanyRepository.findById(drv.getJobTitle()).ifPresent(jc -> {
                                boolean authorized = (recruiterCompId != null && recruiterCompId.equalsIgnoreCase(jc.getCompanyId()))
                                        || recruiterCompanyRepository.existsByUserIdAndCompanyId(user.getUserId(), jc.getCompanyId());
                                if (!authorized) {
                                    throw new AccessDeniedException("Access denied: You may only release offers for recruitment drives belonging to your authorized company (" + jc.getCompanyId() + ").");
                                }
                            });
                        });
                    }
                }
            });
        }

        // Authoritative CTC from Oracle PlacementDrive record
        BigDecimal canonicalCtc = null;
        if (driveId != null) {
            canonicalCtc = driveRepository.findById(driveId)
                    .map(PlacementDrive::getCtc)
                    .orElse(null);
        }
        if (canonicalCtc == null || canonicalCtc.compareTo(BigDecimal.ZERO) <= 0) {
            canonicalCtc = dto.getCtcLpa();
        }
        if (canonicalCtc == null || canonicalCtc.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Unable to determine canonical compensation (CTC) for drive: " + driveId);
        }

        LocalDate date = dto.getOfferDate() != null ? dto.getOfferDate() : LocalDate.now();
        String offerId = offerJdbcDao.issueOffer(dto.getApplicationId(), canonicalCtc, date);

        // Update student placement status to 'OFFERED' if currently 'NOT_PLACED'
        studentRepository.findById(app.getStudentId()).ifPresent(student -> {
            if ("NOT_PLACED".equalsIgnoreCase(student.getPlacementStatus())) {
                student.setPlacementStatus("OFFERED");
                studentRepository.save(student);
            }
        });

        String oldAppStatus = app.getStatus();
        app.setStatus("OFFERED");
        applicationRepository.save(app);

        // Audit trail
        auditRepository.save(new ApplicationAudit(
                app.getApplicationId(),
                oldAppStatus,
                "OFFERED",
                principal != null ? principal.getName() : "RECRUITER"
        ));

        return getOfferById(offerId);
    }

    public List<OfferDto> getAllOffers() {
        return offerRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public OfferDto getOfferById(String offerId) {
        OfferLetter ol = offerRepository.findById(offerId)
                .orElseThrow(() -> new IllegalArgumentException("Offer not found: " + offerId));
        return mapToDto(ol);
    }

    public OfferDto getOfferByApplicationId(String applicationId) {
        return offerRepository.findByApplicationId(applicationId)
                .map(this::mapToDto)
                .orElse(null);
    }

    public List<OfferDto> getOffersByStudent(String studentId) {
        List<String> appIds = applicationRepository.findByStudentId(studentId).stream()
                .map(Application::getApplicationId)
                .collect(Collectors.toList());

        return offerRepository.findAll().stream()
                .filter(ol -> appIds.contains(ol.getApplicationId()))
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Accepts a specific employment offer for an authenticated student.
     * Atomic transaction:
     * 1. Verifies student ownership
     * 2. Verifies offer is currently in OFFERED state
     * 3. Verifies student has not already finalized another offer
     * 4. Updates selected offer and application to ACCEPTED
     * 5. Atomically transitions all other active offers for this student to DECLINED
     * 6. Updates student's placement status to PLACED
     * 7. Logs full audit trail
     */
    @Transactional
    public synchronized OfferDto acceptOffer(String offerId, String studentId) {
        OfferLetter offer = offerRepository.findById(offerId)
                .orElseThrow(() -> new IllegalArgumentException("Offer not found: " + offerId));

        Application app = applicationRepository.findById(offer.getApplicationId())
                .orElseThrow(() -> new IllegalStateException("Application record not found for offer: " + offerId));

        if (!app.getStudentId().equalsIgnoreCase(studentId)) {
            throw new AccessDeniedException("Access denied: You may only accept offers awarded directly to your student profile.");
        }

        if (!"OFFERED".equalsIgnoreCase(offer.getStatus())) {
            throw new IllegalStateException("Offer " + offerId + " cannot be accepted because its status is '" + offer.getStatus() + "'.");
        }

        // Verify that the student has not already finalized an offer
        List<String> appIds = applicationRepository.findByStudentId(studentId).stream()
                .map(Application::getApplicationId)
                .toList();

        List<OfferLetter> studentOffers = offerRepository.findAll().stream()
                .filter(o -> appIds.contains(o.getApplicationId()))
                .toList();

        boolean alreadyAccepted = studentOffers.stream()
                .anyMatch(o -> "ACCEPTED".equalsIgnoreCase(o.getStatus()));
        if (alreadyAccepted) {
            throw new IllegalStateException("Placement already finalized: You have already accepted another placement offer. Accepting multiple offers is strictly prohibited.");
        }

        // 1. Mark this offer as ACCEPTED
        offer.setStatus("ACCEPTED");
        offer.setAcceptedAt(LocalDateTime.now());
        offerRepository.save(offer);

        // 2. Mark this application as ACCEPTED
        String oldAppStatus = app.getStatus();
        app.setStatus("ACCEPTED");
        applicationRepository.save(app);
        auditRepository.save(new ApplicationAudit(app.getApplicationId(), oldAppStatus, "ACCEPTED", "STUDENT: " + studentId));

        // 3. Atomically decline all other competing active offers for this student
        for (OfferLetter other : studentOffers) {
            if (!other.getOfferId().equalsIgnoreCase(offer.getOfferId()) && "OFFERED".equalsIgnoreCase(other.getStatus())) {
                other.setStatus("DECLINED");
                offerRepository.save(other);

                applicationRepository.findById(other.getApplicationId()).ifPresent(otherApp -> {
                    String otherOld = otherApp.getStatus();
                    otherApp.setStatus("DECLINED");
                    applicationRepository.save(otherApp);
                    auditRepository.save(new ApplicationAudit(otherApp.getApplicationId(), otherOld, "DECLINED", "STUDENT_OFFER_SELECTION_MUTEX"));
                });
            }
        }

        // 4. Update student placement status to PLACED
        studentRepository.findById(studentId).ifPresent(student -> {
            student.setPlacementStatus("PLACED");
            studentRepository.save(student);
        });

        return mapToDto(offer);
    }

    private OfferDto mapToDto(OfferLetter ol) {
        String studentId = "";
        String studentName = "Student";
        String driveId = "";
        String jobTitle = "Campus Placement Position";
        String companyName = "Corporate Recruiter";

        var appOpt = applicationRepository.findById(ol.getApplicationId());
        if (appOpt.isPresent()) {
            Application app = appOpt.get();
            studentId = app.getStudentId();
            studentName = studentRepository.findById(studentId).map(Student::getName).orElse("Student");

            driveId = app.getDriveId();
            if (driveId == null || driveId.isBlank()) {
                var routineOpt = dailyRoutineRepository.findByStudentIdAndApplyDate(app.getStudentId(), app.getApplyDate());
                if (routineOpt.isPresent()) {
                    driveId = routineOpt.get().getDriveId();
                }
            }

            if (driveId != null && !driveId.isBlank()) {
                var driveOpt = driveRepository.findById(driveId);
                if (driveOpt.isPresent()) {
                    jobTitle = driveOpt.get().getJobTitle();
                    var jobOpt = jobCompanyRepository.findById(jobTitle);
                    if (jobOpt.isPresent()) {
                        var compOpt = companyRepository.findById(jobOpt.get().getCompanyId());
                        if (compOpt.isPresent()) {
                            companyName = emailCompanyRepository.findById(compOpt.get().getEmail())
                                    .map(EmailCompany::getCompanyName).orElse(companyName);
                        }
                    }
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
                driveId != null ? driveId : ""
        );
    }
}
