package com.placement.portal.offer;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public class IssueOfferDto {
    @NotBlank(message = "Application ID is required")
    private String applicationId;

    @DecimalMin(value = "0.01", message = "CTC LPA must be greater than 0")
    private BigDecimal ctcLpa;

    private LocalDate offerDate = LocalDate.now();

    public IssueOfferDto() {}

    public IssueOfferDto(String applicationId, BigDecimal ctcLpa, LocalDate offerDate) {
        this.applicationId = applicationId;
        this.ctcLpa = ctcLpa;
        this.offerDate = offerDate != null ? offerDate : LocalDate.now();
    }

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public BigDecimal getCtcLpa() {
        return ctcLpa;
    }

    public void setCtcLpa(BigDecimal ctcLpa) {
        this.ctcLpa = ctcLpa;
    }

    public LocalDate getOfferDate() {
        return offerDate;
    }

    public void setOfferDate(LocalDate offerDate) {
        this.offerDate = offerDate;
    }
}
