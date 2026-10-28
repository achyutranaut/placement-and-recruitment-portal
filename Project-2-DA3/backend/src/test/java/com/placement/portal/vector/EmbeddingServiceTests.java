package com.placement.portal.vector;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class EmbeddingServiceTests {

    private final EmbeddingService embeddingService = new EmbeddingService("local", "", "", 384);

    @Test
    @DisplayName("Embedding service produces 384-dimensional unit-normalized vector")
    void testVectorDimensionAndNormalization() {
        float[] vector = embeddingService.generateEmbedding("Java Spring Boot microservices and Oracle SQL");
        assertNotNull(vector);
        assertEquals(384, vector.length);

        double sumSq = 0.0;
        for (float v : vector) {
            sumSq += v * v;
        }
        assertEquals(1.0, Math.sqrt(sumSq), 0.01, "Vector must be unit normalized for cosine distance");
    }

    @Test
    @DisplayName("Deterministic embeddings: identical inputs yield identical vectors")
    void testDeterministicEmbeddings() {
        String text = "Software Engineer with experience in Docker and Kubernetes";
        float[] v1 = embeddingService.generateEmbedding(text);
        float[] v2 = embeddingService.generateEmbedding(text);

        assertEquals(v1.length, v2.length);
        for (int i = 0; i < v1.length; i++) {
            assertEquals(v1[i], v2[i], 0.0001f);
        }
    }

    @Test
    @DisplayName("Semantic affinity: Related technologies have significantly higher similarity than unrelated")
    void testSemanticAffinity() {
        float[] javaBackend = embeddingService.generateEmbedding("Java developer building Spring Boot REST microservices");
        float[] backendJob = embeddingService.generateEmbedding("Looking for backend engineer skilled in Spring, Java, and APIs");
        float[] civilSurvey = embeddingService.generateEmbedding("Civil site engineer for concrete construction and highway surveying");

        double similarityRelated = cosineSimilarity(javaBackend, backendJob);
        double similarityUnrelated = cosineSimilarity(javaBackend, civilSurvey);

        assertTrue(similarityRelated > 0.50, "Related backend queries should have high similarity, got: " + similarityRelated);
        assertTrue(similarityUnrelated < 0.35, "Cross-domain queries should have low similarity, got: " + similarityUnrelated);
        assertTrue(similarityRelated > similarityUnrelated + 0.25, "Related similarity must substantially exceed unrelated");
    }

    @Test
    @DisplayName("Empty or blank text produces zero-vector safely without throwing")
    void testBlankInputHandling() {
        float[] emptyVec = embeddingService.generateEmbedding("");
        assertNotNull(emptyVec);
        assertEquals(384, emptyVec.length);

        float[] nullVec = embeddingService.generateEmbedding(null);
        assertNotNull(nullVec);
        assertEquals(384, nullVec.length);
    }

    private double cosineSimilarity(float[] a, float[] b) {
        double dot = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0.0 || normB == 0.0) return 0.0;
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
