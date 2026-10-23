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
    void testStrictDataIntegrityValidationMissingNotNull() {
        // Create table with NOT NULL column
        sqlCompilerService.execute(new SqlExecuteRequestDto(
                "CREATE TABLE STRICT_TEST (ID INT PRIMARY KEY, NAME VARCHAR(50) NOT NULL, EMAIL VARCHAR(50) NOT NULL)", "QUERY"
        ));

        try {
            // Missing EMAIL column in INSERT
            SqlExecuteRequestDto req = new SqlExecuteRequestDto(
                    "INSERT INTO STRICT_TEST (ID, NAME) VALUES (1, 'Alice')", "QUERY", true
            );
            SqlExecuteResponseDto resp = sqlCompilerService.execute(req);
            assertFalse(resp.isSuccess());
            assertFalse(resp.isValidationPassed());
            assertEquals("ORA-VALIDATION-BLOCKED", resp.getErrorCode());
            assertTrue(resp.getErrorMessage().contains("EMAIL") || resp.getErrorMessage().contains("NOT NULL"));

            // Explicit NULL for EMAIL in INSERT
            SqlExecuteRequestDto reqNull = new SqlExecuteRequestDto(
                    "INSERT INTO STRICT_TEST (ID, NAME, EMAIL) VALUES (1, 'Alice', NULL)", "QUERY", true
            );
            SqlExecuteResponseDto respNull = sqlCompilerService.execute(reqNull);
            assertFalse(respNull.isSuccess());
            assertFalse(respNull.isValidationPassed());
            assertEquals("ORA-VALIDATION-BLOCKED", respNull.getErrorCode());

            // Valid INSERT
            SqlExecuteRequestDto reqValid = new SqlExecuteRequestDto(
                    "INSERT INTO STRICT_TEST (ID, NAME, EMAIL) VALUES (1, 'Alice', 'alice@vit.ac.in')", "QUERY", true
            );
            SqlExecuteResponseDto respValid = sqlCompilerService.execute(reqValid);
            assertTrue(respValid.isSuccess());
            assertTrue(respValid.isValidationPassed());

            // UPDATE setting NOT NULL column to NULL
            SqlExecuteRequestDto reqUpdateNull = new SqlExecuteRequestDto(
                    "UPDATE STRICT_TEST SET EMAIL = NULL WHERE ID = 1", "QUERY", true
            );
            SqlExecuteResponseDto respUpdateNull = sqlCompilerService.execute(reqUpdateNull);
            assertFalse(respUpdateNull.isSuccess());
            assertFalse(respUpdateNull.isValidationPassed());
            assertEquals("ORA-VALIDATION-BLOCKED", respUpdateNull.getErrorCode());

            // Relaxed mode (strictMode = false) bypasses pre-validation (database error or behavior applies)
            SqlExecuteRequestDto reqRelaxed = new SqlExecuteRequestDto(
                    "INSERT INTO STRICT_TEST (ID, NAME) VALUES (2, 'Bob')", "QUERY", false
            );
            SqlExecuteResponseDto respRelaxed = sqlCompilerService.execute(reqRelaxed);
            // Pre-validation was not run/blocked, but DB execution failed due to DB constraint
            assertNotEquals("VALIDATION_ERROR", respRelaxed.getErrorCode());
        } finally {
            sqlCompilerService.execute(new SqlExecuteRequestDto("DROP TABLE STRICT_TEST", "QUERY"));
        }
    }

    @Test
    void testSqlSanitizerRedaction() {
        String sql = "INSERT INTO USERS (USERNAME, PASSWORD_HASH) VALUES ('admin', 'MySecretPassword123!')";
        String sanitized = SqlSanitizer.sanitize(sql);
        assertFalse(sanitized.contains("MySecretPassword123!"));
        assertTrue(sanitized.contains("REDACTED"));
    }

    @Test
    void testExecutionAuditHistory() {
        // Execute a statement
        sqlCompilerService.execute(new SqlExecuteRequestDto("SELECT 1 AS TEST_AUDIT FROM DUAL", "QUERY"));

        List<SqlExecutionAuditDto> history = sqlCompilerService.getExecutionHistory(10);
        assertNotNull(history);
        assertFalse(history.isEmpty());
        assertTrue(history.stream().anyMatch(h -> h.getSqlText() != null && h.getSqlText().contains("TEST_AUDIT")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testAdminAccessAllowed() throws Exception {
        mockMvc.perform(get("/api/v1/admin/sql/connection-info"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/admin/sql/schema"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/admin/sql/history?limit=10"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/sql/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sql\":\"SELECT 1 FROM DUAL\",\"mode\":\"QUERY\",\"strictMode\":true}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void testStudentAccessForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/sql/connection-info"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/admin/sql/history"))
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

        mockMvc.perform(get("/api/v1/admin/sql/history"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/admin/sql/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sql\":\"SELECT 1 FROM DUAL\",\"mode\":\"QUERY\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testSampleQueriesParsing() {
        // Multi-statement DML script
        String dmlScript = """
            INSERT INTO STUDENT (Student_Id, Name, Email, DOB, Program_Id, Branch, CGPA)
            VALUES ('STU_DEMO_999', 'Arun Kumar', 'arun.kumar2026@vitstudent.ac.in', TO_DATE('2004-05-15', 'YYYY-MM-DD'), 'BTECH-CSE', 'CSE', 8.75);
            SELECT Student_Id, Name FROM STUDENT WHERE Student_Id = 'STU_DEMO_999';
            UPDATE STUDENT SET CGPA = 9.10 WHERE Student_Id = 'STU_DEMO_999';
            DELETE FROM STUDENT WHERE Student_Id = 'STU_DEMO_999';
            COMMIT;
            """;
        List<String> dmlStmts = SqlScriptParser.splitScript(dmlScript);
        assertEquals(5, dmlStmts.size());
        assertTrue(dmlStmts.get(0).startsWith("INSERT INTO STUDENT"));
        assertTrue(dmlStmts.get(1).startsWith("SELECT Student_Id"));
        assertTrue(dmlStmts.get(2).startsWith("UPDATE STUDENT"));
        assertTrue(dmlStmts.get(3).startsWith("DELETE FROM STUDENT"));
        assertTrue(dmlStmts.get(4).startsWith("COMMIT"));

        // PL/SQL Anonymous block with /
        String plsqlBlock = """
            DECLARE
                v_count NUMBER := 0;
            BEGIN
                SELECT COUNT(*) INTO v_count FROM STUDENT;
                DBMS_OUTPUT.PUT_LINE('Count: ' || v_count);
            END;
            /
            """;
        List<String> plsqlStmts = SqlScriptParser.splitScript(plsqlBlock);
        assertEquals(1, plsqlStmts.size());
        assertTrue(plsqlStmts.get(0).startsWith("DECLARE"));
        assertTrue(plsqlStmts.get(0).endsWith("END;"));
        assertFalse(plsqlStmts.get(0).contains("/"));
    }
}
