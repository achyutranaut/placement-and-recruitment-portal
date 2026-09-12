package com.placement.portal.admin.compiler;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
@Transactional
class SqlCompilerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SqlCompilerService sqlCompilerService;

    @Test
    void testScriptParserWithStandardSql() {
        String script = "SELECT * FROM STUDENT; SELECT * FROM PROGRAM;";
        List<String> stmts = SqlScriptParser.splitScript(script);
        assertEquals(2, stmts.size());
        assertEquals("SELECT * FROM STUDENT", stmts.get(0));
        assertEquals("SELECT * FROM PROGRAM", stmts.get(1));
    }

    @Test
    void testScriptParserWithPlsqlBlock() {
        String script = """
            CREATE TABLE TEST_T (ID INT);
            BEGIN
                NULL;
            END;
            /
            SELECT * FROM TEST_T;
            """;
        List<String> stmts = SqlScriptParser.splitScript(script);
        assertEquals(3, stmts.size());
        assertTrue(stmts.get(0).startsWith("CREATE TABLE"));
        assertTrue(stmts.get(1).startsWith("BEGIN"));
        assertTrue(stmts.get(2).startsWith("SELECT"));
    }

    @Test
    void testExecuteSelectQuery() {
        SqlExecuteRequestDto req = new SqlExecuteRequestDto("SELECT 1 AS TEST_COL FROM DUAL", "QUERY");
        SqlExecuteResponseDto resp = sqlCompilerService.execute(req);

        assertNotNull(resp);
        assertTrue(resp.isSuccess());
        assertEquals("SELECT", resp.getStatementType());
        assertNotNull(resp.getColumns());
        assertFalse(resp.getColumns().isEmpty());
    }

    @Test
    void testExecuteDdlAndDml() {
        // 1. Create table
        SqlExecuteResponseDto createResp = sqlCompilerService.execute(new SqlExecuteRequestDto(
                "CREATE TABLE COMPILER_UNIT_TEST (ID INT PRIMARY KEY, VAL VARCHAR(50))", "QUERY"
        ));
        assertTrue(createResp.isSuccess());

        // 2. Insert row
        SqlExecuteResponseDto insertResp = sqlCompilerService.execute(new SqlExecuteRequestDto(
                "INSERT INTO COMPILER_UNIT_TEST VALUES (1, 'Hello')", "QUERY"
        ));
        assertTrue(insertResp.isSuccess());
        assertEquals(1, insertResp.getRowsAffected());

        // 3. Update row
        SqlExecuteResponseDto updateResp = sqlCompilerService.execute(new SqlExecuteRequestDto(
                "UPDATE COMPILER_UNIT_TEST SET VAL = 'World' WHERE ID = 1", "QUERY"
        ));
        assertTrue(updateResp.isSuccess());
        assertEquals(1, updateResp.getRowsAffected());

        // 4. Select row
        SqlExecuteResponseDto selectResp = sqlCompilerService.execute(new SqlExecuteRequestDto(
                "SELECT * FROM COMPILER_UNIT_TEST", "QUERY"
        ));
        assertTrue(selectResp.isSuccess());
        assertEquals(1, selectResp.getRowCount());

        // 5. Delete row
        SqlExecuteResponseDto deleteResp = sqlCompilerService.execute(new SqlExecuteRequestDto(
                "DELETE FROM COMPILER_UNIT_TEST WHERE ID = 1", "QUERY"
        ));
        assertTrue(deleteResp.isSuccess());
        assertEquals(1, deleteResp.getRowsAffected());

        // 6. Drop table
        SqlExecuteResponseDto dropResp = sqlCompilerService.execute(new SqlExecuteRequestDto(
                "DROP TABLE COMPILER_UNIT_TEST", "QUERY"
        ));
        assertTrue(dropResp.isSuccess());
    }

    @Test
    void testExecuteInvalidSql() {
        SqlExecuteRequestDto req = new SqlExecuteRequestDto("SELECT * FROM NON_EXISTENT_TABLE_XYZ", "QUERY");
        SqlExecuteResponseDto resp = sqlCompilerService.execute(req);

        assertNotNull(resp);
        assertFalse(resp.isSuccess());
        assertEquals("ERROR", resp.getStatementType());
        assertNotNull(resp.getErrorCode());
        assertNotNull(resp.getErrorMessage());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testAdminAccessAllowed() throws Exception {
        mockMvc.perform(get("/api/v1/admin/sql/connection-info"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/admin/sql/schema"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/sql/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sql\":\"SELECT 1 FROM DUAL\",\"mode\":\"QUERY\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void testStudentAccessForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/sql/connection-info"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/admin/sql/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sql\":\"SELECT 1 FROM DUAL\",\"mode\":\"QUERY\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "RECRUITER")
    void testRecruiterAccessForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/sql/connection-info"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/admin/sql/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sql\":\"SELECT 1 FROM DUAL\",\"mode\":\"QUERY\"}"))
                .andExpect(status().isForbidden());
    }
}
