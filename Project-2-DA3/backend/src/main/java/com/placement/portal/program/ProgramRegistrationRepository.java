package com.placement.portal.program;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProgramRegistrationRepository extends JpaRepository<ProgramRegistration, ProgramRegistration.RegistrationId> {
    List<ProgramRegistration> findByStudentId(String studentId);
    List<ProgramRegistration> findByProgramId(String programId);
    boolean existsByStudentIdAndProgramId(String studentId, String programId);
}
