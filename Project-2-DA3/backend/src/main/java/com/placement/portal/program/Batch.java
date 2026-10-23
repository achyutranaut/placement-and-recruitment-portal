package com.placement.portal.program;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(name = "BATCH")
@IdClass(Batch.BatchId.class)
public class Batch {

    @Id
    @Column(name = "Program_Id", length = 20, nullable = false)
    private String programId;

    @Id
    @Column(name = "Batch_No", length = 20, nullable = false)
    private String batchNo;

    @Column(name = "Schedule", length = 100, nullable = false)
    private String schedule;

    @Column(name = "Room", length = 50, nullable = false)
    private String room;

    @Column(name = "Start_Date", nullable = false)
    private LocalDate startDate;

    @Column(name = "End_Date", nullable = false)
    private LocalDate endDate;

    public Batch() {}

    public Batch(String programId, String batchNo, String schedule, String room, LocalDate startDate, LocalDate endDate) {
        this.programId = programId;
        this.batchNo = batchNo;
        this.schedule = schedule;
        this.room = room;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public String getProgramId() {
        return programId;
    }

    public void setProgramId(String programId) {
        this.programId = programId;
    }

    public String getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(String batchNo) {
        this.batchNo = batchNo;
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

    public static class BatchId implements Serializable {
        private String programId;
        private String batchNo;

        public BatchId() {}

        public BatchId(String programId, String batchNo) {
            this.programId = programId;
            this.batchNo = batchNo;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            BatchId batchId = (BatchId) o;
            return Objects.equals(programId, batchId.programId) && Objects.equals(batchNo, batchId.batchNo);
        }

        @Override
        public int hashCode() {
            return Objects.hash(programId, batchNo);
        }
    }
}
