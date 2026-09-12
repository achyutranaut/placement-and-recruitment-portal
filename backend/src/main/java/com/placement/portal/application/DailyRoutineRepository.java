package com.placement.portal.application;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyRoutineRepository extends JpaRepository<StudentDailyRoutineDrive, StudentDailyRoutineDrive.RoutineId> {
    Optional<StudentDailyRoutineDrive> findByStudentIdAndApplyDate(String studentId, LocalDate applyDate);
    List<StudentDailyRoutineDrive> findByDriveId(String driveId);
    List<StudentDailyRoutineDrive> findByStudentId(String studentId);
    long countByDriveId(String driveId);
}
