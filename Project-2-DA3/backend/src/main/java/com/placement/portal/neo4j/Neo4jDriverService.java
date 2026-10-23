package com.placement.portal.neo4j;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.neo4j.driver.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Manages the official Neo4j Java Driver lifecycle for Neo4j Aura cloud integration.
 * Operates gracefully: when disabled or Aura is unreachable, Oracle operations continue
 * without interruption.
 */
@Service("neo4jHealthIndicator")
public class Neo4jDriverService implements HealthIndicator {

    private static final Logger log = LoggerFactory.getLogger(Neo4jDriverService.class);

    @Value("${portal.neo4j.enabled:false}")
    private boolean enabled;

    @Value("${portal.neo4j.uri:}")
    private String uri;

    @Value("${portal.neo4j.username:neo4j}")
    private String username;

    @Value("${portal.neo4j.password:}")
    private String password;

    private Driver driver;
    private volatile boolean connected = false;
    private volatile String lastStatusMessage = "Not initialized";

    @PostConstruct
    public void initialize() {
        // Fallback to direct environment variables if not bound by Spring Boot properties
        String envEnabled = System.getenv("PORTAL_NEO4J_ENABLED");
        if (envEnabled != null && !envEnabled.isBlank()) {
            this.enabled = "true".equalsIgnoreCase(envEnabled.trim());
        }
        String envUri = System.getenv("NEO4J_URI");
        if (envUri != null && !envUri.isBlank()) {
            this.uri = envUri.trim();
        }
        String envUsername = System.getenv("NEO4J_USERNAME");
        if (envUsername != null && !envUsername.isBlank()) {
            this.username = envUsername.trim();
        }
        String envPassword = System.getenv("NEO4J_PASSWORD");
        if (envPassword != null && !envPassword.isBlank()) {
            this.password = envPassword.trim();
        }

        if (!enabled) {
            lastStatusMessage = "Neo4j Aura integration is disabled (portal.neo4j.enabled=false).";
            log.info("Neo4j Aura integration is disabled. Oracle is the primary and sole active database.");
            return;
        }

        if (uri == null || uri.isBlank() || password == null || password.isBlank()) {
            lastStatusMessage = "Neo4j Aura is enabled but credentials (uri/password) are not configured.";
            log.warn("Neo4j Aura is enabled but uri or password is missing. Check NEO4J_URI and NEO4J_PASSWORD.");
            return;
        }

        try {
            Config config = Config.builder()
                    .withMaxConnectionPoolSize(10)
                    .withConnectionTimeout(15, TimeUnit.SECONDS)
                    .build();

            this.driver = GraphDatabase.driver(uri, AuthTokens.basic(username, password), config);
            // Verify connectivity without blocking indefinitely
            driver.verifyConnectivity();
            this.connected = true;
            this.lastStatusMessage = "Connected to Neo4j Aura (" + sanitizeUri(uri) + ")";
            log.info("Successfully connected to Neo4j Aura at {}", sanitizeUri(uri));
        } catch (Exception e) {
            this.connected = false;
            this.lastStatusMessage = "Connection failed to Neo4j Aura: " + sanitizeMessage(e.getMessage());
            log.warn("Could not connect to Neo4j Aura: {}. Oracle remains authoritative and fully operational.", sanitizeMessage(e.getMessage()));
        }
    }

    public synchronized boolean reconnect() {
        if (driver != null) {
            try {
                driver.close();
            } catch (Exception ignored) {}
            driver = null;
        }
        this.connected = false;
        initialize();
        return isConnected();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isConnected() {
        if (!enabled || driver == null) {
            return false;
        }
        return connected;
    }

    public Session openSession() {
        return openSession(null);
    }

    public Session openSession(SessionConfig config) {
        if (!enabled) {
            throw new IllegalStateException("Neo4j Aura integration is disabled. Set portal.neo4j.enabled=true to enable.");
        }
        if (driver == null || !connected) {
            throw new IllegalStateException("Neo4j Aura is currently unavailable: " + lastStatusMessage);
        }
        return config != null ? driver.session(config) : driver.session();
    }

    public Driver getDriver() {
        return driver;
    }

    public Map<String, Object> getStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("enabled", enabled);
        status.put("connected", isConnected());

        String state;
        if (!enabled) {
            state = "DISABLED";
        } else if (!isConnected()) {
            state = "NOT_CONNECTED";
        } else {
            state = "CONNECTED";
        }
        status.put("state", state);
        status.put("uri", sanitizeUri(uri));
        status.put("username", username);
        status.put("database", "neo4j");
        status.put("message", sanitizeMessage(lastStatusMessage));
        status.put("oracleAuthoritative", true);
        return status;
    }

    private String sanitizeUri(String rawUri) {
        if (rawUri == null || rawUri.isBlank()) return "Not configured";
        // Do not leak passwords if uri is like neo4j+s://user:pass@host
        return rawUri.replaceAll("://.*@", "://***@");
    }

    private String sanitizeMessage(String msg) {
        if (msg == null) return "";
        if (password != null && !password.isBlank()) {
            msg = msg.replace(password, "***");
        }
        return msg.replaceAll("://[^:@/]+:[^@/]+@", "://***:***@");
    }

    @Override
    public Health health() {
        if (!enabled) {
            return Health.up()
                    .withDetail("state", "DISABLED")
                    .withDetail("oracleAuthoritative", true)
                    .withDetail("message", "Neo4j Aura integration is disabled. Oracle is primary.")
                    .build();
        }
        if (isConnected()) {
            return Health.up()
                    .withDetail("state", "CONNECTED")
                    .withDetail("oracleAuthoritative", true)
                    .withDetail("uri", sanitizeUri(uri))
                    .withDetail("database", "neo4j")
                    .build();
        }
        return Health.down()
                .withDetail("state", "NOT_CONNECTED")
                .withDetail("oracleAuthoritative", true)
                .withDetail("uri", sanitizeUri(uri))
                .withDetail("message", sanitizeMessage(lastStatusMessage))
                .build();
    }

    @PreDestroy
    public void close() {
        if (driver != null) {
            try {
                driver.close();
                log.info("Neo4j driver successfully closed.");
            } catch (Exception e) {
                log.warn("Error closing Neo4j driver: {}", e.getMessage());
            }
        }
    }
}
