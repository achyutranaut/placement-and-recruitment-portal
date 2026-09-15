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
import java.util.ArrayList;
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

    public boolean isApplicationEligibleForSelection(String applicationId) {
        return evaluateSelectionEligibility(applicationId).isEligible();
    }

    public SelectionEligibility evaluateSelectionEligibility(String applicationId) {
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found: " + applicationId));
        return evaluateSelectionEligibility(app);
    }

    public SelectionEligibility evaluateSelectionEligibility(Application app) {
        if (app == null) {
            return SelectionEligibility.notReady("Application is null", 0, 0, 0);
        }

        String driveId = app.getDriveId();
        if (driveId == null || driveId.isBlank()) {
            var routineOpt = dailyRoutineRepository.findByStudentIdAndApplyDate(app.getStudentId(), app.getApplyDate());
            if (routineOpt.isPresent()) {
                driveId = routineOpt.get().getDriveId();
            }
        }

        if (driveId == null || driveId.isBlank()) {
            return SelectionEligibility.notReady("NOT_READY: Application is not linked to any placement drive", 0, 0, 0);
        }

        var driveOpt = driveRepository.findById(driveId);
        if (driveOpt.isEmpty()) {
            return SelectionEligibility.notReady("NOT_READY: Placement drive " + driveId + " not found", 0, 0, 0);
        }

        PlacementDrive drive = driveOpt.get();
        List<String> configuredRounds = parseConfiguredRounds(drive.getSelectionProcess());
        int totalRequired = configuredRounds.size();
        if (totalRequired == 0) totalRequired = 3;

        List<Interview> rawInterviews = interviewRepository.findByApplicationId(app.getApplicationId());

        int completedRounds = 0;
        int passedRounds = 0;

        for (int r = 1; r <= totalRequired; r++) {
            String roundTitle = (r <= configuredRounds.size()) ? configuredRounds.get(r - 1) : ("Round " + r);

            // Find interview matching round number r
            final int currentRoundNo = r;
            Interview matchingIv = null;

            for (Interview iv : rawInterviews) {
                Integer ivRoundNo = roundRepository.findById(iv.getInterviewerName())
                        .map(InterviewerRound::getInterviewRoundNo).orElse(null);
                if (ivRoundNo != null && ivRoundNo == currentRoundNo) {
                    matchingIv = iv;
                    break;
                }
            }

            // Fallback for single-row interview structure
            if (matchingIv == null) {
                for (Interview iv : rawInterviews) {
                    if ((currentRoundNo == 1 && iv.getOa() != null) ||
                        (currentRoundNo == 2 && iv.getGd() != null) ||
                        (currentRoundNo == 3 && iv.getHr() != null)) {
                        matchingIv = iv;
                        break;
                    }
                }
            }

            if (matchingIv == null) {
                return SelectionEligibility.notReady("NOT_READY: " + roundTitle + " is pending / not scheduled", totalRequired, completedRounds, passedRounds);
            }

            String result = matchingIv.getResult() != null ? matchingIv.getResult().trim().toUpperCase() : "PENDING";
            if ("PENDING".equals(result)) {
                return SelectionEligibility.notReady("NOT_READY: " + roundTitle + " is still pending", totalRequired, completedRounds, passedRounds);
            }

            if ("REJECTED".equals(result) || "FAILED".equals(result)) {
                return SelectionEligibility.notReady("NOT_READY: " + roundTitle + " was failed/rejected", totalRequired, completedRounds, passedRounds);
            }

            if (!"CLEARED".equals(result) && !"PASSED".equals(result)) {
                return SelectionEligibility.notReady("NOT_READY: " + roundTitle + " has invalid status: " + result, totalRequired, completedRounds, passedRounds);
            }

            // Check that required score exists
            BigDecimal score = getScoreForRound(matchingIv, currentRoundNo);
            if (score == null) {
                return SelectionEligibility.notReady("NOT_READY: " + roundTitle + " score is missing", totalRequired, completedRounds, passedRounds);
            }

            completedRounds++;
            passedRounds++;
        }

        return SelectionEligibility.ready(totalRequired);
    }

    private BigDecimal getScoreForRound(Interview iv, int roundNo) {
        if (roundNo == 1) return iv.getOa() != null ? iv.getOa() : (iv.getGd() != null ? iv.getGd() : iv.getHr());
        if (roundNo == 2) return iv.getGd() != null ? iv.getGd() : (iv.getOa() != null ? iv.getOa() : iv.getHr());
        if (roundNo == 3) return iv.getHr() != null ? iv.getHr() : (iv.getGd() != null ? iv.getGd() : iv.getOa());
        return iv.getOa() != null ? iv.getOa() : (iv.getGd() != null ? iv.getGd() : iv.getHr());
    }

    public static List<String> parseConfiguredRounds(String selectionProcess) {
        List<String> titles = new ArrayList<>();
        if (selectionProcess == null || selectionProcess.isBlank()) {
            titles.add("Round 1: Online Assessment (OA)");
            titles.add("Round 2: Technical Interview");
            titles.add("Round 3: HR Interview");
            return titles;
        }

        String[] parts = selectionProcess.split("\\|");
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                titles.add(trimmed);
            }
        }
        if (titles.isEmpty()) {
            titles.add("Round 1: Online Assessment");
        }
        return titles;
    }

    @Transactional
    public ApplicationDto updateStatus(String applicationId, String newStatus, String changedBy) {
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found: " + applicationId));

        String oldStatus = app.getStatus();
        validateStatusTransition(oldStatus, newStatus);

        if ("SELECTED".equalsIgnoreCase(newStatus)) {
            SelectionEligibility eligibility = evaluateSelectionEligibility(app);
            if (!eligibility.isEligible()) {
                throw new IllegalStateException("Integrity Violation: Cannot transition candidate to SELECTED: " + eligibility.getReason());
            }
        }

        app.setStatus(newStatus.toUpperCase());
        Application saved = applicationRepository.save(app);

        // Record audit entry in real APPLICATION_AUDIT table
        auditRepository.save(new ApplicationAudit(applicationId, oldStatus, newStatus.toUpperCase(), changedBy != null ? changedBy : "RECRUITER"));

        return mapToDto(saved);
    }

    private void validateStatusTransition(String oldStatus, String newStatus) {
        if (oldStatus == null || newStatus == null) return;
        if (oldStatus.equalsIgnoreCase(newStatus)) return;

        if ("ACCEPTED".equalsIgnoreCase(oldStatus) || "DECLINED".equalsIgnoreCase(oldStatus)) {
            throw new IllegalStateException("Application is in finalized state '" + oldStatus + "' and cannot be updated to '" + newStatus + "'");
        }

        if ("REJECTED".equalsIgnoreCase(newStatus)) {
            return; // Rejection allowed from any non-finalized state
        }

        boolean valid = switch (oldStatus.toUpperCase()) {
            case "APPLIED" -> newStatus.equalsIgnoreCase("SHORTLISTED");
            case "SHORTLISTED" -> newStatus.equalsIgnoreCase("INTERVIEWING");
            case "INTERVIEWING" -> newStatus.equalsIgnoreCase("SELECTED");
            case "SELECTED" -> newStatus.equalsIgnoreCase("OFFERED");
            case "OFFERED" -> newStatus.equalsIgnoreCase("ACCEPTED") || newStatus.equalsIgnoreCase("DECLINED");
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

        SelectionEligibility eligibility = evaluateSelectionEligibility(app);

        // Self-heal: If an application is currently marked SELECTED but has not completed all mandatory rounds,
        // revert it back to INTERVIEWING so candidate is not prematurely marked SELECTED.
        if ("SELECTED".equalsIgnoreCase(app.getStatus()) && !eligibility.isEligible()) {
            app.setStatus("INTERVIEWING");
            applicationRepository.save(app);
            dto.setStatus("INTERVIEWING");
            auditRepository.save(new ApplicationAudit(app.getApplicationId(), "SELECTED", "INTERVIEWING", "AUTO_REVERT_INCOMPLETE_ROUNDS"));
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
        dto.setScoreCompletion(new ScoreCompletionDto(
                oaCompleted,
                techCompleted,
                hrCompleted,
                eligibility.isEligible(),
                eligibility.isEligible(),
                eligibility.getReason(),
                eligibility.getRequiredRounds(),
                eligibility.getCompletedRounds(),
                eligibility.getPassedRounds()
        ));

        return dto;
    }

}
