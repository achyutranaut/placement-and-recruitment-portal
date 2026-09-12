package com.placement.portal.program;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "PROGRAM")
public class Program {

    @Id
    @Column(name = "Program_Id", length = 20, nullable = false)
    private String programId;

    @Column(name = "Program_Name", length = 100, nullable = false)
    private String programName;

    @Column(name = "Fee", precision = 10, scale = 2, nullable = false)
    private BigDecimal fee;

    @Column(name = "Duration", length = 50, nullable = false)
    private String duration;

    @Column(name = "Beginner", length = 1, nullable = false)
    private String beginner = "N";

    @Column(name = "Intermediate", length = 1, nullable = false)
    private String intermediate = "N";

    @Column(name = "Advanced", length = 1, nullable = false)
    private String advanced = "N";

    @Column(name = "Program_Level", length = 30)
    private String programLevel = "Bachelor's";

    @Column(name = "Discipline_Family", length = 50)
    private String disciplineFamily = "Engineering";

    @Column(name = "Specialization", length = 100)
    private String specialization;

    @Column(name = "Reg_Pattern", length = 100)
    private String regPattern;

    @Column(name = "Reg_Example", length = 30)
    private String regExample;

    @Column(name = "Is_Active", length = 1)
    private String isActive = "Y";

    public Program() {}

    public Program(String programId, String programName, BigDecimal fee, String duration, String beginner, String intermediate, String advanced) {
        this.programId = programId;
        this.programName = programName;
        this.fee = fee;
        this.duration = duration;
        this.beginner = beginner;
        this.intermediate = intermediate;
        this.advanced = advanced;
    }

    public Program(String programId, String programName, BigDecimal fee, String duration, String beginner, String intermediate, String advanced,
                   String programLevel, String disciplineFamily, String specialization, String regPattern, String regExample, String isActive) {
        this.programId = programId;
        this.programName = programName;
        this.fee = fee;
        this.duration = duration;
        this.beginner = beginner;
        this.intermediate = intermediate;
        this.advanced = advanced;
        this.programLevel = programLevel;
        this.disciplineFamily = disciplineFamily;
        this.specialization = specialization;
        this.regPattern = regPattern;
        this.regExample = regExample;
        this.isActive = isActive != null ? isActive : "Y";
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

    public String getBeginner() {
        return beginner;
    }

    public void setBeginner(String beginner) {
        this.beginner = beginner;
    }

    public String getIntermediate() {
        return intermediate;
    }

    public void setIntermediate(String intermediate) {
        this.intermediate = intermediate;
    }

    public String getAdvanced() {
        return advanced;
    }

    public void setAdvanced(String advanced) {
        this.advanced = advanced;
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
