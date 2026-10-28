package com.placement.portal.neo4j;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.neo4j.driver.*;
import org.neo4j.driver.exceptions.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Service;

import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import java.security.cert.CertificateException;

/**
 * Manages the official Neo4j Java Driver lifecycle for Neo4j Aura cloud integration.
 * Operates gracefully: when disabled or Aura is unreachable, Oracle operations continue
 * without interruption.
 */
@Service("neo4jHealthIndicator")
public class Neo4jDriverService implements HealthIndicator {

    private static final Logger log = LoggerFactory.getLogger(Neo4jDriverService.class);

    public enum Neo4jDiagnosticCode {
        DISABLED,
        CONFIG_MISSING,
        INVALID_URI,
        DNS_FAILURE,
        NETWORK_FAILURE,
        TIMEOUT,
        TLS_FAILURE,
        AUTH_REJECTED,
        AUTH_EXPIRED,
        QUERY_AUTH_FAILURE,
        UNKNOWN_ERROR,
        CONNECTED
    }

    @Value("${portal.neo4j.enabled:false}")
    private boolean enabled;

    @Value("${portal.neo4j.uri:}")
    private String uri;

    @Value("${portal.neo4j.username:}")
    private String username;

    @Value("${portal.neo4j.password:}")
    private String password;

    @Value("${portal.neo4j.credential-source:}")
    private String configuredCredentialSource;

    private Driver driver;
    private volatile boolean connected = false;
    private volatile Neo4jDiagnosticCode diagnosticCode = Neo4jDiagnosticCode.DISABLED;
    private volatile String lastStatusMessage = "Not initialized";
    private volatile String credentialSource = "Unresolved";

    public Neo4jDriverService() {
    }

    // Testing constructor
    public Neo4jDriverService(boolean enabled, String uri, String username, String password, String credentialSource) {
        this.enabled = enabled;
        this.uri = uri;
        this.username = username;
        this.password = password;
        this.configuredCredentialSource = credentialSource;
    }

    @PostConstruct
    public synchronized void initialize() {
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

        // Resolve credential source description
        if (this.configuredCredentialSource != null && !this.configuredCredentialSource.isBlank()) {
            this.credentialSource = this.configuredCredentialSource;
        } else if (System.getenv("NEO4J_PASSWORD") != null || System.getenv("NEO4J_URI") != null) {
            this.credentialSource = "System Environment Variable (shell/Docker/IDE)";
        } else if (System.getProperty("portal.neo4j.password") != null || System.getProperty("NEO4J_PASSWORD") != null) {
            this.credentialSource = "JVM System Property";
        } else {
            this.credentialSource = "application configuration / .env";
        }

        // Close any pre-existing driver instance cleanly before re-initializing
        cleanupDriver();

        if (!enabled) {
            this.diagnosticCode = Neo4jDiagnosticCode.DISABLED;
            this.lastStatusMessage = "Neo4j Aura integration is disabled (portal.neo4j.enabled=false). Oracle Database 23ai is authoritative.";
            log.info("Neo4j Aura integration is disabled. Oracle is the primary and sole active database.");
            return;
        }

        // Validate required configuration fields
        List<String> missingFields = new ArrayList<>();
        if (uri == null || uri.isBlank()) {
            missingFields.add("NEO4J_URI");
        }
        if (username == null || username.isBlank()) {
            missingFields.add("NEO4J_USERNAME");
        }
        if (password == null || password.isBlank()) {
            missingFields.add("NEO4J_PASSWORD");
        }

        if (!missingFields.isEmpty()) {
            this.diagnosticCode = Neo4jDiagnosticCode.CONFIG_MISSING;
            this.lastStatusMessage = "Neo4j is enabled but required configuration is missing: "
                    + String.join(", ", missingFields) + " (credential source: " + credentialSource + ").";
            log.warn("Neo4j is enabled but required fields are missing: {}. Check configuration.", missingFields);
            return;
        }

        // Validate URI format and scheme
        if (!isValidNeo4jUri(uri)) {
            this.diagnosticCode = Neo4jDiagnosticCode.INVALID_URI;
            this.lastStatusMessage = "Invalid Neo4j URI scheme in '" + sanitizeUri(uri)
                    + "'. Expected 'neo4j+s://<instance-id>.databases.neo4j.io', 'neo4j+ssc://', or 'bolt://'.";
            log.warn("Invalid Neo4j URI scheme configured: {}", sanitizeUri(uri));
            return;
        }

        try {
            Config config = Config.builder()
                    .withMaxConnectionPoolSize(10)
                    .withConnectionTimeout(15, TimeUnit.SECONDS)
                    .build();

            this.driver = createDriver(uri, AuthTokens.basic(username, password), config);
            // Verify connectivity without blocking indefinitely
            driver.verifyConnectivity();
            this.connected = true;
            this.diagnosticCode = Neo4jDiagnosticCode.CONNECTED;
            this.lastStatusMessage = "Connected to Neo4j (" + sanitizeUri(uri) + ")";
            log.info("Successfully connected to Neo4j at {} (User: {})", sanitizeUri(uri), sanitizeUsername(username));
        } catch (Exception e) {
            // Cleanly close and release driver resources on connectivity failure
            cleanupDriver();
            classifyException(e);
            log.warn("Could not connect to Neo4j at {}: [{}]. Oracle remains authoritative and fully operational.",
                    sanitizeUri(uri), sanitizeMessage(lastStatusMessage));
        }
    }

