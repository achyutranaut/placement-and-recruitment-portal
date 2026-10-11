package com.placement.portal.neo4j.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class GraphShowcaseDto {

    public static class EligibleDriveGraphDto {
        private String driveId;
        private String jobTitle;
        private String companyName;
        private BigDecimal ctcLpa;
        private BigDecimal minCgpa;
        private LocalDate applicationDeadline;
        private List<String> traversalPath;

        public EligibleDriveGraphDto(String driveId, String jobTitle, String companyName, BigDecimal ctcLpa,
                                     BigDecimal minCgpa, LocalDate applicationDeadline, List<String> traversalPath) {
            this.driveId = driveId;
            this.jobTitle = jobTitle;
            this.companyName = companyName;
            this.ctcLpa = ctcLpa;
            this.minCgpa = minCgpa;
            this.applicationDeadline = applicationDeadline;
            this.traversalPath = traversalPath;
        }

        public String getDriveId() { return driveId; }
        public String getJobTitle() { return jobTitle; }
        public String getCompanyName() { return companyName; }
        public BigDecimal getCtcLpa() { return ctcLpa; }
        public BigDecimal getMinCgpa() { return minCgpa; }
        public LocalDate getApplicationDeadline() { return applicationDeadline; }
        public List<String> getTraversalPath() { return traversalPath; }
    }

    public static class PlacementPathDto {
        private String studentId;
        private String studentName;
        private String programName;
        private String applicationId;
        private String driveId;
        private String jobTitle;
        private String companyName;
        private String offerId;
        private BigDecimal ctcLpa;
        private String offerStatus;
        private List<InterviewStepDto> interviewSteps;

        public PlacementPathDto() {}

        public String getStudentId() { return studentId; }
        public void setStudentId(String studentId) { this.studentId = studentId; }

        public String getStudentName() { return studentName; }
        public void setStudentName(String studentName) { this.studentName = studentName; }

        public String getProgramName() { return programName; }
        public void setProgramName(String programName) { this.programName = programName; }

        public String getApplicationId() { return applicationId; }
        public void setApplicationId(String applicationId) { this.applicationId = applicationId; }

        public String getDriveId() { return driveId; }
        public void setDriveId(String driveId) { this.driveId = driveId; }

        public String getJobTitle() { return jobTitle; }
        public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }

        public String getCompanyName() { return companyName; }
        public void setCompanyName(String companyName) { this.companyName = companyName; }

        public String getOfferId() { return offerId; }
        public void setOfferId(String offerId) { this.offerId = offerId; }

        public BigDecimal getCtcLpa() { return ctcLpa; }
        public void setCtcLpa(BigDecimal ctcLpa) { this.ctcLpa = ctcLpa; }

        public String getOfferStatus() { return offerStatus; }
        public void setOfferStatus(String offerStatus) { this.offerStatus = offerStatus; }

        public List<InterviewStepDto> getInterviewSteps() { return interviewSteps; }
        public void setInterviewSteps(List<InterviewStepDto> interviewSteps) { this.interviewSteps = interviewSteps; }
    }

    public static class InterviewStepDto {
        private Integer roundNo;
        private String interviewerName;
        private String result;
        private BigDecimal score;

        public InterviewStepDto(Integer roundNo, String interviewerName, String result, BigDecimal score) {
            this.roundNo = roundNo;
            this.interviewerName = interviewerName;
            this.result = result;
            this.score = score;
        }

        public Integer getRoundNo() { return roundNo; }
        public String getInterviewerName() { return interviewerName; }
        public String getResult() { return result; }
        public BigDecimal getScore() { return score; }
    }

    public static class ProgramHiringPipelineDto {
        private String programId;
        private String programName;
        private String companyId;
        private String companyName;
        private long drivesCount;
        private long offersCount;
        private double averageCtc;

        public ProgramHiringPipelineDto(String programId, String programName, String companyId, String companyName,
                                        long drivesCount, long offersCount, double averageCtc) {
            this.programId = programId;
            this.programName = programName;
            this.companyId = companyId;
            this.companyName = companyName;
            this.drivesCount = drivesCount;
            this.offersCount = offersCount;
            this.averageCtc = averageCtc;
        }

        public String getProgramId() { return programId; }
        public String getProgramName() { return programName; }
        public String getCompanyId() { return companyId; }
        public String getCompanyName() { return companyName; }
        public long getDrivesCount() { return drivesCount; }
        public long getOffersCount() { return offersCount; }
        public double getAverageCtc() { return averageCtc; }
    }

    public static class SimilarPlacedRecommendationDto {
        private String studentId;
        private String studentName;
        private BigDecimal cgpa;
        private String companyName;
        private String jobTitle;
        private BigDecimal ctcLpa;
        private double cgpaDifference;

        public SimilarPlacedRecommendationDto(String studentId, String studentName, BigDecimal cgpa,
                                              String companyName, String jobTitle, BigDecimal ctcLpa, double cgpaDifference) {
            this.studentId = studentId;
            this.studentName = studentName;
            this.cgpa = cgpa;
            this.companyName = companyName;
            this.jobTitle = jobTitle;
            this.ctcLpa = ctcLpa;
            this.cgpaDifference = cgpaDifference;
        }

        public String getStudentId() { return studentId; }
        public String getStudentName() { return studentName; }
        public BigDecimal getCgpa() { return cgpa; }
        public String getCompanyName() { return companyName; }
        public String getJobTitle() { return jobTitle; }
        public BigDecimal getCtcLpa() { return ctcLpa; }
        public double getCgpaDifference() { return cgpaDifference; }
    }

    public static class InterviewerWorkloadDto {
        private String interviewerName;
        private Integer roundNo;
        private long totalScheduled;
        private long clearedCount;
        private long rejectedCount;

        public InterviewerWorkloadDto(String interviewerName, Integer roundNo, long totalScheduled,
                                      long clearedCount, long rejectedCount) {
            this.interviewerName = interviewerName;
            this.roundNo = roundNo;
            this.totalScheduled = totalScheduled;
            this.clearedCount = clearedCount;
            this.rejectedCount = rejectedCount;
        }

        public String getInterviewerName() { return interviewerName; }
        public Integer getRoundNo() { return roundNo; }
        public long getTotalScheduled() { return totalScheduled; }
        public long getClearedCount() { return clearedCount; }
        public long getRejectedCount() { return rejectedCount; }
    }
}
