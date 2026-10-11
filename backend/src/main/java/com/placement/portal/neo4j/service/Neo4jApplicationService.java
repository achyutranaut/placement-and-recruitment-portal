package com.placement.portal.neo4j.service;

import com.placement.portal.application.ApplicationDto;
import com.placement.portal.application.ApplicationScoresDto;
import com.placement.portal.application.IApplicationService;
import com.placement.portal.application.ScoreCompletionDto;
import com.placement.portal.application.SelectionEligibility;
import com.placement.portal.interview.InterviewDto;
import com.placement.portal.neo4j.model.*;
import com.placement.portal.neo4j.repository.*;
import com.placement.portal.recruitment.IPlacementDriveService;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Profile("neo4j")
@Primary
public class Neo4jApplicationService implements IApplicationService {

    private final Neo4jApplicationRepository applicationRepository;
    private final Neo4jStudentRepository studentRepository;
    private final Neo4jDriveRepository driveRepository;
    private final Neo4jInterviewRepository interviewRepository;
    private final Neo4jAuditEventRepository auditRepository;
    private final IPlacementDriveService placementDriveService;
    private final org.springframework.data.neo4j.core.Neo4jClient neo4jClient;

    public Neo4jApplicationService(
            Neo4jApplicationRepository applicationRepository,
            Neo4jStudentRepository studentRepository,
            Neo4jDriveRepository driveRepository,
            Neo4jInterviewRepository interviewRepository,
            Neo4jAuditEventRepository auditRepository,
            IPlacementDriveService placementDriveService,
            org.springframework.data.neo4j.core.Neo4jClient neo4jClient
    ) {
        this.applicationRepository = applicationRepository;
        this.studentRepository = studentRepository;
        this.driveRepository = driveRepository;
        this.interviewRepository = interviewRepository;
        this.auditRepository = auditRepository;
        this.placementDriveService = placementDriveService;
        this.neo4jClient = neo4jClient;
    }