    /**
     * Factory method for Driver creation (can be overridden in tests).
     */
    protected Driver createDriver(String uri, AuthToken authToken, Config config) {
        return GraphDatabase.driver(uri, authToken, config);
    }

    public synchronized boolean reconnect() {
        cleanupDriver();
        initialize();
        return isConnected();
    }

    private void cleanupDriver() {
        if (this.driver != null) {
            try {
                this.driver.close();
            } catch (Exception e) {
                log.debug("Error while closing driver: {}", e.getMessage());
            }
            this.driver = null;
        }
        this.connected = false;
    }

    private boolean isValidNeo4jUri(String rawUri) {
        if (rawUri == null) return false;
        String trimmed = rawUri.trim().toLowerCase();
        return trimmed.startsWith("neo4j://")
                || trimmed.startsWith("neo4j+s://")
                || trimmed.startsWith("neo4j+ssc://")
                || trimmed.startsWith("bolt://")
                || trimmed.startsWith("bolt+s://")
                || trimmed.startsWith("bolt+ssc://");
    }

    /**
     * Classifies driver and network exceptions into safe, specific diagnostic codes and messages.
     */
    public void classifyException(Throwable e) {
        Throwable cause = findRootCause(e);
        String causeMsg = (cause != null && cause.getMessage() != null) ? cause.getMessage() : "";
        String fullMsg = (e.getMessage() != null) ? e.getMessage() : "";

        if (e instanceof AuthenticationException || cause instanceof AuthenticationException) {
            this.diagnosticCode = Neo4jDiagnosticCode.AUTH_REJECTED;
            this.lastStatusMessage = "Authentication rejected: Neo4j Aura rejected the supplied credentials for username '"
                    + sanitizeUsername(username) + "' (credential source: " + credentialSource
                    + "). The instance is online, but authentication failed. Please verify username and password in .env.";
        } else if (e instanceof TokenExpiredException || e instanceof AuthorizationExpiredException
                || cause instanceof TokenExpiredException || cause instanceof AuthorizationExpiredException) {
            this.diagnosticCode = Neo4jDiagnosticCode.AUTH_EXPIRED;
            this.lastStatusMessage = "Authentication or authorization token expired for Neo4j Aura.";
        } else if (e instanceof SSLHandshakeException || cause instanceof SSLHandshakeException
                || e instanceof CertificateException || cause instanceof CertificateException
                || e instanceof UntrustedServerException || cause instanceof UntrustedServerException
                || fullMsg.contains("SSL") || fullMsg.contains("PKIX") || causeMsg.contains("PKIX")) {
            this.diagnosticCode = Neo4jDiagnosticCode.TLS_FAILURE;
            this.lastStatusMessage = "TLS/SSL certificate handshake failed while connecting to Neo4j Aura: " + sanitizeMessage(causeMsg);
        } else if (e instanceof UnknownHostException || cause instanceof UnknownHostException
                || fullMsg.contains("UnknownHostException") || causeMsg.contains("UnknownHostException")) {
            this.diagnosticCode = Neo4jDiagnosticCode.DNS_FAILURE;
            this.lastStatusMessage = "DNS resolution failed: hostname in '" + sanitizeUri(uri) + "' could not be resolved.";
        } else if (e instanceof SocketTimeoutException || cause instanceof SocketTimeoutException
                || e instanceof TimeoutException || cause instanceof TimeoutException
                || e instanceof ConnectionReadTimeoutException || cause instanceof ConnectionReadTimeoutException
                || fullMsg.toLowerCase().contains("timed out") || causeMsg.toLowerCase().contains("timed out")) {
            this.diagnosticCode = Neo4jDiagnosticCode.TIMEOUT;
            this.lastStatusMessage = "Connection timed out connecting to Neo4j Aura at '" + sanitizeUri(uri) + "' (timeout: 15s).";
        } else if (e instanceof ConnectException || cause instanceof ConnectException
                || e instanceof NoRouteToHostException || cause instanceof NoRouteToHostException
                || fullMsg.contains("Connection refused") || causeMsg.contains("Connection refused")) {
            this.diagnosticCode = Neo4jDiagnosticCode.NETWORK_FAILURE;
            this.lastStatusMessage = "Network failure: Unable to establish TCP connection to '" + sanitizeUri(uri) + "'. Check network, hostel Wi-Fi, or firewall settings.";
        } else if (e instanceof ServiceUnavailableException || cause instanceof ServiceUnavailableException) {
            this.diagnosticCode = Neo4jDiagnosticCode.NETWORK_FAILURE;
            this.lastStatusMessage = "Neo4j Aura service is unavailable at '" + sanitizeUri(uri) + "': " + sanitizeMessage(fullMsg);
        } else if (e instanceof IllegalArgumentException || cause instanceof IllegalArgumentException) {
            this.diagnosticCode = Neo4jDiagnosticCode.INVALID_URI;
            this.lastStatusMessage = "Invalid Neo4j URI: " + sanitizeUri(uri) + " - " + sanitizeMessage(fullMsg);
        } else {
            this.diagnosticCode = Neo4jDiagnosticCode.UNKNOWN_ERROR;
            this.lastStatusMessage = "Connection failed to Neo4j Aura: " + sanitizeMessage(fullMsg);
        }
    }

