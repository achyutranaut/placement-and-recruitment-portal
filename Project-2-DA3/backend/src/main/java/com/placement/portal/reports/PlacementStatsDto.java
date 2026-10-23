package com.placement.portal.reports;

import java.util.List;

public class PlacementStatsDto {
    private long totalStudents;
    private long eligibleStudents;
    private long totalCompanies;
    private long activeDrives;
    private long totalApplications;
    private long totalOffers;
    private double placementRate;
    private double averagePackage;
    private double highestPackage;
    private List<CompanyHiringStatDto> companyStats;

    public PlacementStatsDto() {}

    public PlacementStatsDto(long totalStudents, long eligibleStudents, long totalCompanies, long activeDrives, long totalApplications, long totalOffers, double placementRate, double averagePackage, double highestPackage, List<CompanyHiringStatDto> companyStats) {
        this.totalStudents = totalStudents;
        this.eligibleStudents = eligibleStudents;
        this.totalCompanies = totalCompanies;
        this.activeDrives = activeDrives;
        this.totalApplications = totalApplications;
        this.totalOffers = totalOffers;
        this.placementRate = placementRate;
        this.averagePackage = averagePackage;
        this.highestPackage = highestPackage;
        this.companyStats = companyStats;
    }

    public PlacementStatsDto(long totalStudents, long totalCompanies, long activeDrives, long totalApplications, long totalOffers, double placementRate, double averagePackage, double highestPackage, List<CompanyHiringStatDto> companyStats) {
        this(totalStudents, totalStudents, totalCompanies, activeDrives, totalApplications, totalOffers, placementRate, averagePackage, highestPackage, companyStats);
    }

    public long getTotalStudents() {
        return totalStudents;
    }

    public void setTotalStudents(long totalStudents) {
        this.totalStudents = totalStudents;
    }

    public long getEligibleStudents() {
        return eligibleStudents;
    }

    public void setEligibleStudents(long eligibleStudents) {
        this.eligibleStudents = eligibleStudents;
    }

    public long getTotalCompanies() {
        return totalCompanies;
    }

    public void setTotalCompanies(long totalCompanies) {
        this.totalCompanies = totalCompanies;
    }

    public long getActiveDrives() {
        return activeDrives;
    }

    public void setActiveDrives(long activeDrives) {
        this.activeDrives = activeDrives;
    }

    public long getTotalApplications() {
        return totalApplications;
    }

    public void setTotalApplications(long totalApplications) {
        this.totalApplications = totalApplications;
    }

    public long getTotalOffers() {
        return totalOffers;
    }

    public void setTotalOffers(long totalOffers) {
        this.totalOffers = totalOffers;
    }

    public double getPlacementRate() {
        return placementRate;
    }

    public void setPlacementRate(double placementRate) {
        this.placementRate = placementRate;
    }

    public double getAveragePackage() {
        return averagePackage;
    }

    public void setAveragePackage(double averagePackage) {
        this.averagePackage = averagePackage;
    }

    public double getHighestPackage() {
        return highestPackage;
    }

    public void setHighestPackage(double highestPackage) {
        this.highestPackage = highestPackage;
    }

    public List<CompanyHiringStatDto> getCompanyStats() {
        return companyStats;
    }

    public void setCompanyStats(List<CompanyHiringStatDto> companyStats) {
        this.companyStats = companyStats;
    }
}
