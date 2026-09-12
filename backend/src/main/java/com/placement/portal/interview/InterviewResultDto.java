package com.placement.portal.interview;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public class InterviewResultDto {
    private BigDecimal oa;
    private BigDecimal gd;
    private BigDecimal hr;

    @NotBlank(message = "Result status is required (CLEARED, REJECTED)")
    private String result;

    public InterviewResultDto() {}

    public InterviewResultDto(BigDecimal oa, BigDecimal gd, BigDecimal hr, String result) {
        this.oa = oa;
        this.gd = gd;
        this.hr = hr;
        this.result = result;
    }

    public BigDecimal getOa() {
        return oa;
    }

    public void setOa(BigDecimal oa) {
        this.oa = oa;
    }

    public BigDecimal getGd() {
        return gd;
    }

    public void setGd(BigDecimal gd) {
        this.gd = gd;
    }

    public BigDecimal getHr() {
        return hr;
    }

    public void setHr(BigDecimal hr) {
        this.hr = hr;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }
}
