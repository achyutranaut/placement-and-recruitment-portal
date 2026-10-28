package com.placement.portal.vector;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
public class SemanticSearchSecurityTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Security: Unauthenticated access to semantic search is blocked (401)")
    void testUnauthenticatedAccessBlocked() throws Exception {
        SemanticSearchRequest req = new SemanticSearchRequest("Spring Boot Java", "ALL", 10, 0.2f);
        mockMvc.perform(post("/api/v1/semantic-search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Security: Students are forbidden from triggering reindexing (403)")
    @WithMockUser(username = "student1", roles = {"STUDENT"})
    void testStudentReindexForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/semantic-search/index"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Security: Recruiters are forbidden from triggering full reindexing (403)")
    @WithMockUser(username = "recruiter1", roles = {"RECRUITER"})
    void testRecruiterReindexForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/semantic-search/index"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Security: Authenticated users can inspect Qdrant status endpoint")
    @WithMockUser(username = "student1", roles = {"STUDENT"})
    void testAuthenticatedStatusCheckAllowed() throws Exception {
        mockMvc.perform(get("/api/v1/semantic-search/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.collection").value("placement-semantic-search"))
                .andExpect(jsonPath("$.data.embeddingDimension").value(384));
    }
}
