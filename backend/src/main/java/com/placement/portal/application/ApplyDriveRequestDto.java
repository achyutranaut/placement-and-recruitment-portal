package com.placement.portal.application;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public class ApplyDriveRequestDto {
    @NotBlank(message = "Student ID is required")
    private String studentId;

    @NotBlank(message = "Drive ID is required")
    private String driveId;

    private LocalDate applyDate = LocalDate.now();

    public ApplyDriveRequestDto() {}

    public ApplyDriveRequestDto(String studentId, String driveId, LocalDate applyDate) {
        this.studentId = studentId;
        this.driveId = driveId;
        this.applyDate = applyDate != null ? applyDate : LocalDate.now();
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getDriveId() {
        return driveId;
    }

    public void setDriveId(String driveId) {
        this.driveId = driveId;
    }

    public LocalDate getApplyDate() {
        return applyDate;
    }

    public void setApplyDate(LocalDate applyDate) {
        this.applyDate = applyDate;
    }
}
