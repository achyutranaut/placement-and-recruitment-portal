package com.placement.portal.student;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "STUDENT")
public class Student {

    @Id
    @Column(name = "Student_Id", length = 20, nullable = false)
    private String studentId;

    @Column(name = "Name", length = 100, nullable = false)
    private String name;

    @Column(name = "Email", length = 100, nullable = false, unique = true)
    private String email;

    @Column(name = "Street", length = 150, nullable = false)
    private String street;

    @Column(name = "City", length = 50, nullable = false)
    private String city;

    @Column(name = "State", length = 50, nullable = false)
    private String state;

    @Column(name = "DOB", nullable = false)
    private LocalDate dob;

    @Column(name = "Registration_No", length = 20, unique = true, nullable = false)
    private String registrationNo;

    @Column(name = "CGPA", precision = 4, scale = 2, nullable = false)
    private BigDecimal cgpa;

    @Column(name = "Branch", length = 50, nullable = false)
    private String branch;

    @Column(name = "Program_Id", length = 20, nullable = false)
    private String programId;

    @Column(name = "Placement_Status", length = 20, nullable = false)
    private String placementStatus = "NOT_PLACED";

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "STUDENT_PHONE", joinColumns = @JoinColumn(name = "Student_Id"))
    @Column(name = "Phone")
    private List<String> phoneNumbers = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "STUDENT_SKILL", joinColumns = @JoinColumn(name = "Student_Id"))
    @Column(name = "Skill_Name")
    private List<String> skills = new ArrayList<>();

    public Student() {}

    public Student(String studentId, String name, String email, String street, String city, String state, LocalDate dob, BigDecimal cgpa, String branch, String registrationNo, String programId) {
        this.studentId = studentId;
        this.name = name;
        this.email = email;
        this.street = street;
        this.city = city;
        this.state = state;
        this.dob = dob;
        this.cgpa = cgpa;
        this.branch = branch;
        this.registrationNo = registrationNo;
        this.programId = programId;
    }

    public Student(String studentId, String name, String email, String street, String city, String state, LocalDate dob, BigDecimal cgpa, String branch, String registrationNo) {
        this(studentId, name, email, street, city, state, dob, cgpa, branch, registrationNo, null);
    }

    public Student(String studentId, String name, String email, String street, String city, String state, LocalDate dob, BigDecimal cgpa, String branch) {
        this(studentId, name, email, street, city, state, dob, cgpa, branch, null, null);
    }

    public Student(String studentId, String name, String email, String street, String city, String state, LocalDate dob, BigDecimal cgpa) {
        this(studentId, name, email, street, city, state, dob, cgpa, null, null, null);
    }

    // Derived attribute Age per DA1 specification
    @Transient
    public Integer getAge() {
        if (dob == null) return null;
        return Period.between(dob, LocalDate.now()).getYears();
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

    public String getPlacementStatus() { return placementStatus; }
    public void setPlacementStatus(String placementStatus) { this.placementStatus = placementStatus; }
}
