package com.placement.portal.recruitment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.placement.portal.auth.AuthResponse;
import com.placement.portal.auth.LoginRequest;
import com.placement.portal.auth.RecruiterRegisterRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
public class RecruiterAuthAndCompanyIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String loginAndGetToken(String username, String password) throws Exception {
        LoginRequest req = new LoginRequest(username, password);
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        Map<String, Object> map = objectMapper.readValue(responseBody, Map.class);
        Map<String, Object> data = (Map<String, Object>) map.get("data");
        return (String) data.get("token");
    }

    @Test
    @DisplayName("TEST 1: Recruiter login succeeds using username, email, or full name")
    void testRecruiterLoginVariations() throws Exception {
        // 1. By username
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("recruiter1", "password123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("recruiter1"))
                .andExpect(jsonPath("$.data.role").value("ROLE_RECRUITER"))
                .andExpect(jsonPath("$.data.name").value("Vikram Singh"));

        // 2. By email
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("vikram.singh@microsoft.com", "password123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("recruiter1"))
                .andExpect(jsonPath("$.data.name").value("Vikram Singh"));

        // 3. By full name
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("Vikram Singh", "password123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("recruiter1"));
    }

    @Test
    @DisplayName("TEST 2: Invalid password rejected with 401 Bad Credentials")
    void testRecruiterInvalidPassword() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("recruiter1", "wrong_password"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("TEST 3 & 4: Recruiter companies list returns only authorized companies")
    void testRecruiterAuthorizedCompaniesList() throws Exception {
        // recruiter1 is authorized for COM004 (Microsoft) and COM001 (TCS)
        String recruiter1Token = loginAndGetToken("recruiter1", "password123");

        mockMvc.perform(get("/api/v1/recruiter/companies")
                        .header("Authorization", "Bearer " + recruiter1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[*].companyId", containsInAnyOrder("COM004", "COM001")));

        // recruiter2 is authorized ONLY for COM001 (TCS)
        String recruiter2Token = loginAndGetToken("recruiter2", "password123");

        mockMvc.perform(get("/api/v1/recruiter/companies")
                        .header("Authorization", "Bearer " + recruiter2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].companyId").value("COM001"));
    }

    @Test
    @DisplayName("TEST 5: Recruiter selects authorized company successfully")
    void testRecruiterSelectAuthorizedCompany() throws Exception {
        String token = loginAndGetToken("recruiter1", "password123");

        mockMvc.perform(post("/api/v1/recruiter/company/select")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("companyId", "COM001"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.company.companyId").value("COM001"))
                .andExpect(jsonPath("$.data.company.companyName").value("Tata Consultancy Services"))
                .andExpect(jsonPath("$.data.token", notNullValue()));
    }

    @Test
    @DisplayName("TEST 6 & 7: Recruiter cannot select or view data for unauthorized company (403 Forbidden)")
    void testRecruiterUnauthorizedCompanyAccess() throws Exception {
        // recruiter2 is authorized only for COM001 (TCS), NOT COM004 (Microsoft)
        String recruiter2Token = loginAndGetToken("recruiter2", "password123");

        // Attempting to select COM004 must be rejected
        mockMvc.perform(post("/api/v1/recruiter/company/select")
                        .header("Authorization", "Bearer " + recruiter2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("companyId", "COM004"))))
                .andExpect(status().isForbidden());

        // Attempting to query roles for unauthorized company COM004 must be rejected
        mockMvc.perform(get("/api/v1/recruiter/roles?companyId=COM004")
                        .header("Authorization", "Bearer " + recruiter2Token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("TEST 8: Recruiter self-registration links chosen company and returns 201 CREATED")
    void testRecruiterRegistration() throws Exception {
        // 1. Register with company (COM002 - Infosys) - should return 201 CREATED
        RecruiterRegisterRequest reqWithCompany = new RecruiterRegisterRequest(
                "new_recruiter_" + System.currentTimeMillis(),
                "password123",
                "Anita Desai",
                "anita." + System.currentTimeMillis() + "@infosys.com",
                "COM002"
        );

        MvcResult result = mockMvc.perform(post("/api/v1/auth/recruiter/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqWithCompany)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.username").value(reqWithCompany.getUsername()))
                .andExpect(jsonPath("$.data.role").value("ROLE_RECRUITER"))
                .andExpect(jsonPath("$.data.fullName").value("Anita Desai"))
                .andExpect(jsonPath("$.data.companyId").value("COM002"))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        Map<String, Object> map = objectMapper.readValue(responseBody, Map.class);
        Map<String, Object> data = (Map<String, Object>) map.get("data");
        String newToken = (String) data.get("token");

        // The newly registered recruiter is immediately authorized for their chosen company (COM002)
        mockMvc.perform(get("/api/v1/recruiter/companies")
                        .header("Authorization", "Bearer " + newToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].companyId").value("COM002"));

        // 2. Register without specifying a company should fail validation (companyId is @NotBlank)
        RecruiterRegisterRequest reqNoCompany = new RecruiterRegisterRequest(
                "unassigned_" + System.currentTimeMillis(),
                "password123",
                "Unassigned Recruiter",
                "unassigned." + System.currentTimeMillis() + "@recruiter.org",
                null
        );

        mockMvc.perform(post("/api/v1/auth/recruiter/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqNoCompany)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("TEST 10: Any organization recruiter (Amazon, Oracle, Infosys) can log in and represent their company")
    void testAnyCompanyRecruiterLogin() throws Exception {
        // Amazon recruiter login
        String amazonToken = loginAndGetToken("amazon_recruiter", "password123");
        mockMvc.perform(get("/api/v1/recruiter/companies")
                        .header("Authorization", "Bearer " + amazonToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].companyId").value("COM005"))
                .andExpect(jsonPath("$.data[0].companyName").value("Amazon Development Centre"));

        // Oracle recruiter login
        String oracleToken = loginAndGetToken("oracle_recruiter", "password123");
        mockMvc.perform(get("/api/v1/recruiter/companies")
                        .header("Authorization", "Bearer " + oracleToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].companyId").value("COM006"))
                .andExpect(jsonPath("$.data[0].companyName").value("Oracle India Pvt Ltd"));
    }

    @Test
    @DisplayName("TEST 9: Admin can list recruiters, assign company authorization, and revoke authorization")
    void testAdminRecruiterManagement() throws Exception {
        String adminToken = loginAndGetToken("admin", "admin123");

        // 1. List recruiters
        mockMvc.perform(get("/api/v1/admin/recruiters")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", not(empty())));

        // 2. Assign COM002 (Infosys) to recruiter2 (USR004)
        mockMvc.perform(post("/api/v1/admin/recruiters/USR004/companies/COM002")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        // 3. Now recruiter2 can access COM002
        String recruiter2Token = loginAndGetToken("recruiter2", "password123");
        mockMvc.perform(get("/api/v1/recruiter/companies")
                        .header("Authorization", "Bearer " + recruiter2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].companyId", hasItem("COM002")));

        // 4. Revoke COM002 from USR004
        mockMvc.perform(delete("/api/v1/admin/recruiters/USR004/companies/COM002")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }
}
