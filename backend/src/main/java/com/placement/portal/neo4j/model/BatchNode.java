package com.placement.portal.neo4j.model;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.time.LocalDate;

@Node("Batch")
public class BatchNode {

    @Id
    private String batchId; // composite: programId + "_" + batchNo

    private String programId;
    private String batchNo;
    private String schedule;
    private String room;
    private LocalDate startDate;
    private LocalDate endDate;

    @Relationship(type = "OF_PROGRAM", direction = Relationship.Direction.OUTGOING)
    private ProgramNode program;

    public BatchNode() {}

    public BatchNode(String programId, String batchNo, String schedule, String room, LocalDate startDate, LocalDate endDate) {
        this.batchId = programId + "_" + batchNo;
        this.programId = programId;
        this.batchNo = batchNo;
        this.schedule = schedule;
        this.room = room;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public String getBatchId() { return batchId; }
    public void setBatchId(String batchId) { this.batchId = batchId; }

    public String getProgramId() { return programId; }
    public void setProgramId(String programId) { this.programId = programId; }

    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }

    public String getSchedule() { return schedule; }
    public void setSchedule(String schedule) { this.schedule = schedule; }

    public String getRoom() { return room; }
    public void setRoom(String room) { this.room = room; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public ProgramNode getProgram() { return program; }
    public void setProgram(ProgramNode program) { this.program = program; }
}
