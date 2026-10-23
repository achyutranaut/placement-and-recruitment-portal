package com.placement.portal.program;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public class RegistrationRequestDto {
    @NotBlank(message = "Student ID is required")
    private String studentId;

    @NotBlank(message = "Program ID is required")
    private String programId;

    private LocalDate regDate = LocalDate.now();

    public RegistrationRequestDto() {}

    public RegistrationRequestDto(String studentId, String programId, LocalDate regDate) {
        this.studentId = studentId;
        this.programId = programId;
        this.regDate = regDate != null ? regDate : LocalDate.now();
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getProgramId() {
        return programId;
    }

    public void setProgramId(String programId) {
        this.programId = programId;
    }

    public LocalDate getRegDate() {
        return regDate;
    }

    public void setRegDate(LocalDate regDate) {
        this.regDate = regDate;
    }
}
