package com.placement.portal.application;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(name = "STUDENT_DAILY_ROUTINE_DRIVE")
@IdClass(StudentDailyRoutineDrive.RoutineId.class)
public class StudentDailyRoutineDrive {

    @Id
    @Column(name = "Student_Id", length = 20, nullable = false)
    private String studentId;

    @Id
    @Column(name = "Apply_Date", nullable = false)
    private LocalDate applyDate;

    @Column(name = "Drive_Id", length = 20, nullable = false)
    private String driveId;

    public StudentDailyRoutineDrive() {}

    public StudentDailyRoutineDrive(String studentId, LocalDate applyDate, String driveId) {
        this.studentId = studentId;
        this.applyDate = applyDate;
        this.driveId = driveId;
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

    public String getDriveId() {
        return driveId;
    }

    public void setDriveId(String driveId) {
        this.driveId = driveId;
    }

    public static class RoutineId implements Serializable {
        private String studentId;
        private LocalDate applyDate;

        public RoutineId() {}

        public RoutineId(String studentId, LocalDate applyDate) {
            this.studentId = studentId;
            this.applyDate = applyDate;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            RoutineId routineId = (RoutineId) o;
            return Objects.equals(studentId, routineId.studentId) && Objects.equals(applyDate, routineId.applyDate);
        }

        @Override
        public int hashCode() {
            return Objects.hash(studentId, applyDate);
        }
    }
}
