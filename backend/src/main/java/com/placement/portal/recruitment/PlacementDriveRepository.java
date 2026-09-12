package com.placement.portal.recruitment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlacementDriveRepository extends JpaRepository<PlacementDrive, String> {
    List<PlacementDrive> findByJobTitle(String jobTitle);
}
