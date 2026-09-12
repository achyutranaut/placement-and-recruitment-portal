package com.placement.portal.interview;

import com.placement.portal.application.Application;
import com.placement.portal.application.ApplicationAudit;
import com.placement.portal.application.ApplicationAuditRepository;
import com.placement.portal.application.ApplicationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class InterviewService {

    private final InterviewRepository interviewRepository;
    private final InterviewerRoundRepository roundRepository;
    private final InterviewJdbcDao interviewJdbcDao;
    private final ApplicationRepository applicationRepository;
    private final ApplicationAuditRepository auditRepository;

    public InterviewService(
            InterviewRepository interviewRepository,
            InterviewerRoundRepository roundRepository,
            InterviewJdbcDao interviewJdbcDao,
            ApplicationRepository applicationRepository,
            ApplicationAuditRepository auditRepository
    ) {
        this.interviewRepository = interviewRepository;
        this.roundRepository = roundRepository;
        this.interviewJdbcDao = interviewJdbcDao;
        this.applicationRepository = applicationRepository;
        this.auditRepository = auditRepository;
    }

    @Transactional
    public void scheduleInterview(ScheduleInterviewDto dto) {
        interviewJdbcDao.scheduleInterview(
                dto.getApplicationId(),
                dto.getInterviewerName(),
                dto.getOnline(),
                dto.getOffline()
        );
    }

    public List<InterviewDto> getInterviewsByApplication(String applicationId) {
        return interviewRepository.findByApplicationId(applicationId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<InterviewDto> getInterviewsByStudent(String studentId) {
        List<Application> apps = applicationRepository.findByStudentId(studentId);
        if (apps.isEmpty()) {
            return List.of();
        }
        List<String> appIds = apps.stream().map(Application::getApplicationId).toList();
        return interviewRepository.findByApplicationIdIn(appIds).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<InterviewDto> getAllInterviews() {
        return interviewRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<InterviewerRound> getAllInterviewerRounds() {
        return roundRepository.findAll();
    }

    @Transactional
    public InterviewDto recordInterviewResult(String applicationId, String interviewerName, InterviewResultDto resultDto) {
        Interview.InterviewId id = new Interview.InterviewId(applicationId, interviewerName);
        Interview interview = interviewRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Interview record not found for: " + applicationId + ", " + interviewerName));

        if (resultDto.getOa() != null) interview.setOa(resultDto.getOa());
        if (resultDto.getGd() != null) interview.setGd(resultDto.getGd());
        if (resultDto.getHr() != null) interview.setHr(resultDto.getHr());
        // Normalize result status to match DB constraint CHK_INTERVIEW_RESULT ('PENDING', 'CLEARED', 'REJECTED')
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

        Interview saved = interviewRepository.save(interview);

        // State Machine Transition based on interview outcome:
        // Candidate must complete & clear all 3 rounds (OA, GD/Tech, HR) to be SELECTED.
        // Recruiter can reject candidate after ANY round, transitioning status to REJECTED.
        applicationRepository.findById(applicationId).ifPresent(app -> {
            String oldStatus = app.getStatus();
            String newStatus = oldStatus;
            if ("REJECTED".equals(normalizedResult)) {
                newStatus = "REJECTED";
            } else if ("CLEARED".equals(normalizedResult)) {
                if (areAllThreeRoundsCleared(applicationId)) {
                    newStatus = "SELECTED";
                } else {
                    newStatus = "INTERVIEWING";
                }
            }

            if (!oldStatus.equalsIgnoreCase(newStatus)) {
                app.setStatus(newStatus);
                applicationRepository.save(app);
                auditRepository.save(new ApplicationAudit(applicationId, oldStatus, newStatus, "INTERVIEWER: " + interviewerName + " (Result: " + normalizedResult + ")"));
            }
        });

        return mapToDto(saved);
    }

    public boolean areAllThreeRoundsCleared(String applicationId) {
        List<Interview> allInterviews = interviewRepository.findByApplicationId(applicationId);
        if (allInterviews.isEmpty()) {
            return false;
        }

        boolean round1Cleared = false;
        boolean round2Cleared = false;
        boolean round3Cleared = false;

        for (Interview iv : allInterviews) {
            boolean isCleared = "CLEARED".equalsIgnoreCase(iv.getResult()) || "PASSED".equalsIgnoreCase(iv.getResult());
            if (!isCleared) {
                continue;
            }

            Integer roundNo = roundRepository.findById(iv.getInterviewerName())
                    .map(InterviewerRound::getInterviewRoundNo).orElse(null);

            if (roundNo != null) {
                if (roundNo == 1 && (iv.getOa() != null || iv.getGd() != null || iv.getHr() != null)) {
                    round1Cleared = true;
                } else if (roundNo == 2 && (iv.getGd() != null || iv.getOa() != null || iv.getHr() != null)) {
                    round2Cleared = true;
                } else if (roundNo == 3 && (iv.getHr() != null || iv.getOa() != null || iv.getGd() != null)) {
                    round3Cleared = true;
                }
            }

            // Also support single-row interview where all 3 scores are populated
            if (iv.getOa() != null && iv.getGd() != null && iv.getHr() != null) {
                round1Cleared = true;
                round2Cleared = true;
                round3Cleared = true;
            }
        }

        return round1Cleared && round2Cleared && round3Cleared;
    }

    private InterviewDto mapToDto(Interview i) {
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
    }
}
