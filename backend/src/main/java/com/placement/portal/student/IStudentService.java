package com.placement.portal.student;

import java.util.List;

public interface IStudentService {
    List<StudentDto> getAllStudents();
    StudentDto getStudentById(String studentId);
    StudentDto createStudent(StudentDto dto);
    StudentDto updateStudentProfile(String studentId, StudentUpdateDto dto);
    StudentDto adminUpdateStudent(String studentId, StudentDto dto);
    StudentDto updateProfile(String studentId, StudentDto dto);
}
