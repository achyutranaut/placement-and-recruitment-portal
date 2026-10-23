package com.placement.portal.program;

import java.math.BigDecimal;

public class ProgramDto {
    private String programId;
    private String programName;
    private BigDecimal fee;
    private String duration;
    private String level; // Beginner, Intermediate, or Advanced
    private String programLevel; // Bachelor's, Master's, Integrated, Doctoral
    private String disciplineFamily; // Computer Science, Electronics, Mechanical, etc.
    private String specialization;
    private String regPattern;
    private String regExample;
    private String isActive;

    public ProgramDto() {}

    public ProgramDto(String programId, String programName, BigDecimal fee, String duration, String level) {
        this.programId = programId;
        this.programName = programName;
        this.fee = fee;
        this.duration = duration;
        this.level = level;
    }

    public ProgramDto(String programId, String programName, BigDecimal fee, String duration, String level,
                      String programLevel, String disciplineFamily, String specialization,
                      String regPattern, String regExample, String isActive) {
        this.programId = programId;
        this.programName = programName;
        this.fee = fee;
        this.duration = duration;
        this.level = level;
        this.programLevel = programLevel;
        this.disciplineFamily = disciplineFamily;
        this.specialization = specialization;
        this.regPattern = regPattern;
        this.regExample = regExample;
        this.isActive = isActive;
    }

    public static ProgramDto fromEntity(Program p) {
        if (p == null) return null;
        String level = "Beginner";
        if ("Y".equalsIgnoreCase(p.getAdvanced())) level = "Advanced";
        else if ("Y".equalsIgnoreCase(p.getIntermediate())) level = "Intermediate";

        return new ProgramDto(
                p.getProgramId(),
                p.getProgramName(),
                p.getFee(),
                p.getDuration(),
                level,
                p.getProgramLevel() != null ? p.getProgramLevel() : "Bachelor's",
                p.getDisciplineFamily() != null ? p.getDisciplineFamily() : "Engineering",
                p.getSpecialization(),
                p.getRegPattern(),
                p.getRegExample(),
                p.getIsActive() != null ? p.getIsActive() : "Y"
        );
    }

    public String getProgramId() {
        return programId;
    }

    public void setProgramId(String programId) {
        this.programId = programId;
    }

    public String getProgramName() {
        return programName;
    }

    public void setProgramName(String programName) {
        this.programName = programName;
    }

    public BigDecimal getFee() {
        return fee;
    }

    public void setFee(BigDecimal fee) {
        this.fee = fee;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getProgramLevel() {
        return programLevel;
    }

    public void setProgramLevel(String programLevel) {
        this.programLevel = programLevel;
    }

    public String getDisciplineFamily() {
        return disciplineFamily;
    }

    public void setDisciplineFamily(String disciplineFamily) {
        this.disciplineFamily = disciplineFamily;
    }

    public String getSpecialization() {
        return specialization;
    }

    public void setSpecialization(String specialization) {
        this.specialization = specialization;
    }

    public String getRegPattern() {
        return regPattern;
    }

    public void setRegPattern(String regPattern) {
        this.regPattern = regPattern;
    }

    public String getRegExample() {
        return regExample;
    }

    public void setRegExample(String regExample) {
        this.regExample = regExample;
    }

    public String getIsActive() {
        return isActive;
    }

    public void setIsActive(String isActive) {
        this.isActive = isActive;
    }
}
