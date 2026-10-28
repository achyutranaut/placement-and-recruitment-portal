package com.placement.portal.vector;

import java.util.List;

public class SemanticSearchResponse {

    private String query;
    private int totalFound;
    private List<SemanticSearchResultDto> results;
    private boolean qdrantConnected;
    private String database;
    private String collection;
    private int vectorDimension;
    private long executionTimeMs;

    public SemanticSearchResponse() {
    }

    public SemanticSearchResponse(
            String query,
            List<SemanticSearchResultDto> results,
            boolean qdrantConnected,
            String database,
            String collection,
            int vectorDimension,
            long executionTimeMs
    ) {
        this.query = query;
        this.results = results;
        this.totalFound = results != null ? results.size() : 0;
        this.qdrantConnected = qdrantConnected;
        this.database = database;
        this.collection = collection;
        this.vectorDimension = vectorDimension;
        this.executionTimeMs = executionTimeMs;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public int getTotalFound() {
        return totalFound;
    }

    public void setTotalFound(int totalFound) {
        this.totalFound = totalFound;
    }

    public List<SemanticSearchResultDto> getResults() {
        return results;
    }

    public void setResults(List<SemanticSearchResultDto> results) {
        this.results = results;
        this.totalFound = results != null ? results.size() : 0;
    }

    public boolean isQdrantConnected() {
        return qdrantConnected;
    }

    public void setQdrantConnected(boolean qdrantConnected) {
        this.qdrantConnected = qdrantConnected;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public String getCollection() {
        return collection;
    }

    public void setCollection(String collection) {
        this.collection = collection;
    }

    public int getVectorDimension() {
        return vectorDimension;
    }

    public void setVectorDimension(int vectorDimension) {
        this.vectorDimension = vectorDimension;
    }

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public void setExecutionTimeMs(long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }
}
