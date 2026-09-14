package com.placement.portal.student;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class StudentService {

    private final StudentRepository studentRepository;

    public StudentService(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    public List<StudentDto> getAllStudents() {
        return studentRepository.findAll().stream()
                .map(StudentDto::fromEntity)
                .collect(Collectors.toList());
    }

    public StudentDto getStudentById(String studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found with ID: " + studentId));
        return StudentDto.fromEntity(student);
    }

    @Transactional
    public StudentDto createStudent(StudentDto dto) {
        Student student = new Student(
                dto.getStudentId(),
                dto.getName(),
                dto.getEmail(),
                dto.getStreet(),
                dto.getCity(),
                dto.getState(),
                dto.getDob(),
                dto.getCgpa(),
                dto.getBranch()
        );
        if (dto.getPhoneNumbers() != null) {
            student.setPhoneNumbers(dto.getPhoneNumbers());
        }
        if (dto.getSkills() != null) {
            student.setSkills(dto.getSkills());
        }
        Student saved = studentRepository.save(student);
        return StudentDto.fromEntity(saved);
    }

    @Transactional
    public StudentDto updateStudentProfile(String studentId, StudentUpdateDto dto) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found with ID: " + studentId));

        if (dto.getName() != null) student.setName(dto.getName());
        if (dto.getStreet() != null) student.setStreet(dto.getStreet());
        if (dto.getCity() != null) student.setCity(dto.getCity());
        if (dto.getState() != null) student.setState(dto.getState());
        if (dto.getPhoneNumbers() != null) student.setPhoneNumbers(dto.getPhoneNumbers());
        if (dto.getSkills() != null) student.setSkills(dto.getSkills());

        Student updated = studentRepository.save(student);
        return StudentDto.fromEntity(updated);
    }

    @Transactional
    public StudentDto adminUpdateStudent(String studentId, StudentDto dto) {
        Student student = studentRepository.findById(studentId)
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

        Student updated = studentRepository.save(student);
        return StudentDto.fromEntity(updated);
    }

    @Transactional
    public StudentDto updateProfile(String studentId, StudentDto dto) {
        return adminUpdateStudent(studentId, dto);
    }
}
