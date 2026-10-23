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
    void testExecuteSafeCteQuery() {
        SqlExecuteRequestDto req = new SqlExecuteRequestDto(
                "WITH cte AS (SELECT 1 AS X FROM DUAL) SELECT X FROM cte", "QUERY"
        );
        SqlExecuteResponseDto resp = sqlCompilerService.execute(req);

        assertNotNull(resp);
        assertTrue(resp.isSuccess());
        assertEquals("SELECT", resp.getStatementType());
        assertNotNull(resp.getColumns());
        assertEquals(1, resp.getRowCount());
    }

    @Test
    void testExecuteBlockedDdl() {
        String[] ddlQueries = {
            "CREATE TABLE HACK_TEST (ID INT)",
            "DROP TABLE STUDENT",
            "ALTER TABLE USERS ADD (PIN VARCHAR(10))",
            "TRUNCATE TABLE APPLICATION"
        };
        for (String q : ddlQueries) {
            SqlExecuteResponseDto resp = sqlCompilerService.execute(new SqlExecuteRequestDto(q, "QUERY"));
            assertNotNull(resp, "Response should not be null for: " + q);
            assertFalse(resp.isSuccess(), "DDL should be blocked: " + q);
            assertFalse(resp.isValidationPassed());
            assertEquals("ORA-POLICY-BLOCKED", resp.getErrorCode());
            assertTrue(resp.getErrorMessage().contains("strictly prohibited") || resp.getErrorMessage().contains("Prohibited"));
        }
    }

    @Test
    void testExecuteBlockedDml() {
        String[] dmlQueries = {
            "INSERT INTO STUDENT (Student_Id, Name) VALUES ('STU_999', 'Hacker')",
            "UPDATE USERS SET PASSWORD_HASH = 'hacked' WHERE USERNAME = 'admin'",
            "DELETE FROM STUDENT WHERE 1=1",
            "MERGE INTO STUDENT USING DUAL ON (1=1) WHEN MATCHED THEN UPDATE SET NAME = 'X'"
        };
        for (String q : dmlQueries) {
            SqlExecuteResponseDto resp = sqlCompilerService.execute(new SqlExecuteRequestDto(q, "QUERY"));
            assertNotNull(resp, "Response should not be null for: " + q);
            assertFalse(resp.isSuccess(), "DML should be blocked: " + q);
            assertFalse(resp.isValidationPassed());
            assertEquals("ORA-POLICY-BLOCKED", resp.getErrorCode());
        }
    }

    @Test
    void testExecuteBlockedPlsqlAndCalls() {
        String[] plsqlQueries = {
            "DECLARE x NUMBER; BEGIN x := 1; END;",
            "BEGIN NULL; END;",
            "EXEC dbms_lock.sleep(10);",
            "CALL some_proc()"
        };
        for (String q : plsqlQueries) {
            SqlExecuteResponseDto resp = sqlCompilerService.execute(new SqlExecuteRequestDto(q, "QUERY"));
            assertNotNull(resp, "Response should not be null for: " + q);
            assertFalse(resp.isSuccess(), "PL/SQL should be blocked: " + q);
            assertFalse(resp.isValidationPassed());
            assertEquals("ORA-POLICY-BLOCKED", resp.getErrorCode());
        }
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
    void testExecuteBlockedMultipleStatements() {
        SqlExecuteResponseDto resp = sqlCompilerService.execute(new SqlExecuteRequestDto(
                "SELECT 1 FROM DUAL; SELECT 2 FROM DUAL;", "QUERY"
        ));
        assertNotNull(resp);
        assertFalse(resp.isSuccess());
        assertFalse(resp.isValidationPassed());
        assertEquals("ORA-POLICY-BLOCKED", resp.getErrorCode());
        assertTrue(resp.getErrorMessage().contains("Multiple statements are not permitted"));
    }

    @Test
    void testExecuteBlockedCommentBypasses() {
        String[] bypassAttempts = {
            "/* inline comment */ DROP TABLE STUDENT",
            "-- comment line \n DROP TABLE STUDENT",
            "SELECT 1 FROM DUAL; /* comment */ DROP TABLE STUDENT",
            "SELECT 1 FROM DUAL; -- trailing attack \n DELETE FROM USERS"
        };
        for (String q : bypassAttempts) {
            SqlExecuteResponseDto resp = sqlCompilerService.execute(new SqlExecuteRequestDto(q, "QUERY"));
            assertNotNull(resp, "Response should not be null for: " + q);
            assertFalse(resp.isSuccess(), "Comment bypass should be blocked: " + q);
            assertFalse(resp.isValidationPassed());
            assertEquals("ORA-POLICY-BLOCKED", resp.getErrorCode());
        }
    }

    @Test
    void testStrictModeFlagCannotBypassReadOnlyPolicy() {
        // Even when client explicitly supplies strictMode=false, DML/DDL must be blocked
        SqlExecuteRequestDto req = new SqlExecuteRequestDto("INSERT INTO STUDENT VALUES ('STU_X')", "QUERY", false);
        SqlExecuteResponseDto resp = sqlCompilerService.execute(req);

        assertNotNull(resp);
        assertFalse(resp.isSuccess());
        assertFalse(resp.isValidationPassed());
        assertEquals("ORA-POLICY-BLOCKED", resp.getErrorCode());
    }

    @Test
    void testExecuteBlockedSelectIntoAndForUpdate() {
        SqlExecuteResponseDto forUpdateResp = sqlCompilerService.execute(new SqlExecuteRequestDto(
                "SELECT * FROM STUDENT FOR UPDATE", "QUERY"
        ));
        assertFalse(forUpdateResp.isSuccess());
        assertEquals("ORA-POLICY-BLOCKED", forUpdateResp.getErrorCode());

        SqlExecuteResponseDto intoResp = sqlCompilerService.execute(new SqlExecuteRequestDto(
                "SELECT Student_Id INTO :v FROM STUDENT", "QUERY"
        ));
        assertFalse(intoResp.isSuccess());
        assertEquals("ORA-POLICY-BLOCKED", intoResp.getErrorCode());
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
