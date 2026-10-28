package com.placement.portal.vector;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

public class LiveQdrantIntegrationTests {

    private QdrantClientService qdrantClient;
    private EmbeddingService embeddingService;

    @BeforeEach
    void setUp() {
        qdrantClient = new QdrantClientService(true, "http://localhost:6333", "", "test-integration-search", 384);
        embeddingService = new EmbeddingService("local", "", "", 384);

        // Verify live Qdrant container is reachable before running live integration tests
        boolean reachable = qdrantClient.testConnection();
        assumeTrue(reachable, "Live Qdrant instance is not reachable on http://localhost:6333 - skipping live integration test");

        qdrantClient.initialize();
    }

    @Test
    @DisplayName("Live Qdrant: Collection creation, points batch upsert, and semantic search retrieval")
    void testLiveUpsertAndSearch() {
        // Ensure clean test collection
        assertTrue(qdrantClient.ensureCollectionExists(), "Collection must be created successfully");

        // 1. Prepare candidate vectors
        float[] v1 = embeddingService.generateEmbedding("Senior Java Backend Engineer with Spring Boot and Oracle database");
        float[] v2 = embeddingService.generateEmbedding("React Frontend UI Developer with Tailwind and TypeScript");
        float[] v3 = embeddingService.generateEmbedding("DevOps Cloud Engineer with Docker and Kubernetes infrastructure");

        List<QdrantClientService.PointRecord> batch = List.of(
                new QdrantClientService.PointRecord("550e8400-e29b-41d4-a716-446655440001", v1, Map.of("entityType", "STUDENT", "title", "Aarav Sharma", "skills", "Java, Spring Boot")),
                new QdrantClientService.PointRecord("550e8400-e29b-41d4-a716-446655440002", v2, Map.of("entityType", "STUDENT", "title", "Diya Patel", "skills", "React, TypeScript")),
                new QdrantClientService.PointRecord("550e8400-e29b-41d4-a716-446655440003", v3, Map.of("entityType", "STUDENT", "title", "Ishaan Verma", "skills", "Docker, Kubernetes"))
        );

        boolean upsertSuccess = qdrantClient.upsertPointsBatch(batch);
        assertTrue(upsertSuccess, "Batch upsert into Qdrant must succeed");

        // 2. Query with semantic query "Java Spring Boot developer"
        float[] queryVector = embeddingService.generateEmbedding("Looking for Java microservices Spring specialist");
        List<QdrantClientService.QdrantScoredPoint> results = qdrantClient.search(queryVector, 3, null);

        assertNotNull(results);
        assertFalse(results.isEmpty(), "Search must return results");

        // Top result should be Aarav Sharma (Java Backend)
        QdrantClientService.QdrantScoredPoint top = results.get(0);
        assertEquals("Aarav Sharma", top.payload.get("title"));
        assertTrue(top.score > 0.50f, "Java specialist must have high cosine score, got: " + top.score);

        // 3. Verify total points count
        long count = qdrantClient.getPointCount();
        assertTrue(count >= 3, "Collection point count must be at least 3, got: " + count);

        // 4. Clean up test collection
        qdrantClient.clearCollection();
    }
}