    private Throwable findRootCause(Throwable throwable) {
        Throwable root = throwable;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root;
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
            throw new IllegalStateException("Neo4j Aura is currently unavailable (" + diagnosticCode + "): " + lastStatusMessage);
        }
        return config != null ? driver.session(config) : driver.session();
    }

    public Driver getDriver() {
        return driver;
    }

    public Neo4jDiagnosticCode getDiagnosticCode() {
        return diagnosticCode;
    }

    public String getLastStatusMessage() {
        return lastStatusMessage;
    }

    public String getCredentialSource() {
        return credentialSource;
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
        status.put("diagnosticCode", diagnosticCode != null ? diagnosticCode.name() : state);
        status.put("uri", sanitizeUri(uri));
        status.put("username", sanitizeUsername(username));
        status.put("database", "neo4j");
        status.put("message", sanitizeMessage(lastStatusMessage));
        status.put("credentialSource", credentialSource);
        status.put("oracleAuthoritative", true);
        return status;
    }

    public String sanitizeUri(String rawUri) {
        if (rawUri == null || rawUri.isBlank()) return "Not configured";
        // Redact credentials if URI has format scheme://user:pass@host
        return rawUri.replaceAll("://[^:@/]+:[^@/]+@", "://***:***@")
                .replaceAll("://[^@/]+@", "://***@");
    }

    public String sanitizeUsername(String rawUser) {
        if (rawUser == null || rawUser.isBlank()) return "Not configured";
        return rawUser.trim();
    }

    public String sanitizeMessage(String msg) {
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
                    .withDetail("diagnosticCode", Neo4jDiagnosticCode.DISABLED.name())
                    .withDetail("oracleAuthoritative", true)
                    .withDetail("message", "Neo4j Aura integration is disabled. Oracle Database 23ai is primary.")
                    .build();
        }
        if (isConnected()) {
            return Health.up()
                    .withDetail("state", "CONNECTED")
                    .withDetail("diagnosticCode", Neo4jDiagnosticCode.CONNECTED.name())
                    .withDetail("oracleAuthoritative", true)
                    .withDetail("uri", sanitizeUri(uri))
                    .withDetail("username", sanitizeUsername(username))
                    .withDetail("database", "neo4j")
                    .build();
        }
        return Health.down()
                .withDetail("state", "NOT_CONNECTED")
                .withDetail("diagnosticCode", diagnosticCode != null ? diagnosticCode.name() : "ERROR")
                .withDetail("oracleAuthoritative", true)
                .withDetail("uri", sanitizeUri(uri))
                .withDetail("username", sanitizeUsername(username))
                .withDetail("credentialSource", credentialSource)
                .withDetail("message", sanitizeMessage(lastStatusMessage))
                .build();
    }

    @PreDestroy
    public void close() {
        cleanupDriver();
        log.info("Neo4j driver successfully closed.");
    }

    // Setters for test harness
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setDriver(Driver driver) {
        this.driver = driver;
        this.connected = (driver != null);
        if (this.connected) {
            this.diagnosticCode = Neo4jDiagnosticCode.CONNECTED;
            this.lastStatusMessage = "Connected to Neo4j (" + sanitizeUri(uri) + ")";
        }
    }
}
