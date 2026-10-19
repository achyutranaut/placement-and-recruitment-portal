package com.placement.portal.neo4j;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.placement.portal.neo4j.dto.GraphMutationDto.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("neo4j")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class Neo4jAdminAndGraphSecurityTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @Order(1)
    @DisplayName("Admin Dashboard: Accessible to Admin, Blocked for Student with 403")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testAdminDashboardAccessForAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.stats.totalStudents").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.totalPrograms").value(greaterThanOrEqualTo(1)));
    }

    @Test
    @Order(2)
    @DisplayName("Admin Dashboard: 403 Forbidden for Student")
    @WithMockUser(username = "student1", roles = {"STUDENT"})
    void testAdminDashboardForbiddenForStudent() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(3)
    @DisplayName("Admin Audit: Real Audit records returned under Neo4j profile")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testAdminAuditLogsAccessibleForAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/admin/audit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @Order(4)
    @DisplayName("Admin Audit: 403 Forbidden for Student")
    @WithMockUser(username = "student1", roles = {"STUDENT"})
    void testAdminAuditForbiddenForStudent() throws Exception {
        mockMvc.perform(get("/api/v1/admin/audit"))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(5)
    @DisplayName("Graph Mutation Security: Student attempting to create node receives 403 Forbidden")
    @WithMockUser(username = "student1", roles = {"STUDENT"})
    void testStudentCannotCreateGraphNode() throws Exception {
        CreateEntityRequestDto req = new CreateEntityRequestDto("Company", Map.of(
                "companyName", "Malicious Injected Corp",
                "industry", "Hacking"
        ));

        mockMvc.perform(post("/api/v1/graph/entities")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(6)
    @DisplayName("Graph Mutation Security: Student attempting to create relationship receives 403 Forbidden")
    @WithMockUser(username = "student1", roles = {"STUDENT"})
    void testStudentCannotCreateRelationship() throws Exception {
        CreateRelationshipRequestDto req = new CreateRelationshipRequestDto(
                "Student", "STU023", "Offer", "OFF001", "RECEIVED_OFFER"
        );

        mockMvc.perform(post("/api/v1/graph/relationships")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(7)
    @DisplayName("Graph Mutation: Admin can create, update, connect and delete a node safely")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testAdminCanPerformGraphCrudLifecycle() throws Exception {
        String testCompId = "COM_TEST_SEC_" + System.currentTimeMillis();

        // 1. Create Node
        CreateEntityRequestDto createReq = new CreateEntityRequestDto("Company", Map.of(
                "companyId", testCompId,
                "companyName", "Secure Automated Testing Labs",
                "industry", "Security Software",
                "email", "testing@securelabs.io"
        ));

        mockMvc.perform(post("/api/v1/graph/entities")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.companyId").value(testCompId));

        // 2. Update Node
        UpdateEntityRequestDto updateReq = new UpdateEntityRequestDto(Map.of(
                "companyName", "Secure Automated Testing Labs Updated",
                "industry", "Cybersecurity & Graph AI"
        ));

        mockMvc.perform(put("/api/v1/graph/entities/Company/" + testCompId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 3. Connect Node to existing Drive
        CreateRelationshipRequestDto relReq = new CreateRelationshipRequestDto(
                "Company", testCompId, "Drive", "DRV001", "HOSTS_DRIVE"
        );

        mockMvc.perform(post("/api/v1/graph/relationships")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(relReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 4. Delete Relationship
        DeleteRelationshipRequestDto delRelReq = new DeleteRelationshipRequestDto(
                "Company", testCompId, "Drive", "DRV001", "HOSTS_DRIVE"
        );

        mockMvc.perform(delete("/api/v1/graph/relationships")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(delRelReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 5. Delete Node
        mockMvc.perform(delete("/api/v1/graph/entities/Company/" + testCompId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @Order(8)
    @DisplayName("Integrity Guard: Attempt to delete Student with active application is blocked")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testDeleteStudentWithActiveApplicationsBlocked() throws Exception {
        // STU023 has active applications (APP003, APP036)
        mockMvc.perform(delete("/api/v1/graph/entities/Student/STU023"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("Integrity Violation")));
    }
}