    /**
     * Ported PL/SQL Procedure: APPLY_FOR_DRIVE
     * Business rules enforced transactionally:
     * 1. 1 application per student per calendar day (using APPLIED_ON_DATE relationship check).
     * 2. CGPA threshold via placementDriveService.evaluateEligibility.
     * 3. Academic program eligibility via (Drive)-[:ELIGIBLE_FOR]->(Program).
     * 4. Application deadline verification.
     * 5. Atomically creates ApplicationNode, edges (:Student)-[:SUBMITTED_APPLICATION]->(:Application)-[:FOR_DRIVE]->(:Drive)
     *    and (:Student)-[:APPLIED_ON_DATE]->(:Drive)
     */
    @Override
    @Transactional
    public ApplicationDto applyForDrive(String studentId, String driveId, LocalDate applyDate) {
        StudentNode student = studentRepository.findByStudentId(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found: " + studentId));
        DriveNode drive = driveRepository.findByDriveId(driveId)
                .orElseThrow(() -> new IllegalArgumentException("Drive not found: " + driveId));

        LocalDate date = applyDate != null ? applyDate : LocalDate.now();

        // 1-per-day calendar limit rule
        long dailyCount = applicationRepository.countStudentApplicationsOnDate(studentId, date);
        if (dailyCount > 0) {
            throw new IllegalArgumentException("Daily limit reached: A student may apply to at most one placement drive per calendar day.");
        }

        // Check if student already applied for this drive
        boolean alreadyApplied = applicationRepository.findByStudentId(studentId).stream()
                .anyMatch(a -> driveId.equalsIgnoreCase(a.getDriveId()));
        if (alreadyApplied) {
            throw new IllegalArgumentException("You have already submitted an application for placement drive " + driveId + ".");
        }

        // Eligibility rules check (CGPA, Program, Deadline)
        var eligibility = placementDriveService.evaluateEligibility(driveId, studentId);
        if (!eligibility.isEligible()) {
            throw new IllegalArgumentException(eligibility.getMessage());
        }

        long count = applicationRepository.count();
        String appId = "APP" + String.format("%03d", count + 1);
        while (applicationRepository.findByApplicationId(appId).isPresent()) {
            count++;
            appId = "APP" + String.format("%03d", count + 1);
        }

        ApplicationNode app = new ApplicationNode(appId, studentId, driveId, date, "APPLIED");

        // Audit journal initial write
        AuditEventNode audit = new AuditEventNode(
                "AUD_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                appId,
                null,
                "APPLIED",
                "STUDENT: " + studentId
        );
        app.getAuditEvents().add(audit);

        ApplicationNode saved = applicationRepository.save(app);
        auditRepository.save(audit);

        // Link in graph: (Student)-[:SUBMITTED_APPLICATION]->(Application)-[:FOR_DRIVE]->(Drive)
        neo4jClient.query("""
            MATCH (s:Student {studentId: $studentId}), (a:Application {applicationId: $appId}), (d:Drive {driveId: $driveId})
            MERGE (s)-[:SUBMITTED_APPLICATION]->(a)
            MERGE (a)-[:FOR_DRIVE]->(d)
        """)
        .bind(studentId).to("studentId")
        .bind(appId).to("appId")
        .bind(driveId).to("driveId")
        .run();

        return toDto(saved);
    }

    @Override
    public List<ApplicationDto> getAllApplications() {
        return applicationRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ApplicationDto> getApplicationsByStudent(String studentId) {
        return applicationRepository.findByStudentId(studentId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ApplicationDto> getApplicationsByDrive(String driveId) {
        return applicationRepository.findByDriveId(driveId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public ApplicationDto getApplicationById(String applicationId) {
        ApplicationNode app = applicationRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found: " + applicationId));
        return toDto(app);
    }

    @Override
    public boolean isApplicationEligibleForSelection(String applicationId) {
        return evaluateSelectionEligibility(applicationId).isEligible();
    }

    @Override
    public SelectionEligibility evaluateSelectionEligibility(String applicationId) {
        ApplicationNode app = applicationRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found: " + applicationId));
        return evaluateSelectionEligibility(app);
    }

    public SelectionEligibility evaluateSelectionEligibility(ApplicationNode app) {
        if (app == null) {
            return SelectionEligibility.notReady("Application is null", 0, 0, 0);
        }

        String driveId = app.getDriveId();
        if (driveId == null || driveId.isBlank()) {
            return SelectionEligibility.notReady("NOT_READY: Application is not linked to any placement drive", 0, 0, 0);
        }

        var driveOpt = driveRepository.findByDriveId(driveId);
        if (driveOpt.isEmpty()) {
            return SelectionEligibility.notReady("NOT_READY: Placement drive " + driveId + " not found", 0, 0, 0);
        }

        DriveNode drive = driveOpt.get();
        List<String> configuredRounds = parseConfiguredRounds(drive.getSelectionProcess());
        int totalRequired = configuredRounds.size();
        if (totalRequired == 0) totalRequired = 3;

        List<InterviewNode> rawInterviews = interviewRepository.findByApplicationId(app.getApplicationId());

        int completedRounds = 0;
        int passedRounds = 0;

        for (int r = 1; r <= totalRequired; r++) {
            String roundTitle = (r <= configuredRounds.size()) ? configuredRounds.get(r - 1) : ("Round " + r);
            final int currentRoundNo = r;
            InterviewNode matchingIv = null;

            for (InterviewNode iv : rawInterviews) {
                if (iv.getRoundNo() != null && iv.getRoundNo() == currentRoundNo) {
                    matchingIv = iv;
                    break;
                }
            }

            if (matchingIv == null) {
                for (InterviewNode iv : rawInterviews) {
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

            BigDecimal score = getScoreForRound(matchingIv, currentRoundNo);
            if (score == null) {
                return SelectionEligibility.notReady("NOT_READY: " + roundTitle + " score is missing", totalRequired, completedRounds, passedRounds);
            }

            completedRounds++;
            passedRounds++;
        }

        return SelectionEligibility.ready(totalRequired);
    }

    private BigDecimal getScoreForRound(InterviewNode iv, int roundNo) {
        if (roundNo == 1) return iv.getOa() != null ? iv.getOa() : (iv.getGd() != null ? iv.getGd() : iv.getHr());
        if (roundNo == 2) return iv.getGd() != null ? iv.getGd() : (iv.getOa() != null ? iv.getOa() : iv.getHr());
        if (roundNo == 3) return iv.getHr() != null ? iv.getHr() : (iv.getGd() != null ? iv.getGd() : iv.getOa());
        return iv.getOa() != null ? iv.getOa() : (iv.getGd() != null ? iv.getGd() : iv.getHr());
    }

    private List<String> parseConfiguredRounds(String selectionProcess) {
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

    @Override
    @Transactional
    public ApplicationDto updateStatus(String applicationId, String newStatus, String changedBy) {
        ApplicationNode app = applicationRepository.findByApplicationId(applicationId)
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
        ApplicationNode saved = applicationRepository.save(app);

        // Record audit journal write
        AuditEventNode audit = new AuditEventNode(
                "AUD_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                applicationId,
                oldStatus,
                newStatus.toUpperCase(),
                changedBy != null ? changedBy : "RECRUITER"
        );
        auditRepository.save(audit);

        return toDto(saved);
    }

    private void validateStatusTransition(String oldStatus, String newStatus) {
        if (oldStatus == null || newStatus == null) return;
        if (oldStatus.equalsIgnoreCase(newStatus)) return;

        if ("ACCEPTED".equalsIgnoreCase(oldStatus) || "DECLINED".equalsIgnoreCase(oldStatus)) {
            throw new IllegalStateException("Application is in finalized state '" + oldStatus + "' and cannot be updated to '" + newStatus + "'");
        }

        if ("REJECTED".equalsIgnoreCase(newStatus)) {
            return;
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

    private ApplicationDto toDto(ApplicationNode app) {
        String studentName = studentRepository.findByStudentId(app.getStudentId())
                .map(StudentNode::getName).orElse("Student");

        String driveId = app.getDriveId();
        String jobTitle = "Campus Placement Opening";
        String companyName = "Corporate Recruiter";

        if (driveId != null) {
            var driveOpt = driveRepository.findByDriveId(driveId);
            if (driveOpt.isPresent()) {
                DriveNode drv = driveOpt.get();
                jobTitle = drv.getJobTitle();
                companyName = drv.getCompanyName() != null ? drv.getCompanyName() : "Corporate Recruiter";
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

        List<InterviewNode> rawInterviews = interviewRepository.findByApplicationId(app.getApplicationId());
        List<InterviewDto> interviewDtos = rawInterviews.stream()
                .map(i -> new InterviewDto(
                        i.getApplicationId(),
                        i.getInterviewerName(),
                        i.getRoundNo() != null ? i.getRoundNo() : 1,
                        i.getOa(),
                        i.getGd(),
                        i.getHr(),
                        i.getResult(),
                        i.getOnline(),
                        i.getOffline()
                ))
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
