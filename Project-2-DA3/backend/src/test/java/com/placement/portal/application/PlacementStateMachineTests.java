package com.placement.portal.application;

import com.placement.portal.interview.Interview;
import com.placement.portal.interview.InterviewDto;
import com.placement.portal.interview.InterviewRepository;
import com.placement.portal.interview.InterviewResultDto;
import com.placement.portal.interview.InterviewService;
import com.placement.portal.interview.InterviewerRound;
import com.placement.portal.interview.InterviewerRoundRepository;
import com.placement.portal.offer.IssueOfferDto;
import com.placement.portal.offer.OfferDto;
import com.placement.portal.offer.OfferLetter;
import com.placement.portal.offer.OfferRepository;
import com.placement.portal.offer.OfferService;
import com.placement.portal.recruitment.PlacementDrive;
import com.placement.portal.recruitment.PlacementDriveRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Authoritative Placement State Machine Test Matrix.
 * Verifies strict business logic invariants:
 * - APPLIED -> SHORTLISTED -> INTERVIEWING -> SELECTED -> OFFERED -> ACCEPTED
 * - Mandatory round completion validation per configured placement drive
 * - Prevention of invalid state jumps
 */
@SpringBootTest
@ActiveProfiles("h2")
@Transactional
public class PlacementStateMachineTests {

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private InterviewService interviewService;

    @Autowired
    private OfferService offerService;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private InterviewRepository interviewRepository;

    @Autowired
    private InterviewerRoundRepository roundRepository;

    @Autowired
    private PlacementDriveRepository driveRepository;

    @Autowired
    private OfferRepository offerRepository;

    @BeforeEach
    void setUpRounds() {
        roundRepository.save(new InterviewerRound("Vikram Malhotra", 1));
        roundRepository.save(new InterviewerRound("Sangeeta Rao", 2));
        roundRepository.save(new InterviewerRound("Arvind Swamy", 3));
    }

    @Test
    @DisplayName("CASE 1: OA passed, GD pending, HR pending -> Status remains INTERVIEWING, cannot SELECT")
    void testCase1_OaPassed_GdPending_HrPending() {
        String appId = "APP_SM_CASE1";
        Application app = new Application(appId, "STU001", "DRV001", LocalDate.now(), "INTERVIEWING");
        applicationRepository.save(app);

        // Round 1 passed, Round 2 and Round 3 not completed
        interviewRepository.save(new Interview(appId, "Vikram Malhotra", BigDecimal.valueOf(94), null, null, "CLEARED", "Y", "N"));

        SelectionEligibility readiness = applicationService.evaluateSelectionEligibility(appId);
        assertFalse(readiness.isEligible(), "Candidate must not be eligible for selection when GD and HR are pending");
        assertTrue(readiness.getReason().contains("pending"), "Reason must report pending round: " + readiness.getReason());

        // Attempting to transition to SELECTED must throw IllegalStateException
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                applicationService.updateStatus(appId, "SELECTED", "RECRUITER")
        );
        assertTrue(ex.getMessage().contains("Cannot transition candidate to SELECTED"));

