package com.placement.portal.vector;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

/**
 * Manages official Qdrant Vector Database HTTP/REST integration for semantic recruitment search.
 * Designed with resilience and graceful fallback: when disabled or unreachable, core Oracle OLTP
 * operations remain completely uninterrupted.
 */
@Service("qdrantHealthIndicator")
public class QdrantClientService implements HealthIndicator {

    private static final Logger log = LoggerFactory.getLogger(QdrantClientService.class);

    public enum QdrantState {
        DISABLED,
        NOT_CONNECTED,
        CONNECTED,
        COLLECTION_READY,
        ERROR
    }

    @Value("${portal.qdrant.enabled:false}")
    private boolean enabled;

    @Value("${portal.qdrant.url:http://localhost:6333}")
    private String url;

    @Value("${portal.qdrant.api-key:}")
    private String apiKey;

    @Value("${portal.qdrant.collection:placement-semantic-search}")
    private String collectionName;

    @Value("${portal.qdrant.dimension:384}")
    private int dimension;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(4))
            .build();

    private volatile boolean connected = false;
    private volatile boolean collectionReady = false;
    private volatile QdrantState state = QdrantState.DISABLED;
    private volatile String lastStatusMessage = "Not initialized";

    public QdrantClientService() {
    }

    // Testing constructor
    public QdrantClientService(boolean enabled, String url, String apiKey, String collectionName, int dimension) {
        this.enabled = enabled;
        this.url = url;
        this.apiKey = apiKey;
        this.collectionName = collectionName;
        this.dimension = dimension;
    }

    @PostConstruct
    public synchronized void initialize() {
        // Fallback to direct environment variables if not bound
        String envEnabled = System.getenv("PORTAL_QDRANT_ENABLED");
        if (envEnabled != null && !envEnabled.isBlank()) {
            this.enabled = "true".equalsIgnoreCase(envEnabled.trim());
        }
        String envUrl = System.getenv("QDRANT_URL");
        if (envUrl != null && !envUrl.isBlank()) {
            this.url = envUrl.trim();
        }
        String envKey = System.getenv("QDRANT_API_KEY");
        if (envKey != null && !envKey.isBlank()) {
            this.apiKey = envKey.trim();
        }
        String envCol = System.getenv("QDRANT_COLLECTION");
        if (envCol != null && !envCol.isBlank()) {
            this.collectionName = envCol.trim();
        }

        if (!enabled) {
            this.state = QdrantState.DISABLED;
            this.connected = false;
            this.collectionReady = false;
            this.lastStatusMessage = "Qdrant vector database is disabled (portal.qdrant.enabled=false). Oracle is authoritative.";
            log.info("Qdrant Vector Database is disabled. Oracle Database 23ai remains primary transactional engine.");
            return;
        }

        if (url == null || url.isBlank()) {
            this.state = QdrantState.NOT_CONNECTED;
            this.lastStatusMessage = "Qdrant is enabled but QDRANT_URL is not configured.";
            log.warn("Qdrant is enabled but URL is missing.");
            return;
        }

        // Standardize URL (strip trailing slash)
        if (url.endsWith("/")) {
            this.url = url.substring(0, url.length() - 1);
        }

        try {
            boolean healthOk = testConnection();
            if (healthOk) {
                this.connected = true;
                boolean colCreated = ensureCollectionExists();
                if (colCreated) {
                    this.collectionReady = true;
                    this.state = QdrantState.COLLECTION_READY;
                    this.lastStatusMessage = "Connected to Qdrant. Collection '" + collectionName + "' is active (dim=" + dimension + ").";
                    log.info("Successfully connected to Qdrant at {} with collection '{}'.", sanitizeUrl(url), collectionName);
                } else {
                    this.state = QdrantState.CONNECTED;
                    this.lastStatusMessage = "Connected to Qdrant, but collection '" + collectionName + "' could not be verified.";
                }
            } else {
                this.connected = false;
                this.collectionReady = false;
                this.state = QdrantState.NOT_CONNECTED;
                this.lastStatusMessage = "Qdrant connection test failed at " + sanitizeUrl(url);
            }
        } catch (Exception e) {
            this.connected = false;
            this.collectionReady = false;
            this.state = QdrantState.NOT_CONNECTED;
            this.lastStatusMessage = "Qdrant unavailable: " + e.getMessage();
            log.warn("Could not connect to Qdrant at {}: {}. Oracle remains authoritative and fully operational.",
                    sanitizeUrl(url), e.getMessage());
        }
    }

    public synchronized boolean reconnect() {
        initialize();
        return isConnected();
    }

    public boolean testConnection() {
        try {
            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url + "/healthz"))
                    .timeout(Duration.ofSeconds(3))
                    .GET();
            addAuthHeader(reqBuilder);

            HttpResponse<String> response = httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());
            return response.statusCode() >= 200 && response.statusCode() < 300;
        } catch (Exception e) {
            // Also test /collections if /healthz not exposed
            try {
                HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                        .uri(URI.create(url + "/collections"))
                        .timeout(Duration.ofSeconds(3))
                        .GET();
                addAuthHeader(reqBuilder);
                HttpResponse<String> response = httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());
                return response.statusCode() >= 200 && response.statusCode() < 300;
            } catch (Exception ex) {
                return false;
            }
        }
    }

    public boolean ensureCollectionExists() {
        try {
            // Check if collection already exists
            HttpRequest.Builder getReq = HttpRequest.newBuilder()
                    .uri(URI.create(url + "/collections/" + collectionName))
                    .timeout(Duration.ofSeconds(4))
                    .GET();
            addAuthHeader(getReq);

            HttpResponse<String> getResp = httpClient.send(getReq.build(), HttpResponse.BodyHandlers.ofString());
            if (getResp.statusCode() == 200) {
                return true;
            }

            // Create collection if missing
            ObjectNode root = objectMapper.createObjectNode();
            ObjectNode vectors = root.putObject("vectors");
            vectors.put("size", dimension);
            vectors.put("distance", "Cosine");

            HttpRequest.Builder putReq = HttpRequest.newBuilder()
                    .uri(URI.create(url + "/collections/" + collectionName))
                    .timeout(Duration.ofSeconds(6))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(root.toString(), StandardCharsets.UTF_8));
            addAuthHeader(putReq);

            HttpResponse<String> putResp = httpClient.send(putReq.build(), HttpResponse.BodyHandlers.ofString());
            return putResp.statusCode() >= 200 && putResp.statusCode() < 300;
        } catch (Exception e) {
            log.error("Failed to ensure Qdrant collection '{}' exists: {}", collectionName, e.getMessage());
            return false;
        }
    }

    /**
     * Upserts a single vector point into Qdrant collection with payload metadata.
     */
    public boolean upsertPoint(String pointId, float[] vector, Map<String, Object> payload) {
        return upsertPointsBatch(List.of(new PointRecord(pointId, vector, payload)));
    }

    /**
     * Upserts a batch of vector points into Qdrant collection.
     */
    public boolean upsertPointsBatch(List<PointRecord> points) {
        if (!enabled || !connected) {
            log.debug("Skipping Qdrant upsert: service disabled or not connected.");
            return false;
        }
        if (points == null || points.isEmpty()) {
            return true;
        }

        try {
            ObjectNode root = objectMapper.createObjectNode();
            ArrayNode pointsArray = root.putArray("points");

            for (PointRecord pr : points) {
                ObjectNode pointNode = pointsArray.addObject();
                pointNode.put("id", pr.id);

                ArrayNode vecArray = pointNode.putArray("vector");
                for (float v : pr.vector) {
                    vecArray.add(v);
                }

                ObjectNode payloadNode = pointNode.putObject("payload");
                if (pr.payload != null) {
                    for (Map.Entry<String, Object> entry : pr.payload.entrySet()) {
                        payloadNode.putPOJO(entry.getKey(), entry.getValue());
                    }
                }
            }

            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url + "/collections/" + collectionName + "/points?wait=true"))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(root.toString(), StandardCharsets.UTF_8));
            addAuthHeader(reqBuilder);

            HttpResponse<String> response = httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());
            return response.statusCode() >= 200 && response.statusCode() < 300;
        } catch (Exception e) {
            log.error("Error upserting points into Qdrant: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Executes semantic vector search against Qdrant collection.
     */
    public List<QdrantScoredPoint> search(float[] queryVector, int limit, Map<String, Object> filterMust) {
        if (!enabled || !connected) {
            throw new IllegalStateException("Qdrant vector database is currently unavailable: " + lastStatusMessage);
        }

        try {
            ObjectNode root = objectMapper.createObjectNode();
            ArrayNode vecArray = root.putArray("vector");
            for (float v : queryVector) {
                vecArray.add(v);
            }
            root.put("limit", Math.max(1, Math.min(limit, 100)));
            root.put("with_payload", true);

            // Optional metadata filters
            if (filterMust != null && !filterMust.isEmpty()) {
                ObjectNode filterNode = root.putObject("filter");
                ArrayNode mustArray = filterNode.putArray("must");
                for (Map.Entry<String, Object> entry : filterMust.entrySet()) {
                    ObjectNode matchClause = mustArray.addObject();
                    matchClause.put("key", entry.getKey());
                    ObjectNode matchValue = matchClause.putObject("match");
                    matchValue.putPOJO("value", entry.getValue());
                }
            }

            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url + "/collections/" + collectionName + "/points/search"))
                    .timeout(Duration.ofSeconds(6))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(root.toString(), StandardCharsets.UTF_8));
            addAuthHeader(reqBuilder);

            HttpResponse<String> response = httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Qdrant search returned HTTP " + response.statusCode() + ": " + response.body());
            }

            JsonNode respNode = objectMapper.readTree(response.body());
            JsonNode resultNode = respNode.path("result");
            List<QdrantScoredPoint> results = new ArrayList<>();

            if (resultNode.isArray()) {
                for (JsonNode item : resultNode) {
                    String id = item.path("id").asText();
                    float score = (float) item.path("score").asDouble();
                    Map<String, Object> payload = new LinkedHashMap<>();
                    JsonNode payloadNode = item.path("payload");
                    if (payloadNode.isObject()) {
                        payloadNode.fields().forEachRemaining(entry -> {
                            if (entry.getValue().isTextual()) {
                                payload.put(entry.getKey(), entry.getValue().asText());
                            } else if (entry.getValue().isNumber()) {
                                payload.put(entry.getKey(), entry.getValue().numberValue());
                            } else if (entry.getValue().isBoolean()) {
                                payload.put(entry.getKey(), entry.getValue().asBoolean());
                            } else {
                                payload.put(entry.getKey(), entry.getValue().toString());
                            }
                        });
                    }
                    results.add(new QdrantScoredPoint(id, score, payload));
                }
            }

            return results;
        } catch (Exception e) {
            log.error("Failed to execute Qdrant semantic search: {}", e.getMessage());
            throw new RuntimeException("Qdrant semantic search error: " + e.getMessage(), e);
        }
    }

    public long getPointCount() {
        if (!enabled || !connected) return 0;
        try {
            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url + "/collections/" + collectionName))
                    .timeout(Duration.ofSeconds(3))
                    .GET();
            addAuthHeader(reqBuilder);

            HttpResponse<String> response = httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                return root.path("result").path("points_count").asLong(0);
            }
        } catch (Exception ignored) {}
        return 0;
    }

    public boolean clearCollection() {
        if (!enabled || !connected) return false;
        try {
            HttpRequest.Builder delReq = HttpRequest.newBuilder()
                    .uri(URI.create(url + "/collections/" + collectionName))
                    .timeout(Duration.ofSeconds(6))
                    .DELETE();
            addAuthHeader(delReq);
            httpClient.send(delReq.build(), HttpResponse.BodyHandlers.ofString());
            return ensureCollectionExists();
        } catch (Exception e) {
            log.error("Failed to reset Qdrant collection: {}", e.getMessage());
            return false;
        }
    }

    public Map<String, Object> getStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("enabled", enabled);
        status.put("connected", connected);
        status.put("collectionReady", collectionReady);
        status.put("state", state.name());
        status.put("url", sanitizeUrl(url));
        status.put("collection", collectionName);
        status.put("dimension", dimension);
        status.put("pointsCount", getPointCount());
        status.put("message", lastStatusMessage);
        status.put("oracleAuthoritative", true);
        return status;
    }

    private void addAuthHeader(HttpRequest.Builder builder) {
        if (apiKey != null && !apiKey.isBlank()) {
            builder.header("api-key", apiKey);
        }
    }

    private String sanitizeUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) return "Not configured";
        return rawUrl.replaceAll("://[^:@/]+:[^@/]+@", "://***:***@");
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isConnected() {
        return enabled && connected;
    }

    public boolean isCollectionReady() {
        return enabled && connected && collectionReady;
    }

    public String getCollectionName() {
        return collectionName;
    }

    public int getDimension() {
        return dimension;
    }

    @Override
    public Health health() {
        if (!enabled) {
            return Health.up()
                    .withDetail("state", "DISABLED")
                    .withDetail("oracleAuthoritative", true)
                    .withDetail("message", "Qdrant vector database is disabled. Oracle is primary.")
                    .build();
        }
        if (isConnected() && collectionReady) {
            return Health.up()
                    .withDetail("state", "COLLECTION_READY")
                    .withDetail("collection", collectionName)
                    .withDetail("pointsCount", getPointCount())
                    .withDetail("url", sanitizeUrl(url))
                    .build();
        }
        return Health.down()
                .withDetail("state", state.name())
                .withDetail("url", sanitizeUrl(url))
                .withDetail("message", lastStatusMessage)
                .build();
    }

    @PreDestroy
    public void close() {
        // HTTP client manages its own connection pool; no explicit shutdown required
    }

    public static class PointRecord {
        public final String id;
        public final float[] vector;
        public final Map<String, Object> payload;

        public PointRecord(String id, float[] vector, Map<String, Object> payload) {
            this.id = id;
            this.vector = vector;
            this.payload = payload;
        }
    }

    public static class QdrantScoredPoint {
        public final String id;
        public final float score;
        public final Map<String, Object> payload;

        public QdrantScoredPoint(String id, float score, Map<String, Object> payload) {
            this.id = id;
            this.score = score;
            this.payload = payload;
        }
    }
}
