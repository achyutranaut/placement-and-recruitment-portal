package com.placement.portal.reports;

public class CompanyHiringStatDto {
    private String companyName;
    private String industry;
    private long totalOffers;
    private double averageCtc;
    private double highestCtc;
    private double lowestCtc;

    public CompanyHiringStatDto() {}

    public CompanyHiringStatDto(String companyName, String industry, long totalOffers, double averageCtc, double highestCtc, double lowestCtc) {
        this.companyName = companyName;
        this.industry = industry;
        this.totalOffers = totalOffers;
        this.averageCtc = averageCtc;
        this.highestCtc = highestCtc;
        this.lowestCtc = lowestCtc;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getIndustry() {
        return industry;
    }

    public void setIndustry(String industry) {
        this.industry = industry;
    }

    public long getTotalOffers() {
        return totalOffers;
    }

    public void setTotalOffers(long totalOffers) {
        this.totalOffers = totalOffers;
    }

    public double getAverageCtc() {
        return averageCtc;
    }

    public void setAverageCtc(double averageCtc) {
        this.averageCtc = averageCtc;
    }

    public double getHighestCtc() {
        return highestCtc;
    }

    public void setHighestCtc(double highestCtc) {
        this.highestCtc = highestCtc;
    }

    public double getLowestCtc() {
        return lowestCtc;
    }

    public void setLowestCtc(double lowestCtc) {
        this.lowestCtc = lowestCtc;
    }
}
