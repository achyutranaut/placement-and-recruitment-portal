package com.placement.portal.neo4j.service;

import com.placement.portal.neo4j.model.ProgramNode;
import com.placement.portal.neo4j.model.StudentNode;
import com.placement.portal.neo4j.repository.Neo4jProgramRepository;
import com.placement.portal.neo4j.repository.Neo4jStudentRepository;
import com.placement.portal.student.IStudentService;
import com.placement.portal.student.StudentDto;
import com.placement.portal.student.StudentUpdateDto;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Profile("neo4j")
@Primary
public class Neo4jStudentService implements IStudentService {

    private final Neo4jStudentRepository studentRepository;
    private final Neo4jProgramRepository programRepository;

    public Neo4jStudentService(Neo4jStudentRepository studentRepository, Neo4jProgramRepository programRepository) {
        this.studentRepository = studentRepository;
        this.programRepository = programRepository;
    }

    @Override
    public List<StudentDto> getAllStudents() {
        return studentRepository.findAllOrdered().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public StudentDto getStudentById(String studentId) {
        StudentNode student = studentRepository.findByStudentId(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found with ID: " + studentId));
        return toDto(student);
    }

    @Override
    @Transactional
    public StudentDto createStudent(StudentDto dto) {
        StudentNode student = new StudentNode(
                dto.getStudentId(),
                dto.getName(),
                dto.getEmail(),
                dto.getStreet(),
                dto.getCity(),
                dto.getState(),
                dto.getDob(),
                dto.getCgpa(),
                dto.getBranch(),
                dto.getRegistrationNo(),
                dto.getProgramId() != null ? dto.getProgramId() : "BCE"
        );
        if (dto.getPhoneNumbers() != null) {
            student.setPhoneNumbers(dto.getPhoneNumbers());
        }
        if (dto.getSkills() != null) {
            student.setSkills(dto.getSkills());
        }

        // Link academic program in graph if present
        if (student.getProgramId() != null) {
            programRepository.findByProgramId(student.getProgramId()).ifPresent(student::setProgram);
        }

        StudentNode saved = studentRepository.save(student);
        return toDto(saved);
    }

    @Override
    @Transactional
    public StudentDto updateStudentProfile(String studentId, StudentUpdateDto dto) {
        StudentNode student = studentRepository.findByStudentId(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found with ID: " + studentId));

        if (dto.getName() != null) student.setName(dto.getName());
        if (dto.getStreet() != null) student.setStreet(dto.getStreet());
        if (dto.getCity() != null) student.setCity(dto.getCity());
        if (dto.getState() != null) student.setState(dto.getState());
        if (dto.getPhoneNumbers() != null) student.setPhoneNumbers(dto.getPhoneNumbers());
        if (dto.getSkills() != null) student.setSkills(dto.getSkills());

        StudentNode updated = studentRepository.save(student);
        return toDto(updated);
    }

    @Override
    @Transactional
    public StudentDto adminUpdateStudent(String studentId, StudentDto dto) {
        StudentNode student = studentRepository.findByStudentId(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found with ID: " + studentId));

        if (dto.getName() != null) student.setName(dto.getName());
        if (dto.getEmail() != null) student.setEmail(dto.getEmail());
        if (dto.getStreet() != null) student.setStreet(dto.getStreet());
        if (dto.getCity() != null) student.setCity(dto.getCity());
        if (dto.getState() != null) student.setState(dto.getState());
        if (dto.getCgpa() != null) student.setCgpa(dto.getCgpa());
        if (dto.getBranch() != null) student.setBranch(dto.getBranch());
        if (dto.getPhoneNumbers() != null) student.setPhoneNumbers(dto.getPhoneNumbers());
        if (dto.getSkills() != null) student.setSkills(dto.getSkills());
        if (dto.getProgramId() != null) {
            student.setProgramId(dto.getProgramId());
            programRepository.findByProgramId(dto.getProgramId()).ifPresent(student::setProgram);
        }

        StudentNode updated = studentRepository.save(student);
        return toDto(updated);
    }

    @Override
    @Transactional
    public StudentDto updateProfile(String studentId, StudentDto dto) {
        return adminUpdateStudent(studentId, dto);
    }

    private StudentDto toDto(StudentNode node) {
        Integer age = null;
        if (node.getDob() != null) {
            age = java.time.Period.between(node.getDob(), java.time.LocalDate.now()).getYears();
        }
        return new StudentDto(
                node.getStudentId(),
                node.getName(),
                node.getEmail(),
                node.getStreet(),
                node.getCity(),
                node.getState(),
                node.getDob(),
                age,
                node.getCgpa(),
                node.getBranch(),
                node.getRegistrationNo(),
                node.getProgramId(),
                node.getPhoneNumbers(),
                node.getSkills()
        );
    }
}
