package com.placement.portal.neo4j.service;

import com.placement.portal.application.IApplicationService;
import com.placement.portal.interview.IInterviewService;
import com.placement.portal.interview.InterviewDto;
import com.placement.portal.interview.InterviewResultDto;
import com.placement.portal.interview.InterviewerRound;
import com.placement.portal.interview.ScheduleInterviewDto;
import com.placement.portal.neo4j.model.ApplicationNode;
import com.placement.portal.neo4j.model.AuditEventNode;
import com.placement.portal.neo4j.model.InterviewNode;
import com.placement.portal.neo4j.model.InterviewerNode;
import com.placement.portal.neo4j.repository.Neo4jApplicationRepository;
import com.placement.portal.neo4j.repository.Neo4jAuditEventRepository;
import com.placement.portal.neo4j.repository.Neo4jInterviewRepository;
import com.placement.portal.neo4j.repository.Neo4jInterviewerRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Profile("neo4j")
@Primary
public class Neo4jInterviewService implements IInterviewService {

    private final Neo4jInterviewRepository interviewRepository;
    private final Neo4jInterviewerRepository interviewerRepository;
    private final Neo4jApplicationRepository applicationRepository;
    private final Neo4jAuditEventRepository auditRepository;
    private final IApplicationService applicationService;

    public Neo4jInterviewService(
            Neo4jInterviewRepository interviewRepository,
            Neo4jInterviewerRepository interviewerRepository,
            Neo4jApplicationRepository applicationRepository,
            Neo4jAuditEventRepository auditRepository,
            IApplicationService applicationService
    ) {
        this.interviewRepository = interviewRepository;
        this.interviewerRepository = interviewerRepository;
        this.applicationRepository = applicationRepository;
        this.auditRepository = auditRepository;
        this.applicationService = applicationService;
    }

    /**
     * Ported PL/SQL Procedure: SCHEDULE_INTERVIEW
     * Enforces round lookup, creates InterviewNode, links (Application)-[:HAS_INTERVIEW]->(Interview)-[:CONDUCTED_BY]->(Interviewer)
     * and transitions application status to INTERVIEWING.
     */
    @Override
    @Transactional
    public void scheduleInterview(ScheduleInterviewDto dto) {
        ApplicationNode app = applicationRepository.findByApplicationId(dto.getApplicationId())
                .orElseThrow(() -> new IllegalArgumentException("Application not found: " + dto.getApplicationId()));

        Integer roundNo = interviewerRepository.findByInterviewerName(dto.getInterviewerName())
                .map(InterviewerNode::getInterviewRoundNo).orElse(1);

        InterviewNode interview = new InterviewNode(
                dto.getApplicationId(),
                dto.getInterviewerName(),
                roundNo,
                null,
                null,
                null,
                "PENDING",
                dto.getOnline() != null ? dto.getOnline() : "Y",
                dto.getOffline() != null ? dto.getOffline() : "N"
        );

        interviewerRepository.findByInterviewerName(dto.getInterviewerName()).ifPresent(interview::setInterviewer);
        interviewRepository.save(interview);

        String oldStatus = app.getStatus();
        if (!"INTERVIEWING".equalsIgnoreCase(oldStatus)) {
            app.setStatus("INTERVIEWING");
            applicationRepository.save(app);

            AuditEventNode audit = new AuditEventNode(
                    "AUD_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                    app.getApplicationId(),
                    oldStatus,
                    "INTERVIEWING",
                    "INTERVIEWER: " + dto.getInterviewerName()
            );
            auditRepository.save(audit);
        }
    }

    @Override
    public List<InterviewDto> getInterviewsByApplication(String applicationId) {
        return interviewRepository.findByApplicationId(applicationId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<InterviewDto> getInterviewsByStudent(String studentId) {
        List<ApplicationNode> apps = applicationRepository.findByStudentId(studentId);
        return apps.stream()
                .flatMap(a -> interviewRepository.findByApplicationId(a.getApplicationId()).stream())
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<InterviewDto> getAllInterviews() {
        return interviewRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<InterviewerRound> getAllInterviewerRounds() {
        return interviewerRepository.findAll().stream()
                .map(i -> new InterviewerRound(i.getInterviewerName(), i.getInterviewRoundNo()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public InterviewDto recordInterviewResult(String applicationId, String interviewerName, InterviewResultDto resultDto) {
        String interviewId = applicationId + "_" + interviewerName;
        InterviewNode interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new IllegalArgumentException("Interview record not found for: " + applicationId + ", " + interviewerName));

        if (resultDto.getOa() != null) interview.setOa(resultDto.getOa());
        if (resultDto.getGd() != null) interview.setGd(resultDto.getGd());
        if (resultDto.getHr() != null) interview.setHr(resultDto.getHr());

        String rawResult = resultDto.getResult() != null ? resultDto.getResult().trim().toUpperCase() : "CLEARED";
        String normalizedResult;
        if ("PASSED".equals(rawResult) || "CLEARED".equals(rawResult)) {
            normalizedResult = "CLEARED";
        } else if ("FAILED".equals(rawResult) || "REJECTED".equals(rawResult)) {
            normalizedResult = "REJECTED";
        } else if ("PENDING".equals(rawResult)) {
            normalizedResult = "PENDING";
        } else {
            throw new IllegalArgumentException("Invalid interview result status: '" + rawResult + "'. Allowed values are CLEARED, REJECTED, or PENDING.");
        }

        interview.setResult(normalizedResult);
        InterviewNode saved = interviewRepository.save(interview);

        applicationRepository.findByApplicationId(applicationId).ifPresent(app -> {
            String oldStatus = app.getStatus();
            String newStatus = oldStatus;
            if ("REJECTED".equals(normalizedResult)) {
                newStatus = "REJECTED";
            } else if ("CLEARED".equals(normalizedResult)) {
                if (applicationService.isApplicationEligibleForSelection(applicationId)) {
                    newStatus = "SELECTED";
                } else {
                    newStatus = "INTERVIEWING";
                }
            }

            if (!oldStatus.equalsIgnoreCase(newStatus)) {
                app.setStatus(newStatus);
                applicationRepository.save(app);

                AuditEventNode audit = new AuditEventNode(
                        "AUD_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                        applicationId,
                        oldStatus,
                        newStatus,
                        "INTERVIEWER: " + interviewerName + " (Result: " + normalizedResult + ")"
                );
                auditRepository.save(audit);
            }
        });

        return toDto(saved);
    }

    @Override
    public boolean areAllThreeRoundsCleared(String applicationId) {
        return applicationService.isApplicationEligibleForSelection(applicationId);
    }

    private InterviewDto toDto(InterviewNode i) {
        return new InterviewDto(
                i.getApplicationId(),
                i.getInterviewerName(),
                i.getRoundNo() != null ? i.getRoundNo() : 1,
                i.getOa(),
                i.getGd(),
                i.getHr(),
                i.getResult(),
                i.getOnline(),
                i.getOffline()
        );
    }
}
