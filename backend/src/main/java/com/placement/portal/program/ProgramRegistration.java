package com.placement.portal.program;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(name = "REGISTERS")
@IdClass(ProgramRegistration.RegistrationId.class)
public class ProgramRegistration {

    @Id
    @Column(name = "Student_Id", length = 20, nullable = false)
    private String studentId;

    @Id
    @Column(name = "Program_Id", length = 20, nullable = false)
    private String programId;

    @Column(name = "Reg_Date", nullable = false)
    private LocalDate regDate = LocalDate.now();

    public ProgramRegistration() {}

    public ProgramRegistration(String studentId, String programId, LocalDate regDate) {
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

    public static class RegistrationId implements Serializable {
        private String studentId;
        private String programId;

        public RegistrationId() {}

        public RegistrationId(String studentId, String programId) {
            this.studentId = studentId;
            this.programId = programId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            RegistrationId that = (RegistrationId) o;
            return Objects.equals(studentId, that.studentId) && Objects.equals(programId, that.programId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(studentId, programId);
        }
    }
}
