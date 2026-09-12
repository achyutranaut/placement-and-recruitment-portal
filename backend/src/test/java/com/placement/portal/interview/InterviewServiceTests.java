package com.placement.portal.interview;

import com.placement.portal.application.Application;
import com.placement.portal.application.ApplicationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("h2")
@Transactional
class InterviewServiceTests {

    @Autowired
    private InterviewService interviewService;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private InterviewRepository interviewRepository;

    @Test
    void testRecordInterviewResultClearedKeepsStatusInterviewingIfNotAllThreeRoundsCleared() {
        // APP002 has Round 1 cleared and Round 2 evaluated here, but Round 3 is not cleared yet
        InterviewResultDto dto = new InterviewResultDto(
                null,
                BigDecimal.valueOf(88),
                null,
                "CLEARED"
        );

        InterviewDto result = interviewService.recordInterviewResult("APP002", "Sangeeta Rao", dto);

        assertNotNull(result);
        assertEquals("CLEARED", result.getResult());

        Interview iv = interviewRepository.findById(new Interview.InterviewId("APP002", "Sangeeta Rao")).orElseThrow();
        assertEquals("CLEARED", iv.getResult());

        // Status must remain INTERVIEWING because Round 3 has not been completed
        Application app = applicationRepository.findById("APP002").orElseThrow();
        assertEquals("INTERVIEWING", app.getStatus(), "Candidate must remain INTERVIEWING until all 3 rounds are completed");
    }

    @Test
    void testRecordAllThreeRoundsClearedTransitionsToSelected() {
        // Schedule and clear Round 2 for APP002
        interviewService.recordInterviewResult("APP002", "Sangeeta Rao", new InterviewResultDto(
                null, BigDecimal.valueOf(88), null, "CLEARED"
        ));

        // Schedule Round 3 for APP002 with Arvind Swamy
        interviewRepository.save(new Interview("APP002", "Arvind Swamy", null, null, null, "PENDING", "Y", "N"));

        // Evaluate Round 3 as CLEARED
        interviewService.recordInterviewResult("APP002", "Arvind Swamy", new InterviewResultDto(
                null, null, BigDecimal.valueOf(92), "CLEARED"
        ));

        // Now all 3 rounds (Round 1 OA, Round 2 GD, Round 3 HR) are CLEARED
        Application app = applicationRepository.findById("APP002").orElseThrow();
        assertEquals("SELECTED", app.getStatus(), "Candidate must transition to SELECTED after clearing all 3 rounds");
    }

    @Test
    void testRecordInterviewResultPassedNormalizesToCleared() {
        // Recruiter frontend sending legacy 'PASSED' should normalize to 'CLEARED' to prevent ORA-02290
        InterviewResultDto dto = new InterviewResultDto(
                null,
                BigDecimal.valueOf(90),
                null,
                "PASSED"
        );

        InterviewDto result = interviewService.recordInterviewResult("APP002", "Sangeeta Rao", dto);

        assertNotNull(result);
        assertEquals("CLEARED", result.getResult());

        Interview iv = interviewRepository.findById(new Interview.InterviewId("APP002", "Sangeeta Rao")).orElseThrow();
        assertEquals("CLEARED", iv.getResult());

        Application app = applicationRepository.findById("APP002").orElseThrow();
        assertEquals("INTERVIEWING", app.getStatus());
    }

    @Test
    void testRecordInterviewResultRejected() {
        InterviewResultDto dto = new InterviewResultDto(
                BigDecimal.valueOf(40),
                null,
                null,
                "REJECTED"
        );

        InterviewDto result = interviewService.recordInterviewResult("APP002", "Sangeeta Rao", dto);

        assertNotNull(result);
        assertEquals("REJECTED", result.getResult());

        Interview iv = interviewRepository.findById(new Interview.InterviewId("APP002", "Sangeeta Rao")).orElseThrow();
        assertEquals("REJECTED", iv.getResult());

        Application app = applicationRepository.findById("APP002").orElseThrow();
        assertEquals("REJECTED", app.getStatus());
    }

    @Test
    void testRecordInterviewResultFailedNormalizesToRejected() {
        // Recruiter frontend sending 'FAILED' should normalize to 'REJECTED' to prevent ORA-02290
        InterviewResultDto dto = new InterviewResultDto(
                BigDecimal.valueOf(45),
                null,
                null,
                "FAILED"
        );

        InterviewDto result = interviewService.recordInterviewResult("APP002", "Sangeeta Rao", dto);

        assertNotNull(result);
        assertEquals("REJECTED", result.getResult());

        Interview iv = interviewRepository.findById(new Interview.InterviewId("APP002", "Sangeeta Rao")).orElseThrow();
        assertEquals("REJECTED", iv.getResult());

        Application app = applicationRepository.findById("APP002").orElseThrow();
        assertEquals("REJECTED", app.getStatus());
    }

    @Test
    void testRecordInterviewResultInvalidThrowsException() {
        InterviewResultDto dto = new InterviewResultDto(
                BigDecimal.valueOf(80),
                null,
                null,
                "INVALID_STATUS"
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                interviewService.recordInterviewResult("APP002", "Sangeeta Rao", dto));

        assertTrue(ex.getMessage().contains("Invalid interview result status"));
    }
}
