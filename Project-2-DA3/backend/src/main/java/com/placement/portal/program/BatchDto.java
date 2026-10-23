package com.placement.portal.program;

import java.time.LocalDate;

public class BatchDto {
    private String batchNo;
    private String programId;
    private String schedule;
    private String room;
    private LocalDate startDate;
    private LocalDate endDate;

    public BatchDto() {}

    public BatchDto(String batchNo, String programId, String schedule, String room, LocalDate startDate, LocalDate endDate) {
        this.batchNo = batchNo;
        this.programId = programId;
        this.schedule = schedule;
        this.room = room;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public static BatchDto fromEntity(Batch b) {
        if (b == null) return null;
        return new BatchDto(b.getBatchNo(), b.getProgramId(), b.getSchedule(), b.getRoom(), b.getStartDate(), b.getEndDate());
    }

    public String getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(String batchNo) {
        this.batchNo = batchNo;
    }

    public String getProgramId() {
        return programId;
    }

    public void setProgramId(String programId) {
        this.programId = programId;
    }

    public String getSchedule() {
        return schedule;
    }

    public void setSchedule(String schedule) {
        this.schedule = schedule;
    }

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }
}
