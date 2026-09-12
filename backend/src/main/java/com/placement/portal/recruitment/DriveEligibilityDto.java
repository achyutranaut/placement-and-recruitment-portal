package com.placement.portal.recruitment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class DriveEligibilityDto {
    private boolean eligible;
    private boolean cgpaEligible;
    private boolean programEligible;
    private boolean deadlinePassed;
    private boolean driveOpen;
    private BigDecimal studentCgpa;
    private BigDecimal minimumCgpa;
    private String studentProgram;
    private String studentProgramId;
    private List<String> eligiblePrograms = new ArrayList<>();
    private List<String> eligibleProgramIds = new ArrayList<>();
    private LocalDate applicationDeadline;
    private String message;

    public DriveEligibilityDto() {}

    public DriveEligibilityDto(
            boolean eligible,
            boolean cgpaEligible,
            boolean programEligible,
            boolean deadlinePassed,
            boolean driveOpen,
            BigDecimal studentCgpa,
            BigDecimal minimumCgpa,
            String studentProgram,
            String studentProgramId,
            List<String> eligiblePrograms,
            List<String> eligibleProgramIds,
            LocalDate applicationDeadline,
            String message
    ) {
        this.eligible = eligible;
        this.cgpaEligible = cgpaEligible;
        this.programEligible = programEligible;
        this.deadlinePassed = deadlinePassed;
        this.driveOpen = driveOpen;
        this.studentCgpa = studentCgpa;
        this.minimumCgpa = minimumCgpa;
        this.studentProgram = studentProgram;
        this.studentProgramId = studentProgramId;
        this.eligiblePrograms = eligiblePrograms != null ? eligiblePrograms : new ArrayList<>();
        this.eligibleProgramIds = eligibleProgramIds != null ? eligibleProgramIds : new ArrayList<>();
        this.applicationDeadline = applicationDeadline;
        this.message = message;
    }

    public boolean isEligible() {
        return eligible;
    }

    public void setEligible(boolean eligible) {
        this.eligible = eligible;
    }

    public boolean isCgpaEligible() {
        return cgpaEligible;
    }

    public void setCgpaEligible(boolean cgpaEligible) {
        this.cgpaEligible = cgpaEligible;
    }

    public boolean isProgramEligible() {
        return programEligible;
    }

    public void setProgramEligible(boolean programEligible) {
        this.programEligible = programEligible;
    }

    public boolean isDeadlinePassed() {
        return deadlinePassed;
    }

    public void setDeadlinePassed(boolean deadlinePassed) {
        this.deadlinePassed = deadlinePassed;
    }

    public boolean isDriveOpen() {
        return driveOpen;
    }

    public void setDriveOpen(boolean driveOpen) {
        this.driveOpen = driveOpen;
    }

    public BigDecimal getStudentCgpa() {
        return studentCgpa;
    }

    public void setStudentCgpa(BigDecimal studentCgpa) {
        this.studentCgpa = studentCgpa;
    }

    public BigDecimal getMinimumCgpa() {
        return minimumCgpa;
    }

    public void setMinimumCgpa(BigDecimal minimumCgpa) {
        this.minimumCgpa = minimumCgpa;
    }

    public String getStudentProgram() {
        return studentProgram;
    }

    public void setStudentProgram(String studentProgram) {
        this.studentProgram = studentProgram;
    }

    public String getStudentProgramId() {
        return studentProgramId;
    }

    public void setStudentProgramId(String studentProgramId) {
        this.studentProgramId = studentProgramId;
    }

    public List<String> getEligiblePrograms() {
        return eligiblePrograms;
    }

    public void setEligiblePrograms(List<String> eligiblePrograms) {
        this.eligiblePrograms = eligiblePrograms;
    }

    public List<String> getEligibleProgramIds() {
        return eligibleProgramIds;
    }

    public void setEligibleProgramIds(List<String> eligibleProgramIds) {
        this.eligibleProgramIds = eligibleProgramIds;
    }

    public LocalDate getApplicationDeadline() {
        return applicationDeadline;
    }

    public void setApplicationDeadline(LocalDate applicationDeadline) {
        this.applicationDeadline = applicationDeadline;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
