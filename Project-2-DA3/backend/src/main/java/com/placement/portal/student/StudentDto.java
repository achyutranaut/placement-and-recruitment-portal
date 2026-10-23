package com.placement.portal.student;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class StudentDto {
    private String studentId;
    private String name;
    private String email;
    private String street;
    private String city;
    private String state;
    private LocalDate dob;
    private Integer age;
    private BigDecimal cgpa;
    private String branch;
    private String registrationNo;
    private String programId;
    private List<String> phoneNumbers = new ArrayList<>();
    private List<String> skills = new ArrayList<>();

    public StudentDto() {}

    public StudentDto(String studentId, String name, String email, String street, String city, String state,
                      LocalDate dob, Integer age, BigDecimal cgpa, String branch, String registrationNo, String programId,
                      List<String> phoneNumbers, List<String> skills) {
        this.studentId = studentId;
        this.name = name;
        this.email = email;
        this.street = street;
        this.city = city;
        this.state = state;
        this.dob = dob;
        this.age = age;
        this.cgpa = cgpa;
        this.branch = branch;
        this.registrationNo = registrationNo;
        this.programId = programId;
        this.phoneNumbers = phoneNumbers != null ? phoneNumbers : new ArrayList<>();
        this.skills = skills != null ? skills : new ArrayList<>();
    }

    public StudentDto(String studentId, String name, String email, String street, String city, String state,
                      LocalDate dob, Integer age, BigDecimal cgpa, String branch, String registrationNo,
                      List<String> phoneNumbers, List<String> skills) {
        this(studentId, name, email, street, city, state, dob, age, cgpa, branch, registrationNo, null, phoneNumbers, skills);
    }

    public StudentDto(String studentId, String name, String email, String street, String city, String state,
                      LocalDate dob, Integer age, BigDecimal cgpa, String branch, List<String> phoneNumbers, List<String> skills) {
        this(studentId, name, email, street, city, state, dob, age, cgpa, branch, null, phoneNumbers, skills);
    }

    public StudentDto(String studentId, String name, String email, String street, String city, String state,
                      LocalDate dob, Integer age, BigDecimal cgpa, String branch, List<String> phoneNumbers) {
        this(studentId, name, email, street, city, state, dob, age, cgpa, branch, null, phoneNumbers, new ArrayList<>());
    }

    public StudentDto(String studentId, String name, String email, String street, String city, String state,
                      LocalDate dob, Integer age, BigDecimal cgpa, List<String> phoneNumbers) {
        this(studentId, name, email, street, city, state, dob, age, cgpa, "Computer Science & Engineering", null, phoneNumbers, new ArrayList<>());
    }

    public static StudentDto fromEntity(Student s) {
        if (s == null) return null;
        return new StudentDto(
                s.getStudentId(),
                s.getName(),
                s.getEmail(),
                s.getStreet(),
                s.getCity(),
                s.getState(),
                s.getDob(),
                s.getAge(),
                s.getCgpa(),
                s.getBranch(),
                s.getRegistrationNo(),
                s.getProgramId(),
                s.getPhoneNumbers(),
                s.getSkills()
        );
    }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getStreet() { return street; }
    public void setStreet(String street) { this.street = street; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public LocalDate getDob() { return dob; }
    public void setDob(LocalDate dob) { this.dob = dob; }

    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }

    public BigDecimal getCgpa() { return cgpa; }
    public void setCgpa(BigDecimal cgpa) { this.cgpa = cgpa; }

    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }

    public List<String> getPhoneNumbers() { return phoneNumbers; }
    public void setPhoneNumbers(List<String> phoneNumbers) { this.phoneNumbers = phoneNumbers; }

    public List<String> getSkills() { return skills; }
    public void setSkills(List<String> skills) { this.skills = skills; }

    public String getRegistrationNo() { return registrationNo; }
    public void setRegistrationNo(String registrationNo) { this.registrationNo = registrationNo; }

    public String getProgramId() { return programId; }
    public void setProgramId(String programId) { this.programId = programId; }
}
