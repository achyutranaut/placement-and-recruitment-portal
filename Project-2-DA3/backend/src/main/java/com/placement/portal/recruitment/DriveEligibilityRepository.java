package com.placement.portal.recruitment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DriveEligibilityRepository extends JpaRepository<DriveEligibility, DriveEligibility.EligibilityId> {
    List<DriveEligibility> findByDriveId(String driveId);
    List<DriveEligibility> findByProgramId(String programId);
    boolean existsByDriveIdAndProgramId(String driveId, String programId);
}
