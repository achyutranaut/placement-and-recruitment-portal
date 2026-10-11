package com.placement.portal.neo4j.model;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Node("Student")
public class StudentNode {

    @Id
    private String studentId;

    private String name;
    private String email;
    private String street;
    private String city;
    private String state;
    private LocalDate dob;
    private BigDecimal cgpa;
    private String branch;
    private String registrationNo;
    private String programId;
    private String placementStatus = "NOT_PLACED"; // NOT_PLACED, OFFERED, PLACED
    private List<String> phoneNumbers = new ArrayList<>();
    private List<String> skills = new ArrayList<>();

    @Relationship(type = "ENROLLED_IN", direction = Relationship.Direction.OUTGOING)
    private ProgramNode program;

    public StudentNode() {}

    public StudentNode(String studentId, String name, String email, String street, String city, String state,
                       LocalDate dob, BigDecimal cgpa, String branch, String registrationNo, String programId) {
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

    public String getRegistrationNo() { return registrationNo; }
    public void setRegistrationNo(String registrationNo) { this.registrationNo = registrationNo; }

    public String getProgramId() { return programId; }
    public void setProgramId(String programId) { this.programId = programId; }

    public String getPlacementStatus() { return placementStatus; }
    public void setPlacementStatus(String placementStatus) { this.placementStatus = placementStatus; }

    public List<String> getPhoneNumbers() { return phoneNumbers; }
    public void setPhoneNumbers(List<String> phoneNumbers) { this.phoneNumbers = phoneNumbers; }

    public List<String> getSkills() { return skills; }
    public void setSkills(List<String> skills) { this.skills = skills; }

    public ProgramNode getProgram() { return program; }
    public void setProgram(ProgramNode program) { this.program = program; }
}
