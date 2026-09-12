package com.placement.portal.reports;

import java.util.List;
import java.util.Map;

public class OverviewReportDto {
    private double placementRate;
    private double averagePackage;
    private double highestPackage;
    private long totalOffers;
    private long uniqueStudentsPlaced;
    private long totalStudents;
    private long eligibleStudents;
    private long totalCompanies;
    private long activeDrives;
    private long totalApplications;
    private long totalInterviews;
    private Map<String, Long> applicationStatusCounts;
    private List<CtcTierStat> ctcDistribution;
    private List<CompanyHiringStatDto> companyHiring;
    private List<ProgramPlacementStat> programPlacement;

    public static class CtcTierStat {
        private String tier;
        private long count;
        private double percentage;

        public CtcTierStat() {}

        public CtcTierStat(String tier, long count, double percentage) {
            this.tier = tier;
            this.count = count;
            this.percentage = percentage;
        }

        public String getTier() { return tier; }
        public void setTier(String tier) { this.tier = tier; }
        public long getCount() { return count; }
        public void setCount(long count) { this.count = count; }
        public double getPercentage() { return percentage; }
        public void setPercentage(double percentage) { this.percentage = percentage; }
    }

    public static class ProgramPlacementStat {
        private String programName;
        private long enrolled;
        private long placed;
        private double rate;

        public ProgramPlacementStat() {}

        public ProgramPlacementStat(String programName, long enrolled, long placed, double rate) {
            this.programName = programName;
            this.enrolled = enrolled;
            this.placed = placed;
            this.rate = rate;
        }

        public String getProgramName() { return programName; }
        public void setProgramName(String programName) { this.programName = programName; }
        public long getEnrolled() { return enrolled; }
        public void setEnrolled(long enrolled) { this.enrolled = enrolled; }
        public long getPlaced() { return placed; }
        public void setPlaced(long placed) { this.placed = placed; }
        public double getRate() { return rate; }
        public void setRate(double rate) { this.rate = rate; }
    }

    public OverviewReportDto() {}

    public OverviewReportDto(
            double placementRate,
            double averagePackage,
            double highestPackage,
            long totalOffers,
            long uniqueStudentsPlaced,
            long totalStudents,
            long eligibleStudents,
            long totalCompanies,
            long activeDrives,
            long totalApplications,
            long totalInterviews,
            Map<String, Long> applicationStatusCounts,
            List<CtcTierStat> ctcDistribution,
            List<CompanyHiringStatDto> companyHiring,
            List<ProgramPlacementStat> programPlacement
    ) {
        this.placementRate = placementRate;
        this.averagePackage = averagePackage;
        this.highestPackage = highestPackage;
        this.totalOffers = totalOffers;
        this.uniqueStudentsPlaced = uniqueStudentsPlaced;
        this.totalStudents = totalStudents;
        this.eligibleStudents = eligibleStudents;
        this.totalCompanies = totalCompanies;
        this.activeDrives = activeDrives;
        this.totalApplications = totalApplications;
        this.totalInterviews = totalInterviews;
        this.applicationStatusCounts = applicationStatusCounts;
        this.ctcDistribution = ctcDistribution;
        this.companyHiring = companyHiring;
        this.programPlacement = programPlacement;
    }

    public OverviewReportDto(
            double placementRate,
            double averagePackage,
            double highestPackage,
            long totalOffers,
            long uniqueStudentsPlaced,
            long totalStudents,
            long totalCompanies,
            long activeDrives,
            long totalApplications,
            long totalInterviews,
            Map<String, Long> applicationStatusCounts,
            List<CtcTierStat> ctcDistribution,
            List<CompanyHiringStatDto> companyHiring,
            List<ProgramPlacementStat> programPlacement
    ) {
        this(placementRate, averagePackage, highestPackage, totalOffers, uniqueStudentsPlaced, totalStudents, totalStudents,
                totalCompanies, activeDrives, totalApplications, totalInterviews, applicationStatusCounts, ctcDistribution,
                companyHiring, programPlacement);
    }

    public double getPlacementRate() { return placementRate; }
    public void setPlacementRate(double placementRate) { this.placementRate = placementRate; }
    public double getAveragePackage() { return averagePackage; }
    public void setAveragePackage(double averagePackage) { this.averagePackage = averagePackage; }
    public double getHighestPackage() { return highestPackage; }
    public void setHighestPackage(double highestPackage) { this.highestPackage = highestPackage; }
    public long getTotalOffers() { return totalOffers; }
    public void setTotalOffers(long totalOffers) { this.totalOffers = totalOffers; }
    public long getUniqueStudentsPlaced() { return uniqueStudentsPlaced; }
    public void setUniqueStudentsPlaced(long uniqueStudentsPlaced) { this.uniqueStudentsPlaced = uniqueStudentsPlaced; }
    public long getTotalStudents() { return totalStudents; }
    public void setTotalStudents(long totalStudents) { this.totalStudents = totalStudents; }
    public long getEligibleStudents() { return eligibleStudents; }
    public void setEligibleStudents(long eligibleStudents) { this.eligibleStudents = eligibleStudents; }
    public long getTotalCompanies() { return totalCompanies; }
    public void setTotalCompanies(long totalCompanies) { this.totalCompanies = totalCompanies; }
    public long getActiveDrives() { return activeDrives; }
    public void setActiveDrives(long activeDrives) { this.activeDrives = activeDrives; }
    public long getTotalApplications() { return totalApplications; }
    public void setTotalApplications(long totalApplications) { this.totalApplications = totalApplications; }
    public long getTotalInterviews() { return totalInterviews; }
    public void setTotalInterviews(long totalInterviews) { this.totalInterviews = totalInterviews; }
    public Map<String, Long> getApplicationStatusCounts() { return applicationStatusCounts; }
    public void setApplicationStatusCounts(Map<String, Long> applicationStatusCounts) { this.applicationStatusCounts = applicationStatusCounts; }
    public List<CtcTierStat> getCtcDistribution() { return ctcDistribution; }
    public void setCtcDistribution(List<CtcTierStat> ctcDistribution) { this.ctcDistribution = ctcDistribution; }
    public List<CompanyHiringStatDto> getCompanyHiring() { return companyHiring; }
    public void setCompanyHiring(List<CompanyHiringStatDto> companyHiring) { this.companyHiring = companyHiring; }
    public List<ProgramPlacementStat> getProgramPlacement() { return programPlacement; }
    public void setProgramPlacement(List<ProgramPlacementStat> programPlacement) { this.programPlacement = programPlacement; }
}
