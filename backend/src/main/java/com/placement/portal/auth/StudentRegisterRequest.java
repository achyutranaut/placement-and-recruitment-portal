package com.placement.portal.auth;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class StudentRegisterRequest {

    @NotBlank(message = "Student name is required")
    private String name;

    @NotBlank(message = "VIT institutional email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Registration number is required")
    private String registrationNo;

    @NotBlank(message = "Academic department/branch is required")
    private String branch;

    private String programId;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    @NotNull(message = "CGPA is required")
    @DecimalMin(value = "0.00", message = "CGPA cannot be negative")
    @DecimalMax(value = "10.00", message = "CGPA cannot exceed 10.00")
    private BigDecimal cgpa;

    @NotNull(message = "Date of birth is required")
    private LocalDate dob;

    @NotBlank(message = "Phone number is required")
    private String phone;

    private String street;
    private String city;
    private String state;
    private List<String> skills;

    public StudentRegisterRequest() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRegistrationNo() { return registrationNo; }
    public void setRegistrationNo(String registrationNo) { this.registrationNo = registrationNo; }

    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }

    public String getProgramId() { return programId; }
    public void setProgramId(String programId) { this.programId = programId; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public BigDecimal getCgpa() { return cgpa; }
    public void setCgpa(BigDecimal cgpa) { this.cgpa = cgpa; }

    public LocalDate getDob() { return dob; }
    public void setDob(LocalDate dob) { this.dob = dob; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getStreet() { return street; }
    public void setStreet(String street) { this.street = street; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public List<String> getSkills() { return skills; }
    public void setSkills(List<String> skills) { this.skills = skills; }
}
