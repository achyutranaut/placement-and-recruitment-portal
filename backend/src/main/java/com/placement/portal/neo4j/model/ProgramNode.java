package com.placement.portal.neo4j.model;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Node("Program")
public class ProgramNode {

    @Id
    private String programId;

    private String programName;
    private BigDecimal fee;
    private String duration;
    private String degreeLevel;
    private String department;
    private String specialization;
    private String regNoRegex;
    private String regNoSample;
    private String isOffered;

// batches via BatchNode.program

    public ProgramNode() {}

    public ProgramNode(String programId, String programName, BigDecimal fee, String duration,
                       String degreeLevel, String department, String specialization,
                       String regNoRegex, String regNoSample, String isOffered) {
        this.programId = programId;
        this.programName = programName;
        this.fee = fee;
        this.duration = duration;
        this.degreeLevel = degreeLevel;
        this.department = department;
        this.specialization = specialization;
        this.regNoRegex = regNoRegex;
        this.regNoSample = regNoSample;
        this.isOffered = isOffered;
    }

    public String getProgramId() { return programId; }
    public void setProgramId(String programId) { this.programId = programId; }

    public String getProgramName() { return programName; }
    public void setProgramName(String programName) { this.programName = programName; }

    public BigDecimal getFee() { return fee; }
    public void setFee(BigDecimal fee) { this.fee = fee; }

    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }

    public String getDegreeLevel() { return degreeLevel; }
    public void setDegreeLevel(String degreeLevel) { this.degreeLevel = degreeLevel; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }

    public String getRegNoRegex() { return regNoRegex; }
    public void setRegNoRegex(String regNoRegex) { this.regNoRegex = regNoRegex; }

    public String getRegNoSample() { return regNoSample; }
    public void setRegNoSample(String regNoSample) { this.regNoSample = regNoSample; }

    public String getIsOffered() { return isOffered; }
    public void setIsOffered(String isOffered) { this.isOffered = isOffered; }


}
