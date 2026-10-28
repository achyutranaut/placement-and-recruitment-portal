package com.placement.portal.neo4j;

import com.placement.portal.admin.compiler.DatabaseConnectionInfoDto;
import com.placement.portal.admin.compiler.SqlExecuteResponseDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class Neo4jCoexistenceTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Neo4jDriverService driverService;

    @Autowired
    private Neo4jCypherService cypherService;

    @Autowired
    private Neo4jProjectionService projectionService;

    @Autowired
    private Neo4jGraphService graphService;

    @Test
    void testNeo4jDefaultDisabledAndGracefulDegradation() {
        // By default in test/h2 environment, portal.neo4j.enabled is false
        assertFalse(driverService.isConnected());

        Map<String, Object> status = driverService.getStatus();
        assertNotNull(status);
        assertFalse((Boolean) status.get("connected"));
        assertEquals(true, status.get("oracleAuthoritative"));
    }

    @Test
    void testCypherExecuteFailsGracefullyWhenDisabled() {
        SqlExecuteResponseDto response = cypherService.execute("MATCH (n) RETURN n LIMIT 10");
        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals("NEO4J-DISABLED", response.getErrorCode());
        assertTrue(response.getErrorMessage().contains("disabled"));
    }

    @Test
    void testProjectionFailsGracefullyWhenDisabled() {
        Neo4jProjectionService.ProjectionSummary summary = projectionService.syncOracleToNeo4j();
        assertNotNull(summary);
        assertFalse(summary.success());
        assertTrue(summary.message().contains("disabled"));
    }

    @Test
    void testGraphTraversalFailsGracefullyWhenDisabled() {
        var journey = graphService.getPlacementPath("STU001");
        assertNotNull(journey);
        assertTrue(journey.isEmpty());

        Map<String, Object> elements = graphService.getWholeGraph(10);
        assertNotNull(elements);
        assertEquals("UNAVAILABLE", elements.get("status"));
    }

    @Test
    void testCypherConnectionInfo() {
        DatabaseConnectionInfoDto info = cypherService.getConnectionInfo();
        assertNotNull(info);
        assertEquals("Neo4j Aura Cloud Graph Database", info.getDatabaseProduct());
        assertEquals("DISABLED", info.getStatus());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testAdminEndpointsAccess() throws Exception {
        mockMvc.perform(get("/api/v1/graph/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.oracleAuthoritative").value(true));

        mockMvc.perform(post("/api/v1/admin/cypher/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sql\":\"MATCH (s:Student) RETURN s\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.success").value(false))
                .andExpect(jsonPath("$.data.errorCode").value("NEO4J-DISABLED"));

        mockMvc.perform(get("/api/v1/admin/cypher/connection-info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DISABLED"));

        mockMvc.perform(post("/api/v1/graph/sync"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.success").value(false));

        // SEC-01: Admin can retrieve whole graph topology elements
        mockMvc.perform(get("/api/v1/graph/elements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void testStudentCannotTriggerSyncOrCypher() throws Exception {
        mockMvc.perform(post("/api/v1/graph/sync"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/admin/cypher/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sql\":\"MATCH (s:Student) RETURN s\"}"))
                .andExpect(status().isForbidden());

        // SEC-01: Student cannot retrieve whole graph topology elements
        mockMvc.perform(get("/api/v1/graph/elements"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "recruiter1", roles = "RECRUITER")
    void testRecruiterCannotAccessGlobalGraphElements() throws Exception {
        // SEC-01: Recruiter cannot retrieve whole graph topology elements
        mockMvc.perform(get("/api/v1/graph/elements"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testUnauthenticatedCallerCannotAccessGlobalGraphElements() throws Exception {
        // SEC-01: Unauthenticated caller cannot retrieve whole graph topology elements
        mockMvc.perform(get("/api/v1/graph/elements"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "student1", roles = "STUDENT")
    void testStudentCanAccessOwnJourney() throws Exception {
        mockMvc.perform(get("/api/v1/graph/journey/STU001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @WithMockUser(username = "student1", roles = "STUDENT")
    void testCrossStudentAccessForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/graph/journey/STU002"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/graph/eligible-drives/STU002"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/graph/recommendations/STU002"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "recruiter1", roles = "RECRUITER")
    void testRecruiterStudentJourneyScoping() throws Exception {
        // STU003 applied to Microsoft COM004 drive (DRV004), so recruiter1 is authorized
        mockMvc.perform(get("/api/v1/graph/journey/STU003"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());

        // STU014 applied only to Wipro (COM003 / DRV003); recruiter1 is NOT authorized for COM003
        mockMvc.perform(get("/api/v1/graph/journey/STU014"))
                .andExpect(status().isForbidden());

        // Recruiter cannot access eligible drives for unauthorized student
        mockMvc.perform(get("/api/v1/graph/eligible-drives/STU014"))
                .andExpect(status().isForbidden());

        // Recruiter is forbidden from peer recommendations endpoint entirely
        mockMvc.perform(get("/api/v1/graph/recommendations/STU003"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testAdminGraphMutationFailsGracefullyWhenDisabled() throws Exception {
        mockMvc.perform(put("/api/v1/graph/nodes/Application/APP001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"properties\":{\"applicationStatus\":\"OFFERED\"}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("disabled or unreachable")));

        mockMvc.perform(delete("/api/v1/graph/nodes/Application/APP001"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("disabled or unreachable")));

        mockMvc.perform(post("/api/v1/graph/nodes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"label\":\"Company\",\"properties\":{\"companyName\":\"Test Co\"}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("disabled or unreachable")));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void testStudentCannotMutateGraph() throws Exception {
        mockMvc.perform(put("/api/v1/graph/nodes/Application/APP001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"properties\":{\"applicationStatus\":\"OFFERED\"}}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/v1/graph/nodes/Application/APP001"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/graph/nodes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"label\":\"Company\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/graph/relationships")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fromLabel\":\"Company\",\"fromId\":\"COM001\",\"toLabel\":\"Drive\",\"toId\":\"DRV001\"}"))
                .andExpect(status().isForbidden());
    }
}
