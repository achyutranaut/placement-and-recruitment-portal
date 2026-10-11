package com.placement.portal.recruitment;

import java.util.List;
import java.util.Map;

public interface IPlacementDriveService {
    DriveEligibilityDto evaluateEligibility(String driveId, String studentId);
    Map<String, DriveEligibilityDto> evaluateAllEligibilityForStudent(String studentId);
    List<PlacementDriveDto> getAllDrives();
    PlacementDriveDto getDriveById(String driveId);
    PlacementDriveDto createDrive(PlacementDriveDto dto);
    PlacementDriveDto updateDrive(String driveId, PlacementDriveDto dto);
}
