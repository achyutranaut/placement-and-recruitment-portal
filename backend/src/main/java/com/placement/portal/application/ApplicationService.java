package com.placement.portal.application;

import com.placement.portal.company.CompanyRepository;
import com.placement.portal.company.EmailCompany;
import com.placement.portal.company.EmailCompanyRepository;
import com.placement.portal.company.JobCompanyRepository;
import com.placement.portal.interview.Interview;
import com.placement.portal.interview.InterviewDto;
import com.placement.portal.interview.InterviewRepository;
import com.placement.portal.interview.InterviewerRound;
import com.placement.portal.interview.InterviewerRoundRepository;
import com.placement.portal.offer.OfferRepository;
import com.placement.portal.recruitment.DriveEligibilityDto;
import com.placement.portal.recruitment.PlacementDrive;
import com.placement.portal.recruitment.PlacementDriveRepository;
import com.placement.portal.recruitment.PlacementDriveService;
import com.placement.portal.student.Student;
import com.placement.portal.student.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final DailyRoutineRepository dailyRoutineRepository;
    private final ApplicationAuditRepository auditRepository;
    private final ApplicationJdbcDao applicationJdbcDao;
    private final StudentRepository studentRepository;
    private final PlacementDriveRepository driveRepository;
    private final JobCompanyRepository jobCompanyRepository;
    private final CompanyRepository companyRepository;
    private final EmailCompanyRepository emailCompanyRepository;
    private final PlacementDriveService placementDriveService;
    private final InterviewRepository interviewRepository;
    private final InterviewerRoundRepository roundRepository;
    private final OfferRepository offerRepository;

    public ApplicationService(
            ApplicationRepository applicationRepository,
            DailyRoutineRepository dailyRoutineRepository,
            ApplicationAuditRepository auditRepository,
            ApplicationJdbcDao applicationJdbcDao,
            StudentRepository studentRepository,
            PlacementDriveRepository driveRepository,
            JobCompanyRepository jobCompanyRepository,
            CompanyRepository companyRepository,
            EmailCompanyRepository emailCompanyRepository,
            PlacementDriveService placementDriveService,
            InterviewRepository interviewRepository,
            InterviewerRoundRepository roundRepository,
            OfferRepository offerRepository
    ) {
        this.applicationRepository = applicationRepository;
        this.dailyRoutineRepository = dailyRoutineRepository;
        this.auditRepository = auditRepository;
        this.applicationJdbcDao = applicationJdbcDao;
        this.studentRepository = studentRepository;
        this.driveRepository = driveRepository;
        this.jobCompanyRepository = jobCompanyRepository;
        this.companyRepository = companyRepository;
        this.emailCompanyRepository = emailCompanyRepository;
        this.placementDriveService = placementDriveService;
        this.interviewRepository = interviewRepository;
        this.roundRepository = roundRepository;
        this.offerRepository = offerRepository;
    }

    @Transactional
    public ApplicationDto applyForDrive(String studentId, String driveId, LocalDate applyDate) {
        // Enforce authoritative backend eligibility check
        var eligibility = placementDriveService.evaluateEligibility(driveId, studentId);
        if (!eligibility.isEligible()) {
            throw new IllegalArgumentException(eligibility.getMessage());
        }

        LocalDate date = applyDate != null ? applyDate : LocalDate.now();
        // Invoke Oracle Stored Procedure via Spring JDBC
        String applicationId = applicationJdbcDao.applyForDrive(studentId, driveId, date);
        applicationRepository.findById(applicationId).ifPresent(app -> {
            if (app.getDriveId() == null) {
                app.setDriveId(driveId);
                applicationRepository.save(app);
            }
        });
        return getApplicationById(applicationId);
    }

    public List<ApplicationDto> getAllApplications() {
        return applicationRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<ApplicationDto> getApplicationsByStudent(String studentId) {
        return applicationRepository.findByStudentId(studentId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<ApplicationDto> getApplicationsByDrive(String driveId) {
        List<String> studentIds = dailyRoutineRepository.findByDriveId(driveId).stream()
                .map(StudentDailyRoutineDrive::getStudentId)
                .collect(Collectors.toList());

        return applicationRepository.findAll().stream()
                .filter(app -> studentIds.contains(app.getStudentId()))
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public ApplicationDto getApplicationById(String applicationId) {
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found: " + applicationId));
        return mapToDto(app);
    }

    @Transactional
    public ApplicationDto updateStatus(String applicationId, String newStatus, String changedBy) {
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found: " + applicationId));

        String oldStatus = app.getStatus();
        validateStatusTransition(oldStatus, newStatus);

        app.setStatus(newStatus.toUpperCase());
        Application saved = applicationRepository.save(app);

        // Record audit entry in real APPLICATION_AUDIT table
        auditRepository.save(new ApplicationAudit(applicationId, oldStatus, newStatus.toUpperCase(), changedBy != null ? changedBy : "RECRUITER"));

        return mapToDto(saved);
    }

    private void validateStatusTransition(String oldStatus, String newStatus) {
        if (oldStatus == null || newStatus == null) return;
        if (oldStatus.equalsIgnoreCase(newStatus)) return;

        if ("OFFERED".equalsIgnoreCase(oldStatus) || "REJECTED".equalsIgnoreCase(oldStatus)) {
            throw new IllegalStateException("Application is in terminal state '" + oldStatus + "' and cannot be updated to '" + newStatus + "'");
        }

        boolean valid = switch (oldStatus.toUpperCase()) {
            case "APPLIED" -> newStatus.equalsIgnoreCase("SHORTLISTED") || newStatus.equalsIgnoreCase("REJECTED");
            case "SHORTLISTED" -> newStatus.equalsIgnoreCase("INTERVIEWING") || newStatus.equalsIgnoreCase("REJECTED");
            case "INTERVIEWING" -> newStatus.equalsIgnoreCase("SELECTED") || newStatus.equalsIgnoreCase("REJECTED");
            case "SELECTED" -> newStatus.equalsIgnoreCase("OFFERED") || newStatus.equalsIgnoreCase("REJECTED");
            default -> false;
        };

        if (!valid) {
            throw new IllegalStateException("Invalid status transition: cannot change application from '" + oldStatus + "' to '" + newStatus + "'");
        }
    }

    private ApplicationDto mapToDto(Application app) {
        String studentName = studentRepository.findById(app.getStudentId())
                .map(Student::getName).orElse("Student");

        String driveId = app.getDriveId();
        if (driveId == null || driveId.isBlank()) {
            var routineOpt = dailyRoutineRepository.findByStudentIdAndApplyDate(app.getStudentId(), app.getApplyDate());
            if (routineOpt.isPresent()) {
                driveId = routineOpt.get().getDriveId();
            }
        }

        String jobTitle = "Campus Placement Opening";
        String companyName = "Corporate Recruiter";

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

        ApplicationDto dto = new ApplicationDto(
                app.getApplicationId(),
                app.getStudentId(),
                studentName,
                driveId != null ? driveId : "",
                jobTitle,
                companyName,
                app.getApplyDate(),
                app.getStatus()
        );

        // Retrieve real Oracle interview records and sort in logical chronological round order (1 -> 2 -> 3)
        List<Interview> rawInterviews = interviewRepository.findByApplicationId(app.getApplicationId());
        List<InterviewDto> interviewDtos = rawInterviews.stream()
                .map(i -> {
                    Integer roundNo = roundRepository.findById(i.getInterviewerName())
                            .map(InterviewerRound::getInterviewRoundNo).orElse(1);
                    return new InterviewDto(
                            i.getApplicationId(),
                            i.getInterviewerName(),
                            roundNo,
                            i.getOa(),
                            i.getGd(),
                            i.getHr(),
                            i.getResult(),
                            i.getOnline(),
                            i.getOffline()
                    );
                })
                .sorted(Comparator.comparing(InterviewDto::getRoundNo, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());

        dto.setInterviews(interviewDtos);

        BigDecimal oaScore = null;
        boolean oaCompleted = false;

        BigDecimal techScore = null;
        boolean techCompleted = false;

        BigDecimal hrScore = null;
        boolean hrCompleted = false;

        for (InterviewDto iv : interviewDtos) {
            int rNo = iv.getRoundNo() != null ? iv.getRoundNo() : 1;
            boolean passed = "PASSED".equalsIgnoreCase(iv.getResult()) || "CLEARED".equalsIgnoreCase(iv.getResult());

            if (rNo == 1) {
                oaScore = iv.getOa() != null ? iv.getOa() : iv.getScore();
                oaCompleted = passed && oaScore != null;
            } else if (rNo == 2) {
                techScore = iv.getGd() != null ? iv.getGd() : iv.getScore();
                techCompleted = passed && techScore != null;
            } else if (rNo == 3) {
                hrScore = iv.getHr() != null ? iv.getHr() : iv.getScore();
                hrCompleted = passed && hrScore != null;
            }
        }

        boolean allRoundsCompleted = oaCompleted && techCompleted && hrCompleted;

        // Self-heal: If an application is currently marked SELECTED but has not completed all 3 rounds and has no offer,
        // revert it back to INTERVIEWING so candidate is not prematurely marked SELECTED.
        if ("SELECTED".equalsIgnoreCase(app.getStatus()) && !rawInterviews.isEmpty() && !allRoundsCompleted) {
            boolean hasOffer = offerRepository.findByApplicationId(app.getApplicationId()).isPresent();
            if (!hasOffer) {
                app.setStatus("INTERVIEWING");
                applicationRepository.save(app);
                dto.setStatus("INTERVIEWING");
            }
        }

        // Authoritative Overall Score Rule:
        // Calculate overall score using ONLY evaluated/completed rounds with non-null scores.
        // DO NOT treat pending/future rounds as zero.
        BigDecimal totalScore = BigDecimal.ZERO;
        int completedRounds = 0;

        if (oaScore != null) {
            totalScore = totalScore.add(oaScore);
            completedRounds++;
        }
        if (techScore != null) {
            totalScore = totalScore.add(techScore);
            completedRounds++;
        }
        if (hrScore != null) {
            totalScore = totalScore.add(hrScore);
            completedRounds++;
        }

        BigDecimal overallScore = null;
        if (completedRounds > 0) {
            overallScore = totalScore.divide(BigDecimal.valueOf(completedRounds), 2, RoundingMode.HALF_UP);
        }

        dto.setScores(new ApplicationScoresDto(oaScore, techScore, hrScore, overallScore, completedRounds));
        dto.setScoreCompletion(new ScoreCompletionDto(oaCompleted, techCompleted, hrCompleted, allRoundsCompleted));

        return dto;
    }

}
