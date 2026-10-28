package com.placement.portal.vector;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SemanticSearchRequest {

    @NotBlank(message = "Search query must not be blank")
    @Size(min = 2, max = 500, message = "Search query must be between 2 and 500 characters")
    private String query;

    /**
     * Optional filter: "ALL", "STUDENT", "DRIVE", "RESUME"
     */
    private String entityType;

    private Integer limit;

    private Float minScore;

    public SemanticSearchRequest() {
    }

    public SemanticSearchRequest(String query, String entityType, Integer limit, Float minScore) {
        this.query = query;
        this.entityType = entityType;
        this.limit = limit;
        this.minScore = minScore;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    public Float getMinScore() {
        return minScore;
    }

    public void setMinScore(Float minScore) {
        this.minScore = minScore;
    }
}
