package com.placement.portal.recruitment;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "DRIVE_ELIGIBILITY")
@IdClass(DriveEligibility.EligibilityId.class)
public class DriveEligibility {

    @Id
    @Column(name = "Drive_Id", length = 20, nullable = false)
    private String driveId;

    @Id
    @Column(name = "Program_Id", length = 20, nullable = false)
    private String programId;

    public DriveEligibility() {}

    public DriveEligibility(String driveId, String programId) {
        this.driveId = driveId;
        this.programId = programId;
    }

    public String getDriveId() {
        return driveId;
    }

    public void setDriveId(String driveId) {
        this.driveId = driveId;
    }

    public String getProgramId() {
        return programId;
    }

    public void setProgramId(String programId) {
        this.programId = programId;
    }

    public static class EligibilityId implements Serializable {
        private String driveId;
        private String programId;

        public EligibilityId() {}

        public EligibilityId(String driveId, String programId) {
            this.driveId = driveId;
            this.programId = programId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            EligibilityId that = (EligibilityId) o;
            return Objects.equals(driveId, that.driveId) && Objects.equals(programId, that.programId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(driveId, programId);
        }
    }
}
