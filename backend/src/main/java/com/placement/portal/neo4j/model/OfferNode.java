package com.placement.portal.neo4j.model;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Node("Offer")
public class OfferNode {

    @Id
    private String offerId;

    private String applicationId;
    private LocalDate offerDate;
    private BigDecimal ctcLpa;
    private String status = "OFFERED"; // OFFERED, ACCEPTED, DECLINED, WITHDRAWN
    private LocalDateTime acceptedAt;

// application mapped via applicationId

    public OfferNode() {}

    public OfferNode(String offerId, String applicationId, LocalDate offerDate, BigDecimal ctcLpa, String status) {
        this.offerId = offerId;
        this.applicationId = applicationId;
        this.offerDate = offerDate;
        this.ctcLpa = ctcLpa;
        this.status = status;
    }

    public String getOfferId() { return offerId; }
    public void setOfferId(String offerId) { this.offerId = offerId; }

    public String getApplicationId() { return applicationId; }
    public void setApplicationId(String applicationId) { this.applicationId = applicationId; }

    public LocalDate getOfferDate() { return offerDate; }
    public void setOfferDate(LocalDate offerDate) { this.offerDate = offerDate; }

    public BigDecimal getCtcLpa() { return ctcLpa; }
    public void setCtcLpa(BigDecimal ctcLpa) { this.ctcLpa = ctcLpa; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getAcceptedAt() { return acceptedAt; }
    public void setAcceptedAt(LocalDateTime acceptedAt) { this.acceptedAt = acceptedAt; }


}