        Application reloaded = applicationRepository.findById(appId).orElseThrow();
        assertEquals("INTERVIEWING", reloaded.getStatus(), "Application status must remain INTERVIEWING");
    }

    @Test
    @DisplayName("CASE 2: OA passed, GD passed, HR pending -> Status remains INTERVIEWING, cannot SELECT")
    void testCase2_OaPassed_GdPassed_HrPending() {
        String appId = "APP_SM_CASE2";
        Application app = new Application(appId, "STU001", "DRV001", LocalDate.now(), "INTERVIEWING");
        applicationRepository.save(app);

        // Round 1 passed, Round 2 passed, Round 3 pending
        interviewRepository.save(new Interview(appId, "Vikram Malhotra", BigDecimal.valueOf(94), null, null, "CLEARED", "Y", "N"));
        interviewRepository.save(new Interview(appId, "Sangeeta Rao", null, BigDecimal.valueOf(88), null, "CLEARED", "Y", "N"));
        interviewRepository.save(new Interview(appId, "Arvind Swamy", null, null, null, "PENDING", "Y", "N"));

        SelectionEligibility readiness = applicationService.evaluateSelectionEligibility(appId);
        assertFalse(readiness.isEligible(), "Candidate must not be eligible for selection when HR is still pending");
        assertTrue(readiness.getReason().contains("pending"), "Reason must report Round 3 is still pending: " + readiness.getReason());

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                applicationService.updateStatus(appId, "SELECTED", "RECRUITER")
        );
        assertTrue(ex.getMessage().contains("Cannot transition candidate to SELECTED"));
    }

    @Test
    @DisplayName("CASE 3: OA passed, GD passed, HR passed -> Candidate eligible for SELECTED and transitions successfully")
    void testCase3_AllThreeRoundsPassed_EligibleForSelected() {
        String appId = "APP_SM_CASE3";
        Application app = new Application(appId, "STU001", "DRV001", LocalDate.now(), "INTERVIEWING");
        applicationRepository.save(app);

        // Clear Round 1 and Round 2
        interviewRepository.save(new Interview(appId, "Vikram Malhotra", BigDecimal.valueOf(94), null, null, "CLEARED", "Y", "N"));
        interviewRepository.save(new Interview(appId, "Sangeeta Rao", null, BigDecimal.valueOf(88), null, "CLEARED", "Y", "N"));

        // Evaluate Round 3 as CLEARED
        interviewRepository.save(new Interview(appId, "Arvind Swamy", null, null, null, "PENDING", "Y", "N"));
        InterviewDto evaluated = interviewService.recordInterviewResult(appId, "Arvind Swamy", new InterviewResultDto(
                null, null, BigDecimal.valueOf(90), "CLEARED"
        ));

        assertNotNull(evaluated);
        assertEquals("CLEARED", evaluated.getResult());

        // Verify that application automatically transitioned to SELECTED
        Application reloaded = applicationRepository.findById(appId).orElseThrow();
        assertEquals("SELECTED", reloaded.getStatus(), "Application must transition to SELECTED after clearing all mandatory rounds");

        SelectionEligibility readiness = applicationService.evaluateSelectionEligibility(appId);
        assertTrue(readiness.isEligible(), "Candidate must be eligible for selection");
        assertEquals("READY", readiness.getReason());
    }

    @Test
    @DisplayName("CASE 4: Any required round failed/rejected -> Cannot SELECT, transitions to REJECTED")
    void testCase4_AnyRequiredRoundFailed_TransitionsToRejected() {
        String appId = "APP_SM_CASE4";
        Application app = new Application(appId, "STU001", "DRV001", LocalDate.now(), "INTERVIEWING");
        applicationRepository.save(app);

        // Round 1 passed
        interviewRepository.save(new Interview(appId, "Vikram Malhotra", BigDecimal.valueOf(94), null, null, "CLEARED", "Y", "N"));

        // Round 2 evaluated as REJECTED
        interviewRepository.save(new Interview(appId, "Sangeeta Rao", null, null, null, "PENDING", "Y", "N"));
        interviewService.recordInterviewResult(appId, "Sangeeta Rao", new InterviewResultDto(
                null, BigDecimal.valueOf(40), null, "REJECTED"
        ));

        Application reloaded = applicationRepository.findById(appId).orElseThrow();
        assertEquals("REJECTED", reloaded.getStatus(), "Candidate rejected in Round 2 must transition to REJECTED");

        SelectionEligibility readiness = applicationService.evaluateSelectionEligibility(appId);
        assertFalse(readiness.isEligible(), "Rejected candidate cannot be eligible for selection");
    }

    @Test
    @DisplayName("CASE 5: Drive has only OA configured -> OA passed is immediately eligible for SELECTED")
    void testCase5_DriveHasOnlyOA_EligibleForSelected() {
        // Create custom drive with single-round selection process
        String singleRoundDriveId = "DRV_OA_ONLY";
        PlacementDrive singleRoundDrive = new PlacementDrive(
                singleRoundDriveId, "Junior Analyst", BigDecimal.valueOf(6.50),
                LocalDate.now(), LocalDate.now().plusDays(10), 5, BigDecimal.valueOf(8.00),
                "Junior Analyst Opening", "Bangalore", "CSE,ECE", "Round 1: Cognitive & Coding OA"
        );
        driveRepository.save(singleRoundDrive);

        String appId = "APP_SM_CASE5";
        Application app = new Application(appId, "STU001", singleRoundDriveId, LocalDate.now(), "INTERVIEWING");
        applicationRepository.save(app);

        // Clear Round 1
        interviewRepository.save(new Interview(appId, "Vikram Malhotra", BigDecimal.valueOf(95), null, null, "CLEARED", "Y", "N"));

        SelectionEligibility readiness = applicationService.evaluateSelectionEligibility(appId);
        assertTrue(readiness.isEligible(), "Candidate on OA-only drive must be eligible after clearing Round 1");
        assertEquals("READY", readiness.getReason());
        assertEquals(1, readiness.getRequiredRounds());

        // Update status to SELECTED succeeds
        ApplicationDto updated = applicationService.updateStatus(appId, "SELECTED", "RECRUITER");
        assertEquals("SELECTED", updated.getStatus());
    }

    @Test
    @DisplayName("CASE 6: SELECTED + valid selection -> OFFERED allowed")
    void testCase6_SelectedAndValidSelection_OfferedAllowed() {
        String appId = "APP_SM_CASE6";
        Application app = new Application(appId, "STU001", "DRV001", LocalDate.now(), "SELECTED");
        applicationRepository.save(app);

        // Populate and clear all 3 mandatory rounds
        interviewRepository.save(new Interview(appId, "Vikram Malhotra", BigDecimal.valueOf(92), null, null, "CLEARED", "Y", "N"));
        interviewRepository.save(new Interview(appId, "Sangeeta Rao", null, BigDecimal.valueOf(85), null, "CLEARED", "Y", "N"));
        interviewRepository.save(new Interview(appId, "Arvind Swamy", null, null, BigDecimal.valueOf(88), "CLEARED", "Y", "N"));

        IssueOfferDto offerDto = new IssueOfferDto(appId, null, LocalDate.now());
        OfferDto result = offerService.issueOffer(offerDto);

        assertNotNull(result);
        assertEquals("OFFERED", result.getStatus());
        assertEquals(0, BigDecimal.valueOf(44.00).compareTo(result.getCtcLpa()));

        Application reloaded = applicationRepository.findById(appId).orElseThrow();
        assertEquals("OFFERED", reloaded.getStatus(), "Application status must update to OFFERED");
    }

    @Test
    @DisplayName("CASE 7: OFFERED + valid offer -> ACCEPTED allowed")
    void testCase7_OfferedAndValidOffer_AcceptedAllowed() {
        String appId = "APP_SM_CASE7";
        Application app = new Application(appId, "STU001", "DRV001", LocalDate.now(), "OFFERED");
        applicationRepository.save(app);

        OfferLetter offer = new OfferLetter("OFF_TEST_7", appId, LocalDate.now(), BigDecimal.valueOf(44.00), "OFFERED");
        offerRepository.save(offer);

        OfferDto accepted = offerService.acceptOffer("OFF_TEST_7", "STU001");
        assertNotNull(accepted);
        assertEquals("ACCEPTED", accepted.getStatus());

        Application reloaded = applicationRepository.findById(appId).orElseThrow();
        assertEquals("ACCEPTED", reloaded.getStatus(), "Application must transition to ACCEPTED");
    }

    @Test
    @DisplayName("CASE 8: PENDING round -> direct transition to ACCEPTED is BLOCKED")
    void testCase8_PendingRound_DirectJumpToAccepted_Blocked() {
        String appId = "APP_SM_CASE8";
        Application app = new Application(appId, "STU001", "DRV001", LocalDate.now(), "INTERVIEWING");
        applicationRepository.save(app);

        // Only Round 1 is cleared
        interviewRepository.save(new Interview(appId, "Vikram Malhotra", BigDecimal.valueOf(94), null, null, "CLEARED", "Y", "N"));

        // Attempting direct jump to ACCEPTED
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                applicationService.updateStatus(appId, "ACCEPTED", "ATTACKER")
        );
        assertTrue(ex.getMessage().contains("Invalid status transition"));

        // Attempting direct jump to OFFERED
        IllegalStateException ex2 = assertThrows(IllegalStateException.class, () ->
                applicationService.updateStatus(appId, "OFFERED", "ATTACKER")
        );
        assertTrue(ex2.getMessage().contains("Invalid status transition"));
    }

    @Test
    @DisplayName("CASE 9: Direct offer issuance on INTERVIEWING application is BLOCKED by backend validation")
    void testCase9_DirectOfferIssuanceOnInterviewing_Blocked() {
        String appId = "APP_SM_CASE9";
        Application app = new Application(appId, "STU001", "DRV001", LocalDate.now(), "INTERVIEWING");
        applicationRepository.save(app);

        interviewRepository.save(new Interview(appId, "Vikram Malhotra", BigDecimal.valueOf(94), null, null, "CLEARED", "Y", "N"));

        IssueOfferDto dto = new IssueOfferDto(appId, BigDecimal.valueOf(44.00), LocalDate.now());

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                offerService.issueOffer(dto)
        );
        assertTrue(ex.getMessage().contains("Candidate must be in SELECTED state to receive an offer"));
    }
}
