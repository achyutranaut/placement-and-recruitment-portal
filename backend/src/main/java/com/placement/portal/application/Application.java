package com.placement.portal.application;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "APPLICATION")
public class Application {

    @Id
    @Column(name = "Application_Id", length = 20, nullable = false)
    private String applicationId;

    @Column(name = "Student_Id", length = 20, nullable = false)
    private String studentId;

    @Column(name = "Apply_Date", nullable = false)
    private LocalDate applyDate;

    @Column(name = "Status", length = 30, nullable = false)
    private String status = "APPLIED";

    @Column(name = "Drive_Id", length = 20)
    private String driveId;

    public Application() {}

    public Application(String applicationId, String studentId, LocalDate applyDate, String status) {
        this.applicationId = applicationId;
        this.studentId = studentId;
        this.applyDate = applyDate;
        this.status = status;
    }

    public Application(String applicationId, String studentId, String driveId, LocalDate applyDate, String status) {
        this.applicationId = applicationId;
        this.studentId = studentId;
        this.driveId = driveId;
        this.applyDate = applyDate;
        this.status = status;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public LocalDate getApplyDate() {
        return applyDate;
    }

    public void setApplyDate(LocalDate applyDate) {
        this.applyDate = applyDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDriveId() {
        return driveId;
    }

    public void setDriveId(String driveId) {
        this.driveId = driveId;
    }
}
