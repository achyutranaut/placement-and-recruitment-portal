package com.placement.portal.vector;

public class IndexSummaryDto {

    private boolean success;
    private int studentsIndexed;
    private int drivesIndexed;
    private int resumesIndexed;
    private long totalPointsInCollection;
    private long durationMs;
    private String collectionName;
    private String message;

    public IndexSummaryDto() {
    }

    public IndexSummaryDto(
            boolean success,
            int studentsIndexed,
            int drivesIndexed,
            int resumesIndexed,
            long totalPointsInCollection,
            long durationMs,
            String collectionName,
            String message
    ) {
        this.success = success;
        this.studentsIndexed = studentsIndexed;
        this.drivesIndexed = drivesIndexed;
        this.resumesIndexed = resumesIndexed;
        this.totalPointsInCollection = totalPointsInCollection;
        this.durationMs = durationMs;
        this.collectionName = collectionName;
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public int getStudentsIndexed() {
        return studentsIndexed;
    }

    public void setStudentsIndexed(int studentsIndexed) {
        this.studentsIndexed = studentsIndexed;
    }

    public int getDrivesIndexed() {
        return drivesIndexed;
    }

    public void setDrivesIndexed(int drivesIndexed) {
        this.drivesIndexed = drivesIndexed;
    }

    public int getResumesIndexed() {
        return resumesIndexed;
    }

    public void setResumesIndexed(int resumesIndexed) {
        this.resumesIndexed = resumesIndexed;
    }

    public long getTotalPointsInCollection() {
        return totalPointsInCollection;
    }

    public void setTotalPointsInCollection(long totalPointsInCollection) {
        this.totalPointsInCollection = totalPointsInCollection;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }

    public String getCollectionName() {
        return collectionName;
    }

    public void setCollectionName(String collectionName) {
        this.collectionName = collectionName;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
