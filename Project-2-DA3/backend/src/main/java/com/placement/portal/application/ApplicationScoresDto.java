package com.placement.portal.application;

import java.math.BigDecimal;

public class ApplicationScoresDto {
    private BigDecimal oa;
    private BigDecimal technical;
    private BigDecimal hr;
    private BigDecimal overall;
    private int completedRounds;

    public ApplicationScoresDto() {}

    public ApplicationScoresDto(BigDecimal oa, BigDecimal technical, BigDecimal hr, BigDecimal overall) {
        this.oa = oa;
        this.technical = technical;
        this.hr = hr;
        this.overall = overall;
    }

    public ApplicationScoresDto(BigDecimal oa, BigDecimal technical, BigDecimal hr, BigDecimal overall, int completedRounds) {
        this.oa = oa;
        this.technical = technical;
        this.hr = hr;
        this.overall = overall;
        this.completedRounds = completedRounds;
    }

    public BigDecimal getOa() {
        return oa;
    }

    public void setOa(BigDecimal oa) {
        this.oa = oa;
    }

    public BigDecimal getTechnical() {
        return technical;
    }

    public void setTechnical(BigDecimal technical) {
        this.technical = technical;
    }

    public BigDecimal getHr() {
        return hr;
    }

    public void setHr(BigDecimal hr) {
        this.hr = hr;
    }

    public BigDecimal getOverall() {
        return overall;
    }

    public void setOverall(BigDecimal overall) {
        this.overall = overall;
    }

    public int getCompletedRounds() {
        return completedRounds;
    }

    public void setCompletedRounds(int completedRounds) {
        this.completedRounds = completedRounds;
    }
}
