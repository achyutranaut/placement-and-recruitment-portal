package com.placement.portal.vector;

import java.util.List;
import java.util.Map;

public class SemanticSearchResultDto {

    private String entityId;
    private String entityType; // "STUDENT", "DRIVE", "RESUME"
    private String title;
    private String subtitle;
    private float similarityScore;
    private String matchConfidence; // "HIGH" (>= 0.8), "MEDIUM" (>= 0.6), "LOW"
    private String summary;
    private List<String> tags;
    private Map<String, Object> metadata;

    public SemanticSearchResultDto() {
    }

    public SemanticSearchResultDto(
            String entityId,
            String entityType,
            String title,
            String subtitle,
            float similarityScore,
            String summary,
            List<String> tags,
            Map<String, Object> metadata
    ) {
        this.entityId = entityId;
        this.entityType = entityType;
        this.title = title;
        this.subtitle = subtitle;
        this.similarityScore = similarityScore;
        this.matchConfidence = similarityScore >= 0.75f ? "HIGH" : (similarityScore >= 0.55f ? "MEDIUM" : "LOW");
        this.summary = summary;
        this.tags = tags;
        this.metadata = metadata;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public float getSimilarityScore() {
        return similarityScore;
    }

    public void setSimilarityScore(float similarityScore) {
        this.similarityScore = similarityScore;
    }

    public String getMatchConfidence() {
        return matchConfidence;
    }

    public void setMatchConfidence(String matchConfidence) {
        this.matchConfidence = matchConfidence;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}
