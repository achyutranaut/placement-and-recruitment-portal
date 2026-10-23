package com.placement.portal.offer;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "OFFER_LETTER")
public class OfferLetter {

    @Id
    @Column(name = "Offer_Id", length = 20, nullable = false)
    private String offerId;

    @Column(name = "Application_Id", length = 20, nullable = false, unique = true)
    private String applicationId;

    @Column(name = "Offer_Date", nullable = false)
    private LocalDate offerDate = LocalDate.now();

    @Column(name = "CTC_LPA", precision = 6, scale = 2, nullable = false)
    private BigDecimal ctcLpa;

    @Column(name = "Status", length = 20, nullable = false)
    private String status = "OFFERED";

    @Column(name = "Accepted_At")
    private java.time.LocalDateTime acceptedAt;

    public OfferLetter() {}

    public OfferLetter(String offerId, String applicationId, LocalDate offerDate, BigDecimal ctcLpa) {
        this.offerId = offerId;
        this.applicationId = applicationId;
        this.offerDate = offerDate != null ? offerDate : LocalDate.now();
        this.ctcLpa = ctcLpa;
        this.status = "OFFERED";
    }

    public OfferLetter(String offerId, String applicationId, LocalDate offerDate, BigDecimal ctcLpa, String status) {
        this.offerId = offerId;
        this.applicationId = applicationId;
        this.offerDate = offerDate != null ? offerDate : LocalDate.now();
        this.ctcLpa = ctcLpa;
        this.status = status != null ? status : "OFFERED";
    }

    public String getOfferId() {
        return offerId;
    }

    public void setOfferId(String offerId) {
        this.offerId = offerId;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public LocalDate getOfferDate() {
        return offerDate;
    }

    public void setOfferDate(LocalDate offerDate) {
        this.offerDate = offerDate;
    }

    public BigDecimal getCtcLpa() {
        return ctcLpa;
    }

    public void setCtcLpa(BigDecimal ctcLpa) {
        this.ctcLpa = ctcLpa;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public java.time.LocalDateTime getAcceptedAt() {
        return acceptedAt;
    }

    public void setAcceptedAt(java.time.LocalDateTime acceptedAt) {
        this.acceptedAt = acceptedAt;
    }
}
