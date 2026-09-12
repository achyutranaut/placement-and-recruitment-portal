package com.placement.portal.program;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProgramService {

    private final ProgramRepository programRepository;
    private final ProgramRegistrationRepository registrationRepository;
    private final BatchRepository batchRepository;
    private final AssessmentRepository assessmentRepository;
    private final ProgramJdbcDao programJdbcDao;

    public ProgramService(
            ProgramRepository programRepository,
            ProgramRegistrationRepository registrationRepository,
            BatchRepository batchRepository,
            AssessmentRepository assessmentRepository,
            ProgramJdbcDao programJdbcDao
    ) {
        this.programRepository = programRepository;
        this.registrationRepository = registrationRepository;
        this.batchRepository = batchRepository;
        this.assessmentRepository = assessmentRepository;
        this.programJdbcDao = programJdbcDao;
    }



    public List<ProgramDto> getAllPrograms() {
        List<Program> active = programRepository.findByIsActiveOrderByProgramLevelAscProgramNameAsc("Y");
        if (active.isEmpty()) {
            active = programRepository.findAll();
        }
        return active.stream()
                .filter(p -> !"N".equalsIgnoreCase(p.getIsActive()))
                .map(ProgramDto::fromEntity)
                .collect(Collectors.toList());
    }

    public ProgramDto getProgramById(String programId) {
        Program program = programRepository.findById(programId)
                .orElseThrow(() -> new IllegalArgumentException("Program not found with ID: " + programId));
        return ProgramDto.fromEntity(program);
    }

    @Transactional
    public void registerStudent(String studentId, String programId, LocalDate regDate) {
        programJdbcDao.registerStudentForProgram(studentId, programId, regDate);
    }

    public List<ProgramDto> getStudentRegisteredPrograms(String studentId) {
        List<String> programIds = registrationRepository.findByStudentId(studentId).stream()
                .map(ProgramRegistration::getProgramId)
                .collect(Collectors.toList());

        return programRepository.findAllById(programIds).stream()
                .map(ProgramDto::fromEntity)
                .collect(Collectors.toList());
    }

    public List<BatchDto> getAllBatches() {
        return batchRepository.findAll().stream()
                .map(BatchDto::fromEntity)
                .collect(Collectors.toList());
    }

    public List<BatchDto> getBatchesByProgram(String programId) {
        return batchRepository.findByProgramId(programId).stream()
                .map(BatchDto::fromEntity)
                .collect(Collectors.toList());
    }

    public List<AssessmentDto> getAllAssessments() {
        return assessmentRepository.findAll().stream()
                .map(AssessmentDto::fromEntity)
                .collect(Collectors.toList());
    }

    public List<AssessmentDto> getAssessmentsByBatch(String batchNo) {
        return assessmentRepository.findByBatchNo(batchNo).stream()
                .map(AssessmentDto::fromEntity)
                .collect(Collectors.toList());
    }
}
