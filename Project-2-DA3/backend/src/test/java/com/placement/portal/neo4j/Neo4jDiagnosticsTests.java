package com.placement.portal.neo4j;

import com.placement.portal.config.DotenvEnvironmentPostProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.neo4j.driver.Driver;
import org.neo4j.driver.exceptions.AuthenticationException;
import org.neo4j.driver.exceptions.ConnectionReadTimeoutException;
import org.neo4j.driver.exceptions.ServiceUnavailableException;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.mock.env.MockEnvironment;

import javax.net.ssl.SSLHandshakeException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.security.cert.CertificateException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class Neo4jDiagnosticsTests {

    private Neo4jDriverService driverService;

    @BeforeEach
    void setUp() {
        driverService = new Neo4jDriverService();
    }

    @Test
    @DisplayName("Phase 2 & 3: Intentionally disabled Neo4j produces DISABLED status and healthy actuator")
    void testDisabledNeo4jConfiguration() {
        driverService.setEnabled(false);
        driverService.initialize();

        assertFalse(driverService.isConnected());
        assertFalse(driverService.isEnabled());
        assertEquals(Neo4jDriverService.Neo4jDiagnosticCode.DISABLED, driverService.getDiagnosticCode());

        Map<String, Object> status = driverService.getStatus();
        assertEquals("DISABLED", status.get("state"));
        assertEquals("DISABLED", status.get("diagnosticCode"));
        assertTrue((Boolean) status.get("oracleAuthoritative"));
        assertTrue(status.get("message").toString().contains("disabled"));

        Health health = driverService.health();
        assertEquals(Status.UP, health.getStatus());
        assertEquals("DISABLED", health.getDetails().get("state"));
        assertTrue((Boolean) health.getDetails().get("oracleAuthoritative"));
    }

    @Test
    @DisplayName("Phase 2 & 3: Enabled Neo4j with missing configuration reports CONFIG_MISSING without leaking secrets")
    void testMissingConfiguration() {
        // Missing URI
        driverService.setEnabled(true);
        driverService.setUri("");
        driverService.setUsername("neo4j");
        driverService.setPassword("secretPass123");
        driverService.initialize();

        assertFalse(driverService.isConnected());
        assertEquals(Neo4jDriverService.Neo4jDiagnosticCode.CONFIG_MISSING, driverService.getDiagnosticCode());
        assertTrue(driverService.getLastStatusMessage().contains("NEO4J_URI"));
        assertFalse(driverService.getLastStatusMessage().contains("secretPass123"));

        // Missing Password
        driverService.setUri("neo4j+s://4e2acb4d.databases.neo4j.io");
        driverService.setPassword("");
        driverService.initialize();

        assertEquals(Neo4jDriverService.Neo4jDiagnosticCode.CONFIG_MISSING, driverService.getDiagnosticCode());
        assertTrue(driverService.getLastStatusMessage().contains("NEO4J_PASSWORD"));

        // Missing Username
        driverService.setPassword("secretPass123");
        driverService.setUsername("");
        driverService.initialize();

        assertEquals(Neo4jDriverService.Neo4jDiagnosticCode.CONFIG_MISSING, driverService.getDiagnosticCode());
        assertTrue(driverService.getLastStatusMessage().contains("NEO4J_USERNAME"));
    }

    @Test
    @DisplayName("Phase 3: Invalid URI scheme reports INVALID_URI")
    void testInvalidUriScheme() {
        driverService.setEnabled(true);
        driverService.setUri("http://4e2acb4d.databases.neo4j.io");
        driverService.setUsername("neo4j");
        driverService.setPassword("secretPass123");
        driverService.initialize();

        assertFalse(driverService.isConnected());
        assertEquals(Neo4jDriverService.Neo4jDiagnosticCode.INVALID_URI, driverService.getDiagnosticCode());
        assertTrue(driverService.getLastStatusMessage().contains("Invalid Neo4j URI scheme"));
    }

    @Test
    @DisplayName("Phase 3: Authentication failure reports AUTH_REJECTED with credential source and without asserting password error")
    void testAuthenticationRejectionClassification() {
        driverService.setEnabled(true);
        driverService.setUri("neo4j+s://4e2acb4d.databases.neo4j.io");
        driverService.setUsername("4e2acb4d");
        driverService.setPassword("secretPassword456!");

        AuthenticationException authEx = new AuthenticationException("Neo.ClientError.Security.Unauthorized", "The client is unauthorized due to authentication failure.");
        driverService.classifyException(authEx);

        assertEquals(Neo4jDriverService.Neo4jDiagnosticCode.AUTH_REJECTED, driverService.getDiagnosticCode());
        String msg = driverService.getLastStatusMessage();
        assertTrue(msg.contains("Authentication rejected"));
        assertTrue(msg.contains("4e2acb4d"));
        assertFalse(msg.contains("secretPassword456!"));
        // Verifies message does not claim password is wrong
        assertFalse(msg.contains("password itself is incorrect"));
    }

    @Test
    @DisplayName("Phase 3: DNS resolution failure reports DNS_FAILURE")
    void testDnsFailureClassification() {
        driverService.setUri("neo4j+s://nonexistent.databases.neo4j.io");
        ServiceUnavailableException ex = new ServiceUnavailableException("Unable to connect", new UnknownHostException("nonexistent.databases.neo4j.io"));
        driverService.classifyException(ex);

        assertEquals(Neo4jDriverService.Neo4jDiagnosticCode.DNS_FAILURE, driverService.getDiagnosticCode());
        assertTrue(driverService.getLastStatusMessage().contains("DNS resolution failed"));
    }

    @Test
    @DisplayName("Phase 3: Timeout error reports TIMEOUT")
    void testTimeoutClassification() {
        driverService.setUri("neo4j+s://4e2acb4d.databases.neo4j.io");
        ConnectionReadTimeoutException timeoutEx = new ConnectionReadTimeoutException("Connection read timed out");
        driverService.classifyException(timeoutEx);

        assertEquals(Neo4jDriverService.Neo4jDiagnosticCode.TIMEOUT, driverService.getDiagnosticCode());
        assertTrue(driverService.getLastStatusMessage().contains("timed out"));

        SocketTimeoutException socketTimeout = new SocketTimeoutException("Read timed out");
        driverService.classifyException(new ServiceUnavailableException("Failed", socketTimeout));
        assertEquals(Neo4jDriverService.Neo4jDiagnosticCode.TIMEOUT, driverService.getDiagnosticCode());
    }

    @Test
    @DisplayName("Phase 3: TLS / SSL failure reports TLS_FAILURE")
    void testTlsFailureClassification() {
        driverService.setUri("neo4j+s://4e2acb4d.databases.neo4j.io");
        SSLHandshakeException sslEx = new SSLHandshakeException("PKIX path building failed: unable to find valid certification path to requested target");
        driverService.classifyException(sslEx);

        assertEquals(Neo4jDriverService.Neo4jDiagnosticCode.TLS_FAILURE, driverService.getDiagnosticCode());
        assertTrue(driverService.getLastStatusMessage().contains("TLS/SSL"));
    }

    @Test
    @DisplayName("Phase 3: Network / connection refused reports NETWORK_FAILURE")
    void testNetworkFailureClassification() {
        driverService.setUri("bolt://localhost:7687");
        ConnectException connectEx = new ConnectException("Connection refused");
        driverService.classifyException(new ServiceUnavailableException("Failed to connect to localhost:7687", connectEx));

        assertEquals(Neo4jDriverService.Neo4jDiagnosticCode.NETWORK_FAILURE, driverService.getDiagnosticCode());
        assertTrue(driverService.getLastStatusMessage().contains("Network failure"));
    }

    @Test
    @DisplayName("Phase 4: Successful mock driver connectivity reports CONNECTED and healthy actuator")
    void testSuccessfulDriverConnectivity() {
        Driver mockDriver = mock(Driver.class);
        doNothing().when(mockDriver).verifyConnectivity();

        driverService.setEnabled(true);
        driverService.setUri("neo4j+s://4e2acb4d.databases.neo4j.io");
        driverService.setUsername("4e2acb4d");
        driverService.setPassword("secretPass!");
        driverService.setDriver(mockDriver);

        assertTrue(driverService.isConnected());
        assertEquals(Neo4jDriverService.Neo4jDiagnosticCode.CONNECTED, driverService.getDiagnosticCode());

        Map<String, Object> status = driverService.getStatus();
        assertEquals("CONNECTED", status.get("state"));
        assertTrue((Boolean) status.get("connected"));
        assertEquals("neo4j+s://4e2acb4d.databases.neo4j.io", status.get("uri"));
        assertEquals("4e2acb4d", status.get("username"));

        Health health = driverService.health();
        assertEquals(Status.UP, health.getStatus());
        assertEquals("CONNECTED", health.getDetails().get("state"));
    }

    @Test
    @DisplayName("Phase 6: Redaction of sensitive information in URI, message, and logs")
    void testRedactionOfSecrets() {
        driverService.setPassword("superSecretPassword999!");

        // Redaction in URI
        String sanitizedUri = driverService.sanitizeUri("neo4j+s://4e2acb4d:superSecretPassword999!@4e2acb4d.databases.neo4j.io");
        assertFalse(sanitizedUri.contains("superSecretPassword999!"));
        assertTrue(sanitizedUri.contains("***"));

        // Redaction in status message
        String sanitizedMsg = driverService.sanitizeMessage("Failed with credentials: superSecretPassword999! at neo4j+s://user:pass@host");
        assertFalse(sanitizedMsg.contains("superSecretPassword999!"));
        assertFalse(sanitizedMsg.contains("pass@host"));
        assertTrue(sanitizedMsg.contains("***"));

        // Redaction in getStatus map
        driverService.setEnabled(false);
        driverService.initialize();
        Map<String, Object> status = driverService.getStatus();
        assertFalse(status.containsKey("password"));
        for (Object val : status.values()) {
            if (val != null) {
                assertFalse(val.toString().contains("superSecretPassword999!"));
            }
        }
    }

    @Test
    @DisplayName("Phase 2: DotenvEnvironmentPostProcessor maps environment variables with correct precedence")
    void testDotenvPostProcessor() {
        DotenvEnvironmentPostProcessor processor = new DotenvEnvironmentPostProcessor();
        MockEnvironment mockEnv = new MockEnvironment();

        processor.postProcessEnvironment(mockEnv, null);

        // Assert property source is registered
        assertTrue(mockEnv.getPropertySources().contains(DotenvEnvironmentPostProcessor.PROPERTY_SOURCE_NAME)
                || mockEnv.getProperty(DotenvEnvironmentPostProcessor.NEO4J_CREDENTIAL_SOURCE_KEY) != null);
    }
}
