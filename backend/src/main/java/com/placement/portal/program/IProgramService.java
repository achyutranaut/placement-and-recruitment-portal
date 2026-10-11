package com.placement.portal.program;

import java.time.LocalDate;
import java.util.List;

public interface IProgramService {
    List<ProgramDto> getAllPrograms();
    ProgramDto getProgramById(String programId);
    void registerStudent(String studentId, String programId, LocalDate regDate);
    List<ProgramDto> getStudentRegisteredPrograms(String studentId);
    List<BatchDto> getAllBatches();
    List<BatchDto> getBatchesByProgram(String programId);
    List<AssessmentDto> getAllAssessments();
    List<AssessmentDto> getAssessmentsByBatch(String batchNo);
}
