package com.placement.portal.neo4j.service;

import com.placement.portal.neo4j.model.AssessmentNode;
import com.placement.portal.neo4j.model.BatchNode;
import com.placement.portal.neo4j.model.ProgramNode;
import com.placement.portal.neo4j.model.StudentNode;
import com.placement.portal.neo4j.repository.*;
import com.placement.portal.program.*;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Profile("neo4j")
@Primary
public class Neo4jProgramService implements IProgramService {

    private final Neo4jProgramRepository programRepository;
    private final Neo4jStudentRepository studentRepository;
    private final Neo4jBatchRepository batchRepository;
    private final Neo4jAssessmentRepository assessmentRepository;

    public Neo4jProgramService(
            Neo4jProgramRepository programRepository,
            Neo4jStudentRepository studentRepository,
            Neo4jBatchRepository batchRepository,
            Neo4jAssessmentRepository assessmentRepository
    ) {
        this.programRepository = programRepository;
        this.studentRepository = studentRepository;
        this.batchRepository = batchRepository;
        this.assessmentRepository = assessmentRepository;
    }

    @Override
    public List<ProgramDto> getAllPrograms() {
        return programRepository.findAllOrdered().stream()
                .filter(p -> !"N".equalsIgnoreCase(p.getIsOffered()))
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public ProgramDto getProgramById(String programId) {
        ProgramNode p = programRepository.findByProgramId(programId)
                .orElseThrow(() -> new IllegalArgumentException("Program not found with ID: " + programId));
        return toDto(p);
    }

    /**
     * Ported PL/SQL Procedure: REGISTER_STUDENT_PROGRAM
     * Creates the (:Student)-[:ENROLLED_IN]->(:Program) relationship in Neo4j within transaction.
     */
    @Override
    @Transactional
    public void registerStudent(String studentId, String programId, LocalDate regDate) {
        StudentNode student = studentRepository.findByStudentId(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found: " + studentId));
        ProgramNode program = programRepository.findByProgramId(programId)
                .orElseThrow(() -> new IllegalArgumentException("Program not found: " + programId));

        if (student.getProgram() != null && programId.equalsIgnoreCase(student.getProgram().getProgramId())) {
            throw new IllegalArgumentException("Student is already enrolled in this training program.");
        }

        student.setProgram(program);
        student.setProgramId(programId);
        studentRepository.save(student);
    }

    @Override
    public List<ProgramDto> getStudentRegisteredPrograms(String studentId) {
        return studentRepository.findByStudentId(studentId)
                .map(StudentNode::getProgram)
                .map(p -> List.of(toDto(p)))
                .orElse(List.of());
    }

    @Override
    public List<BatchDto> getAllBatches() {
        return batchRepository.findAll().stream()
                .map(this::toBatchDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<BatchDto> getBatchesByProgram(String programId) {
        return batchRepository.findByProgramId(programId).stream()
                .map(this::toBatchDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<AssessmentDto> getAllAssessments() {
        return assessmentRepository.findAll().stream()
                .map(this::toAssessmentDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<AssessmentDto> getAssessmentsByBatch(String batchNo) {
        return assessmentRepository.findByBatchNo(batchNo).stream()
                .map(this::toAssessmentDto)
                .collect(Collectors.toList());
    }

    private ProgramDto toDto(ProgramNode p) {
        String level = "Beginner";
        return new ProgramDto(
                p.getProgramId(),
                p.getProgramName(),
                p.getFee(),
                p.getDuration(),
                level,
                p.getDegreeLevel() != null ? p.getDegreeLevel() : "Bachelor's",
                p.getDepartment() != null ? p.getDepartment() : "Engineering",
                p.getSpecialization(),
                p.getRegNoRegex(),
                p.getRegNoSample(),
                p.getIsOffered() != null ? p.getIsOffered() : "Y"
        );
    }

    private BatchDto toBatchDto(BatchNode b) {
        return new BatchDto(
                b.getProgramId(),
                b.getBatchNo(),
                b.getSchedule(),
                b.getRoom(),
                b.getStartDate(),
                b.getEndDate()
        );
    }

    private AssessmentDto toAssessmentDto(AssessmentNode a) {
        String level = "Beginner";
        if ("Y".equalsIgnoreCase(a.getAdvanced())) level = "Advanced";
        else if ("Y".equalsIgnoreCase(a.getIntermediate())) level = "Intermediate";
        return new AssessmentDto(
                a.getAssessmentId(),
                a.getProgramId(),
                a.getBatchNo(),
                a.getTitle(),
                a.getMaxMarks(),
                level,
                a.getEligibilityCriteria()
        );
    }
}
