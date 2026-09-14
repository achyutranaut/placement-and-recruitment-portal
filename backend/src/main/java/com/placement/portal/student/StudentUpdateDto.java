package com.placement.portal.student;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(description = "DTO for updating student mutable personal profile details (excludes academic records)")
public class StudentUpdateDto {

    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    @Size(max = 150, message = "Street must not exceed 150 characters")
    private String street;

    @Size(max = 50, message = "City must not exceed 50 characters")
    private String city;

    @Size(max = 50, message = "State must not exceed 50 characters")
    private String state;

    private List<String> phoneNumbers;
    private List<String> skills;

    public StudentUpdateDto() {}

    public StudentUpdateDto(String name, String street, String city, String state, List<String> phoneNumbers, List<String> skills) {
        this.name = name;
        this.street = street;
        this.city = city;
        this.state = state;
        this.phoneNumbers = phoneNumbers;
        this.skills = skills;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getStreet() { return street; }
    public void setStreet(String street) { this.street = street; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public List<String> getPhoneNumbers() { return phoneNumbers; }
    public void setPhoneNumbers(List<String> phoneNumbers) { this.phoneNumbers = phoneNumbers; }

    public List<String> getSkills() { return skills; }
    public void setSkills(List<String> skills) { this.skills = skills; }
}
