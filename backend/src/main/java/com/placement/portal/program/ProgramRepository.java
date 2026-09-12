package com.placement.portal.program;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProgramRepository extends JpaRepository<Program, String> {
    List<Program> findByIsActive(String isActive);
    List<Program> findByIsActiveOrderByProgramLevelAscProgramNameAsc(String isActive);
}
