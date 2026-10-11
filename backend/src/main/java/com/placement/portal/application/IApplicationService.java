package com.placement.portal.application;

import java.time.LocalDate;
import java.util.List;

public interface IApplicationService {
    ApplicationDto applyForDrive(String studentId, String driveId, LocalDate applyDate);
    List<ApplicationDto> getAllApplications();
    List<ApplicationDto> getApplicationsByStudent(String studentId);
    List<ApplicationDto> getApplicationsByDrive(String driveId);
    ApplicationDto getApplicationById(String applicationId);
    boolean isApplicationEligibleForSelection(String applicationId);
    SelectionEligibility evaluateSelectionEligibility(String applicationId);
    ApplicationDto updateStatus(String applicationId, String newStatus, String changedBy);
}
