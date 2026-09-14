package com.placement.portal.admin.compiler;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class SqlCompilerService {

    private static final Logger log = LoggerFactory.getLogger(SqlCompilerService.class);
    private static final int MAX_SELECT_ROWS = 1000;
    private static final int DBMS_OUTPUT_BUFFER_SIZE = 1000000;

    private final DataSource dataSource;

    public SqlCompilerService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @PostConstruct
    public void ensureAuditTableExists() {
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            String dbProduct = meta.getDatabaseProductName() != null ? meta.getDatabaseProductName().toLowerCase() : "";
            boolean isOracle = dbProduct.contains("oracle");

            String checkSql = isOracle
                    ? "SELECT COUNT(*) FROM USER_TABLES WHERE TABLE_NAME = 'SQL_EXECUTION_AUDIT'"
                    : "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE UPPER(TABLE_NAME) = 'SQL_EXECUTION_AUDIT'";

            boolean exists = false;
            try (Statement s = conn.createStatement(); ResultSet rs = s.executeQuery(checkSql)) {
                if (rs.next()) {
                    exists = rs.getInt(1) > 0;
                }
            } catch (Exception ignored) {}

            if (!exists) {
                String ddl = isOracle ? """
                    CREATE TABLE SQL_EXECUTION_AUDIT (
                        Audit_Id          NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                        Admin_Id          VARCHAR2(50) DEFAULT 'admin' NOT NULL,
                        Executed_At       TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
                        Statement_Type    VARCHAR2(30) NOT NULL,
                        SQL_Text          CLOB NOT NULL,
                        Status            VARCHAR2(20) NOT NULL,
                        Affected_Rows     NUMBER,
                        Returned_Rows     NUMBER,
                        Execution_Time_Ms NUMBER NOT NULL,
                        Error_Code        VARCHAR2(50),
                        Error_Message     VARCHAR2(1000)
                    )
                """ : """
                    CREATE TABLE IF NOT EXISTS SQL_EXECUTION_AUDIT (
                        Audit_Id          BIGINT AUTO_INCREMENT PRIMARY KEY,
                        Admin_Id          VARCHAR(50) DEFAULT 'admin' NOT NULL,
                        Executed_At       TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                        Statement_Type    VARCHAR(30) NOT NULL,
                        SQL_Text          CLOB NOT NULL,
                        Status            VARCHAR(20) NOT NULL,
                        Affected_Rows     INT,
                        Returned_Rows     INT,
                        Execution_Time_Ms BIGINT NOT NULL,
                        Error_Code        VARCHAR(50),
                        Error_Message     VARCHAR(1000)
                    )
                """;
                try (Statement s = conn.createStatement()) {
                    s.execute(ddl);
                    log.info("SQL_EXECUTION_AUDIT table verified and created successfully.");
                } catch (Exception e) {
                    log.debug("Notice while creating SQL_EXECUTION_AUDIT: {}", e.getMessage());
                }

                if (isOracle) {
                    try (Statement s = conn.createStatement()) {
                        s.execute("CREATE INDEX IDX_SQL_AUDIT_TIME ON SQL_EXECUTION_AUDIT (Executed_At DESC)");
                    } catch (Exception ignored) {}
                    try (Statement s = conn.createStatement()) {
                        s.execute("CREATE INDEX IDX_SQL_AUDIT_ADMIN ON SQL_EXECUTION_AUDIT (Admin_Id)");
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception e) {
            log.warn("Could not verify or create SQL_EXECUTION_AUDIT table: {}", e.getMessage());
        }
    }

    /**
     * Executes SQL or PL/SQL in either QUERY mode (single statement) or SCRIPT mode (multi-statement script).
     */
    public SqlExecuteResponseDto execute(SqlExecuteRequestDto request) {
        if (request == null || request.getSql() == null || request.getSql().trim().isEmpty()) {
            return SqlExecuteResponseDto.errorResult("ORA-EMPTY", "SQL statement cannot be empty", "SQL statement cannot be empty", null, null, 0);
        }

        String rawSql = request.getSql().trim();
        String mode = request.getMode();
        boolean strictMode = request.isStrictMode();

        List<String> statements = SqlScriptParser.splitScript(rawSql);
        if (statements.isEmpty()) {
            return SqlExecuteResponseDto.errorResult("ORA-EMPTY", "No executable statement found", "No executable statement found", null, null, 0);
        }

        // Automatically execute as multi-statement script if more than 1 statement is present or if explicitly requested
        if (statements.size() > 1 || "SCRIPT".equalsIgnoreCase(mode)) {
            return executeScript(rawSql, strictMode);
        } else {
            return executeSingleQuery(statements.get(0), strictMode);
        }
    }

    /**
     * Executes a single SQL or PL/SQL statement with transaction management and strict data integrity checks.
     */
    private SqlExecuteResponseDto executeSingleQuery(String rawSql, boolean strictMode) {
        long startTime = System.currentTimeMillis();
        String sql = SqlScriptParser.cleanStatement(rawSql, false);
        if (sql.isEmpty()) {
            return SqlExecuteResponseDto.errorResult("ORA-EMPTY", "No executable statement found", "No executable statement found", null, null, 0);
        }

        String currentAdmin = getCurrentAdminId();

        try (Connection conn = dataSource.getConnection()) {
            // Pre-execution Strict Data Integrity Validation
            SqlDataIntegrityValidator.ValidationResult valResult = SqlDataIntegrityValidator.validate(conn, sql, strictMode);
            if (!valResult.isPassed()) {
                SqlExecuteResponseDto blockedDto = SqlExecuteResponseDto.blockedResult(valResult.getMessage(), valResult.getDiagnosticDetail(), sql);
                logAudit(currentAdmin, "BLOCKED", sql, "BLOCKED", 0, 0, 0, "ORA-VALIDATION-BLOCKED", valResult.getDiagnosticDetail());
                return blockedDto;
            }

            conn.setAutoCommit(false);
            enableDbmsOutput(conn);

            boolean isSelect = isSelectQuery(sql);
            try (Statement stmt = conn.createStatement()) {
                stmt.setQueryTimeout(30);

                if (isSelect) {
                    try (ResultSet rs = stmt.executeQuery(sql)) {
                        long elapsed = System.currentTimeMillis() - startTime;
                        ResultSetMetaData meta = rs.getMetaData();
                        int colCount = meta.getColumnCount();

                        List<String> columns = new ArrayList<>();
                        for (int i = 1; i <= colCount; i++) {
                            columns.add(meta.getColumnLabel(i));
                        }

                        List<List<Object>> rows = new ArrayList<>();
                        int count = 0;
                        while (rs.next() && count < MAX_SELECT_ROWS) {
                            List<Object> row = new ArrayList<>();
                            for (int i = 1; i <= colCount; i++) {
                                row.add(formatValue(rs, i, meta.getColumnType(i)));
                            }
                            rows.add(row);
                            count++;
                        }

                        String dbmsOutput = drainDbmsOutput(conn);
                        SqlExecuteResponseDto dto = SqlExecuteResponseDto.selectResult(columns, rows, count, dbmsOutput, elapsed);
                        dto.setSql(sql);
                        dto.setTransactionState("NONE");
                        logAudit(currentAdmin, "SELECT", sql, "SUCCESS", 0, count, elapsed, null, null);
                        return dto;
                    }
                } else {
                    boolean hasResultSet = stmt.execute(sql);
                    long elapsed = System.currentTimeMillis() - startTime;
                    String dbmsOutput = drainDbmsOutput(conn);

                    SqlExecuteResponseDto dto;
                    if (hasResultSet) {
                        try (ResultSet rs = stmt.getResultSet()) {
                            ResultSetMetaData meta = rs.getMetaData();
                            int colCount = meta.getColumnCount();
                            List<String> columns = new ArrayList<>();
                            for (int i = 1; i <= colCount; i++) {
                                columns.add(meta.getColumnLabel(i));
                            }
                            List<List<Object>> rows = new ArrayList<>();
                            int count = 0;
                            while (rs.next() && count < MAX_SELECT_ROWS) {
                                List<Object> row = new ArrayList<>();
                                for (int i = 1; i <= colCount; i++) {
                                    row.add(formatValue(rs, i, meta.getColumnType(i)));
                                }
                                rows.add(row);
                                count++;
                            }
                            dto = SqlExecuteResponseDto.selectResult(columns, rows, count, dbmsOutput, elapsed);
                            dto.setTransactionState("NONE");
                            logAudit(currentAdmin, "SELECT", sql, "SUCCESS", 0, count, elapsed, null, null);
                        }
                    } else {
                        int updateCount = stmt.getUpdateCount();
                        String upper = sql.replaceAll("--.*", "").trim().toUpperCase();

                        if (upper.startsWith("INSERT")) {
                            conn.commit();
                            dto = SqlExecuteResponseDto.dmlResult("INSERT", Math.max(updateCount, 0), dbmsOutput, elapsed);
                            dto.setTransactionState("COMMITTED");
                            logAudit(currentAdmin, "INSERT", sql, "SUCCESS", Math.max(updateCount, 0), 0, elapsed, null, null);
                        } else if (upper.startsWith("UPDATE")) {
                            conn.commit();
                            dto = SqlExecuteResponseDto.dmlResult("UPDATE", Math.max(updateCount, 0), dbmsOutput, elapsed);
                            dto.setTransactionState("COMMITTED");
                            logAudit(currentAdmin, "UPDATE", sql, "SUCCESS", Math.max(updateCount, 0), 0, elapsed, null, null);
                        } else if (upper.startsWith("DELETE")) {
                            conn.commit();
                            dto = SqlExecuteResponseDto.dmlResult("DELETE", Math.max(updateCount, 0), dbmsOutput, elapsed);
                            dto.setTransactionState("COMMITTED");
                            logAudit(currentAdmin, "DELETE", sql, "SUCCESS", Math.max(updateCount, 0), 0, elapsed, null, null);
                        } else if (upper.startsWith("MERGE")) {
                            conn.commit();
                            dto = SqlExecuteResponseDto.dmlResult("MERGE", Math.max(updateCount, 0), dbmsOutput, elapsed);
                            dto.setTransactionState("COMMITTED");
                            logAudit(currentAdmin, "MERGE", sql, "SUCCESS", Math.max(updateCount, 0), 0, elapsed, null, null);
                        } else if (upper.startsWith("DECLARE") || upper.startsWith("BEGIN")) {
                            conn.commit();
                            dto = SqlExecuteResponseDto.plsqlResult(dbmsOutput, elapsed);
                            dto.setTransactionState("COMMITTED");
                            logAudit(currentAdmin, "PLSQL", sql, "SUCCESS", 0, 0, elapsed, null, null);
                        } else if (upper.startsWith("CREATE") || upper.startsWith("ALTER") || upper.startsWith("DROP") || upper.startsWith("TRUNCATE")) {
                            conn.commit();
                            String type = upper.split("\\s+")[0];
                            dto = SqlExecuteResponseDto.ddlResult(type, "Command executed successfully.", dbmsOutput, elapsed);
                            dto.setTransactionState("COMMITTED");
                            logAudit(currentAdmin, type, sql, "SUCCESS", 0, 0, elapsed, null, null);
                        } else if (upper.startsWith("COMMIT")) {
                            conn.commit();
                            dto = SqlExecuteResponseDto.tclResult("COMMIT", "Commit complete.", elapsed);
                            dto.setTransactionState("COMMITTED");
                            logAudit(currentAdmin, "COMMIT", sql, "SUCCESS", 0, 0, elapsed, null, null);
                        } else if (upper.startsWith("ROLLBACK")) {
                            conn.rollback();
                            dto = SqlExecuteResponseDto.tclResult("ROLLBACK", "Rollback complete.", elapsed);
                            dto.setTransactionState("ROLLED_BACK");
                            logAudit(currentAdmin, "ROLLBACK", sql, "SUCCESS", 0, 0, elapsed, null, null);
                        } else if (upper.startsWith("SAVEPOINT")) {
                            dto = SqlExecuteResponseDto.tclResult("SAVEPOINT", "Savepoint created.", elapsed);
                            dto.setTransactionState("NONE");
                            logAudit(currentAdmin, "SAVEPOINT", sql, "SUCCESS", 0, 0, elapsed, null, null);
                        } else {
                            conn.commit();
                            dto = SqlExecuteResponseDto.ddlResult("STATEMENT", "Statement executed successfully.", dbmsOutput, elapsed);
                            dto.setTransactionState("COMMITTED");
                            logAudit(currentAdmin, "STATEMENT", sql, "SUCCESS", 0, 0, elapsed, null, null);
                        }
                    }
                    if (dto != null) {
                        dto.setSql(sql);
                    }
                    return dto;
                }
            } catch (SQLException e) {
                try {
                    conn.rollback();
                } catch (Exception ignored) {}
                long elapsed = System.currentTimeMillis() - startTime;
                String oraCode = formatErrorCode(e.getErrorCode());
                String fullError = e.getMessage();
                SqlExecuteResponseDto dto = SqlExecuteResponseDto.errorResult(oraCode, e.getMessage(), fullError, null, null, elapsed);
                dto.setSql(sql);
                dto.setTransactionState("ROLLED_BACK");
                logAudit(currentAdmin, "ERROR", sql, "ERROR", 0, 0, elapsed, oraCode, e.getMessage());
                return dto;
            }
        } catch (SQLException e) {
            long elapsed = System.currentTimeMillis() - startTime;
            String oraCode = formatErrorCode(e.getErrorCode());
            String fullError = e.getMessage();
            SqlExecuteResponseDto dto = SqlExecuteResponseDto.errorResult(oraCode, e.getMessage(), fullError, null, null, elapsed);
            dto.setSql(sql);
            dto.setTransactionState("ROLLED_BACK");
            logAudit(currentAdmin, "ERROR", sql, "ERROR", 0, 0, elapsed, oraCode, e.getMessage());
            return dto;
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            SqlExecuteResponseDto dto = SqlExecuteResponseDto.errorResult("ERROR", e.getMessage(), e.getMessage(), null, null, elapsed);
            dto.setSql(sql);
            dto.setTransactionState("ROLLED_BACK");
            logAudit(currentAdmin, "ERROR", sql, "ERROR", 0, 0, elapsed, "ERROR", e.getMessage());
            return dto;
        }
    }

    /**
     * Executes a multi-statement script sequentially within a managed transaction.
     */
    private SqlExecuteResponseDto executeScript(String rawScript, boolean strictMode) {
        long totalStart = System.currentTimeMillis();
        List<String> statements = SqlScriptParser.splitScript(rawScript);

        if (statements.isEmpty()) {
            return SqlExecuteResponseDto.errorResult("ORA-EMPTY", "No executable statements found in script", "No executable statements found in script", null, null, 0);
        }

        String currentAdmin = getCurrentAdminId();
        List<SqlExecuteResponseDto> results = new ArrayList<>();
        StringBuilder combinedDbms = new StringBuilder();

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            enableDbmsOutput(conn);

            for (String stmtSql : statements) {
                long stmtStart = System.currentTimeMillis();

                // Strict Data Integrity Pre-Validation per statement
                SqlDataIntegrityValidator.ValidationResult valResult = SqlDataIntegrityValidator.validate(conn, stmtSql, strictMode);
                if (!valResult.isPassed()) {
                    conn.rollback();
                    SqlExecuteResponseDto blockedDto = SqlExecuteResponseDto.blockedResult(valResult.getMessage(), valResult.getDiagnosticDetail(), stmtSql);
                    blockedDto.setTransactionState("ROLLED_BACK");
                    results.add(blockedDto);
                    logAudit(currentAdmin, "BLOCKED", stmtSql, "BLOCKED", 0, 0, 0, "ORA-VALIDATION-BLOCKED", valResult.getDiagnosticDetail());
                    break;
                }

                try (Statement stmt = conn.createStatement()) {
                    stmt.setQueryTimeout(30);

                    SqlExecuteResponseDto stmtResult = null;
                    boolean isSelect = isSelectQuery(stmtSql);
                    if (isSelect) {
                        try (ResultSet rs = stmt.executeQuery(stmtSql)) {
                            long elapsed = System.currentTimeMillis() - stmtStart;
                            ResultSetMetaData meta = rs.getMetaData();
                            int colCount = meta.getColumnCount();
                            List<String> columns = new ArrayList<>();
                            for (int i = 1; i <= colCount; i++) {
                                columns.add(meta.getColumnLabel(i));
                            }
                            List<List<Object>> rows = new ArrayList<>();
                            int count = 0;
                            while (rs.next() && count < MAX_SELECT_ROWS) {
                                List<Object> row = new ArrayList<>();
                                for (int i = 1; i <= colCount; i++) {
                                    row.add(formatValue(rs, i, meta.getColumnType(i)));
                                }
                                rows.add(row);
                                count++;
                            }
                            String out = drainDbmsOutput(conn);
                            if (out != null && !out.isEmpty()) {
                                combinedDbms.append(out).append("\n");
                            }
                            stmtResult = SqlExecuteResponseDto.selectResult(columns, rows, count, out, elapsed);
                            stmtResult.setTransactionState("NONE");
                            logAudit(currentAdmin, "SELECT", stmtSql, "SUCCESS", 0, count, elapsed, null, null);
                        }
                    } else {
                        boolean hasRs = stmt.execute(stmtSql);
                        long elapsed = System.currentTimeMillis() - stmtStart;
                        String out = drainDbmsOutput(conn);
                        if (out != null && !out.isEmpty()) {
                            combinedDbms.append(out).append("\n");
                        }

                        if (hasRs) {
                            try (ResultSet rs = stmt.getResultSet()) {
                                ResultSetMetaData meta = rs.getMetaData();
                                int colCount = meta.getColumnCount();
                                List<String> columns = new ArrayList<>();
                                for (int i = 1; i <= colCount; i++) {
                                    columns.add(meta.getColumnLabel(i));
                                }
                                List<List<Object>> rows = new ArrayList<>();
                                int count = 0;
                                while (rs.next() && count < MAX_SELECT_ROWS) {
                                    List<Object> row = new ArrayList<>();
                                    for (int i = 1; i <= colCount; i++) {
                                        row.add(formatValue(rs, i, meta.getColumnType(i)));
                                    }
                                    rows.add(row);
                                    count++;
                                }
                                stmtResult = SqlExecuteResponseDto.selectResult(columns, rows, count, out, elapsed);
                                stmtResult.setTransactionState("NONE");
                                logAudit(currentAdmin, "SELECT", stmtSql, "SUCCESS", 0, count, elapsed, null, null);
                            }
                        } else {
                            int updateCount = stmt.getUpdateCount();
                            String upper = stmtSql.replaceAll("--.*", "").trim().toUpperCase();

                            if (upper.startsWith("INSERT")) {
                                stmtResult = SqlExecuteResponseDto.dmlResult("INSERT", Math.max(updateCount, 0), out, elapsed);
                                stmtResult.setTransactionState("COMMITTED");
                                logAudit(currentAdmin, "INSERT", stmtSql, "SUCCESS", Math.max(updateCount, 0), 0, elapsed, null, null);
                            } else if (upper.startsWith("UPDATE")) {
                                stmtResult = SqlExecuteResponseDto.dmlResult("UPDATE", Math.max(updateCount, 0), out, elapsed);
                                stmtResult.setTransactionState("COMMITTED");
                                logAudit(currentAdmin, "UPDATE", stmtSql, "SUCCESS", Math.max(updateCount, 0), 0, elapsed, null, null);
                            } else if (upper.startsWith("DELETE")) {
                                stmtResult = SqlExecuteResponseDto.dmlResult("DELETE", Math.max(updateCount, 0), out, elapsed);
                                stmtResult.setTransactionState("COMMITTED");
                                logAudit(currentAdmin, "DELETE", stmtSql, "SUCCESS", Math.max(updateCount, 0), 0, elapsed, null, null);
                            } else if (upper.startsWith("MERGE")) {
                                stmtResult = SqlExecuteResponseDto.dmlResult("MERGE", Math.max(updateCount, 0), out, elapsed);
                                stmtResult.setTransactionState("COMMITTED");
                                logAudit(currentAdmin, "MERGE", stmtSql, "SUCCESS", Math.max(updateCount, 0), 0, elapsed, null, null);
                            } else if (upper.startsWith("DECLARE") || upper.startsWith("BEGIN")) {
                                stmtResult = SqlExecuteResponseDto.plsqlResult(out, elapsed);
                                stmtResult.setTransactionState("COMMITTED");
                                logAudit(currentAdmin, "PLSQL", stmtSql, "SUCCESS", 0, 0, elapsed, null, null);
                            } else if (upper.startsWith("CREATE") || upper.startsWith("ALTER") || upper.startsWith("DROP") || upper.startsWith("TRUNCATE")) {
                                String type = upper.split("\\s+")[0];
                                stmtResult = SqlExecuteResponseDto.ddlResult(type, "Command executed successfully.", out, elapsed);
                                stmtResult.setTransactionState("COMMITTED");
                                logAudit(currentAdmin, type, stmtSql, "SUCCESS", 0, 0, elapsed, null, null);
                            } else if (upper.startsWith("COMMIT")) {
                                conn.commit();
                                stmtResult = SqlExecuteResponseDto.tclResult("COMMIT", "Commit complete.", elapsed);
                                stmtResult.setTransactionState("COMMITTED");
                                logAudit(currentAdmin, "COMMIT", stmtSql, "SUCCESS", 0, 0, elapsed, null, null);
                            } else if (upper.startsWith("ROLLBACK")) {
                                conn.rollback();
                                stmtResult = SqlExecuteResponseDto.tclResult("ROLLBACK", "Rollback complete.", elapsed);
                                stmtResult.setTransactionState("ROLLED_BACK");
                                logAudit(currentAdmin, "ROLLBACK", stmtSql, "SUCCESS", 0, 0, elapsed, null, null);
                            } else {
                                stmtResult = SqlExecuteResponseDto.ddlResult("STATEMENT", "Statement executed successfully.", out, elapsed);
                                stmtResult.setTransactionState("COMMITTED");
                                logAudit(currentAdmin, "STATEMENT", stmtSql, "SUCCESS", 0, 0, elapsed, null, null);
                            }
                        }
                    }

                    if (stmtResult != null) {
                        stmtResult.setSql(stmtSql);
                        results.add(stmtResult);
                    }
                } catch (SQLException e) {
                    conn.rollback();
                    long elapsed = System.currentTimeMillis() - stmtStart;
                    String oraCode = formatErrorCode(e.getErrorCode());
                    SqlExecuteResponseDto errDto = SqlExecuteResponseDto.errorResult(oraCode, e.getMessage(), e.getMessage(), null, null, elapsed);
                    errDto.setSql(stmtSql);
                    errDto.setTransactionState("ROLLED_BACK");
                    results.add(errDto);
                    logAudit(currentAdmin, "ERROR", stmtSql, "ERROR", 0, 0, elapsed, oraCode, e.getMessage());
                    break;
                }
            }

            // If all statements succeeded and no unhandled rollback, commit transaction
            boolean allPassed = results.stream().allMatch(SqlExecuteResponseDto::isSuccess);
            if (allPassed) {
                conn.commit();
            }
        } catch (Exception e) {
            return SqlExecuteResponseDto.errorResult("SCRIPT_ERROR", e.getMessage(), e.getMessage(), null, null, System.currentTimeMillis() - totalStart);
        }

        long totalElapsed = System.currentTimeMillis() - totalStart;
        SqlExecuteResponseDto scriptDto = SqlExecuteResponseDto.scriptResult(results, totalElapsed, combinedDbms.toString().trim());

        // Find last SELECT query results to populate root columns/rows for default grid viewing
        for (int i = results.size() - 1; i >= 0; i--) {
            SqlExecuteResponseDto r = results.get(i);
            if (r.getColumns() != null && !r.getColumns().isEmpty()) {
                scriptDto.setColumns(r.getColumns());
                scriptDto.setRows(r.getRows());
                scriptDto.setRowCount(r.getRowCount());
                break;
            }
        }
        return scriptDto;
    }

    @jakarta.annotation.PostConstruct
    public void initAuditTableIfH2() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            DatabaseMetaData meta = conn.getMetaData();
            String dbName = meta.getDatabaseProductName();
            if (dbName != null && dbName.toUpperCase().contains("H2")) {
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS SQL_EXECUTION_AUDIT (
                        AUDIT_ID BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                        ADMIN_ID VARCHAR(100) NOT NULL,
                        EXECUTED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                        STATEMENT_TYPE VARCHAR(20) NOT NULL,
                        SQL_TEXT CLOB NOT NULL,
                        STATUS VARCHAR(20) NOT NULL,
                        AFFECTED_ROWS INT,
                        RETURNED_ROWS INT,
                        EXECUTION_TIME_MS BIGINT NOT NULL,
                        ERROR_CODE VARCHAR(50),
                        ERROR_MESSAGE VARCHAR(1000)
                    )
                """);
            }
        } catch (Exception e) {
            log.debug("Notice on audit table check: {}", e.getMessage());
        }
    }

    /**
     * Records an execution audit entry into Oracle SQL_EXECUTION_AUDIT table.
     */
    private void logAudit(String adminId, String stmtType, String sql, String status, Integer affectedRows, Integer returnedRows, long elapsed, String errCode, String errMsg) {
        String sanitizedSql = SqlSanitizer.sanitize(sql);
        String insertSql = """
            INSERT INTO SQL_EXECUTION_AUDIT (ADMIN_ID, STATEMENT_TYPE, SQL_TEXT, STATUS, AFFECTED_ROWS, RETURNED_ROWS, EXECUTION_TIME_MS, ERROR_CODE, ERROR_MESSAGE)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (Connection auditConn = dataSource.getConnection()) {
            auditConn.setAutoCommit(true);
            try (PreparedStatement ps = auditConn.prepareStatement(insertSql)) {
                ps.setString(1, adminId != null ? adminId : "admin");
                ps.setString(2, stmtType != null ? stmtType : "SQL");
                ps.setString(3, sanitizedSql);
                ps.setString(4, status != null ? status : "UNKNOWN");
                if (affectedRows != null) ps.setInt(5, affectedRows); else ps.setNull(5, Types.INTEGER);
                if (returnedRows != null) ps.setInt(6, returnedRows); else ps.setNull(6, Types.INTEGER);
                ps.setLong(7, elapsed);
                ps.setString(8, errCode);
                if (errMsg != null) {
                    ps.setString(9, errMsg.length() > 1000 ? errMsg.substring(0, 997) + "..." : errMsg);
                } else {
                    ps.setNull(9, Types.VARCHAR);
                }
                ps.executeUpdate();
            }
        } catch (Exception e) {
            log.debug("Could not write to SQL_EXECUTION_AUDIT table: {}", e.getMessage());
        }
    }

    /**
     * Retrieves query execution history from the Oracle-backed SQL_EXECUTION_AUDIT table.
     */
    public List<SqlExecutionAuditDto> getExecutionHistory(int limit) {
        int fetchLimit = Math.min(Math.max(limit, 1), 100);
        List<SqlExecutionAuditDto> list = new ArrayList<>();

        String oraSql = """
            SELECT AUDIT_ID, ADMIN_ID, EXECUTED_AT, STATEMENT_TYPE, SQL_TEXT, STATUS, AFFECTED_ROWS, RETURNED_ROWS, EXECUTION_TIME_MS, ERROR_CODE, ERROR_MESSAGE
            FROM SQL_EXECUTION_AUDIT
            ORDER BY AUDIT_ID DESC
            FETCH FIRST ? ROWS ONLY
        """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(oraSql)) {
            ps.setInt(1, fetchLimit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapAuditRow(rs));
                }
                return list;
            }
        } catch (Exception e) {
            // Fallback for dialect without FETCH FIRST (e.g. H2 limit)
            String fallbackSql = """
                SELECT AUDIT_ID, ADMIN_ID, EXECUTED_AT, STATEMENT_TYPE, SQL_TEXT, STATUS, AFFECTED_ROWS, RETURNED_ROWS, EXECUTION_TIME_MS, ERROR_CODE, ERROR_MESSAGE
                FROM SQL_EXECUTION_AUDIT
                ORDER BY AUDIT_ID DESC
                LIMIT ?
            """;
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(fallbackSql)) {
                ps.setInt(1, fetchLimit);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(mapAuditRow(rs));
                    }
                }
            } catch (Exception ex) {
                log.debug("Execution history query error: {}", ex.getMessage());
            }
        }
        return list;
    }

    private SqlExecutionAuditDto mapAuditRow(ResultSet rs) throws SQLException {
        long auditId = rs.getLong("Audit_Id");
        String adminId = rs.getString("Admin_Id");
        Timestamp ts = rs.getTimestamp("Executed_At");
        LocalDateTime executedAt = ts != null ? ts.toLocalDateTime() : LocalDateTime.now();
        String stmtType = rs.getString("Statement_Type");
        String sqlText = rs.getString("SQL_Text");
        String status = rs.getString("Status");
        int aff = rs.getInt("Affected_Rows");
        Integer affected = rs.wasNull() ? null : aff;
        int ret = rs.getInt("Returned_Rows");
        Integer returned = rs.wasNull() ? null : ret;
        long elapsed = rs.getLong("Execution_Time_Ms");
        String errCode = rs.getString("Error_Code");
        String errMsg = rs.getString("Error_Message");

        return new SqlExecutionAuditDto(
                auditId, adminId, executedAt, stmtType, sqlText, status, affected, returned, elapsed, errCode, errMsg
        );
    }

    private String getCurrentAdminId() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getName() != null && !auth.getName().isBlank()) {
                return auth.getName();
            }
        } catch (Exception ignored) {}
        return "admin";
    }

    /**
     * Enables DBMS_OUTPUT on the connection.
     */
    private void enableDbmsOutput(Connection conn) {
        try (CallableStatement cs = conn.prepareCall("BEGIN DBMS_OUTPUT.ENABLE(?); END;")) {
            cs.setInt(1, DBMS_OUTPUT_BUFFER_SIZE);
            cs.execute();
        } catch (Exception e) {
            log.debug("DBMS_OUTPUT.ENABLE not supported or failed: {}", e.getMessage());
        }
    }

    /**
     * Drains lines from DBMS_OUTPUT buffer.
     */
    private String drainDbmsOutput(Connection conn) {
        StringBuilder sb = new StringBuilder();
        try (CallableStatement cs = conn.prepareCall("BEGIN DBMS_OUTPUT.GET_LINE(?, ?); END;")) {
            cs.registerOutParameter(1, Types.VARCHAR);
            cs.registerOutParameter(2, Types.INTEGER);

            while (true) {
                cs.execute();
                int status = cs.getInt(2);
                if (status != 0) break; // 0 = line retrieved, 1 = no more lines

                String line = cs.getString(1);
                if (line != null) {
                    sb.append(line).append("\n");
                }
            }
        } catch (Exception e) {
            log.debug("DBMS_OUTPUT.GET_LINE error: {}", e.getMessage());
        }
        return sb.toString().trim();
    }

    /**
     * Formats result set values safely, preserving NULLs and converting BLOB/CLOB/dates cleanly.
     */
    private Object formatValue(ResultSet rs, int colIndex, int colType) throws SQLException {
        Object val = rs.getObject(colIndex);
        if (val == null) {
            return null;
        }

        switch (colType) {
            case Types.BLOB:
            case Types.BINARY:
            case Types.VARBINARY:
            case Types.LONGVARBINARY:
                return "[BLOB]";
            case Types.CLOB:
            case Types.NCLOB:
                Clob clob = rs.getClob(colIndex);
                if (clob != null) {
                    try {
                        long len = Math.min(clob.length(), 4000);
                        return clob.getSubString(1, (int) len);
                    } catch (Exception e) {
                        return "[CLOB]";
                    }
                }
                return null;
            case Types.DATE:
                java.sql.Date d = rs.getDate(colIndex);
                return d != null ? d.toString() : null;
            case Types.TIMESTAMP:
            case Types.TIMESTAMP_WITH_TIMEZONE:
                Timestamp ts = rs.getTimestamp(colIndex);
                return ts != null ? ts.toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null;
            default:
                return val;
        }
    }

    private boolean isSelectQuery(String sql) {
        String clean = sql.replaceAll("--.*", "").trim().toUpperCase();
        return clean.startsWith("SELECT") || clean.startsWith("WITH");
    }

    private String formatErrorCode(int code) {
        if (code <= 0) return "ORA-ERROR";
        return String.format("ORA-%05d", code);
    }

    /**
     * Retrieves rich schema metadata from Oracle dictionary tables.
     */
    public SchemaMetadataDto getSchemaMetadata() {
        List<SchemaMetadataDto.TableMetadata> tables = new ArrayList<>();
        List<SchemaMetadataDto.ViewMetadata> views = new ArrayList<>();
        List<SchemaMetadataDto.ProcedureMetadata> procedures = new ArrayList<>();
        List<SchemaMetadataDto.FunctionMetadata> functions = new ArrayList<>();
        List<SchemaMetadataDto.TriggerMetadata> triggers = new ArrayList<>();

        try (Connection conn = dataSource.getConnection()) {
            // 1. Primary keys mapping: TABLE -> COLUMN
            Set<String> pkColumns = new HashSet<>();
            String pkSql = """
                SELECT cc.TABLE_NAME, cc.COLUMN_NAME
                FROM USER_CONSTRAINTS c
                JOIN USER_CONS_COLUMNS cc ON c.CONSTRAINT_NAME = cc.CONSTRAINT_NAME
                WHERE c.CONSTRAINT_TYPE = 'P'
            """;
            try (Statement s = conn.createStatement(); ResultSet rs = s.executeQuery(pkSql)) {
                while (rs.next()) {
                    pkColumns.add(rs.getString("TABLE_NAME") + "." + rs.getString("COLUMN_NAME"));
                }
            } catch (Exception ignored) {}

            // 2. Foreign keys mapping: TABLE.COL -> REF_TABLE.REF_COL
            Map<String, String> fkMap = new HashMap<>();
            String fkSql = """
                SELECT cc.TABLE_NAME, cc.COLUMN_NAME, r_c.TABLE_NAME AS REF_TABLE, r_cc.COLUMN_NAME AS REF_COL
                FROM USER_CONSTRAINTS c
                JOIN USER_CONS_COLUMNS cc ON c.CONSTRAINT_NAME = cc.CONSTRAINT_NAME
                JOIN USER_CONSTRAINTS r_c ON c.R_CONSTRAINT_NAME = r_c.CONSTRAINT_NAME
                JOIN USER_CONS_COLUMNS r_cc ON r_c.CONSTRAINT_NAME = r_cc.CONSTRAINT_NAME
                WHERE c.CONSTRAINT_TYPE = 'R'
            """;
            try (Statement s = conn.createStatement(); ResultSet rs = s.executeQuery(fkSql)) {
                while (rs.next()) {
                    String src = rs.getString("TABLE_NAME") + "." + rs.getString("COLUMN_NAME");
                    String ref = rs.getString("REF_TABLE") + "." + rs.getString("REF_COL");
                    fkMap.put(src, ref);
                }
            } catch (Exception ignored) {}

            // 3. Unique constraints: TABLE -> list of constraints / columns
            Map<String, List<String>> uniqueMap = new HashMap<>();
            Set<String> uniqueColumns = new HashSet<>();
            String uqSql = """
                SELECT cc.TABLE_NAME, cc.COLUMN_NAME, c.CONSTRAINT_NAME
                FROM USER_CONSTRAINTS c
                JOIN USER_CONS_COLUMNS cc ON c.CONSTRAINT_NAME = cc.CONSTRAINT_NAME
                WHERE c.CONSTRAINT_TYPE = 'U'
            """;
            try (Statement s = conn.createStatement(); ResultSet rs = s.executeQuery(uqSql)) {
                while (rs.next()) {
                    String tName = rs.getString("TABLE_NAME");
                    String cName = rs.getString("COLUMN_NAME");
                    String constrName = rs.getString("CONSTRAINT_NAME");
                    uniqueColumns.add(tName + "." + cName);
                    uniqueMap.computeIfAbsent(tName, k -> new ArrayList<>()).add(constrName + " (" + cName + ")");
                }
            } catch (Exception ignored) {}

            // 4. Check constraints: TABLE -> list of search conditions
            Map<String, List<String>> checkMap = new HashMap<>();
            String chkSql = """
                SELECT TABLE_NAME, CONSTRAINT_NAME, SEARCH_CONDITION_VC
                FROM USER_CONSTRAINTS
                WHERE CONSTRAINT_TYPE = 'C' AND SEARCH_CONDITION_VC IS NOT NULL
                  AND CONSTRAINT_NAME NOT LIKE 'SYS_%'
            """;
            try (Statement s = conn.createStatement(); ResultSet rs = s.executeQuery(chkSql)) {
                while (rs.next()) {
                    String tName = rs.getString("TABLE_NAME");
                    String cond = rs.getString("SEARCH_CONDITION_VC");
                    String cName = rs.getString("CONSTRAINT_NAME");
                    if (cond != null && !cond.isBlank()) {
                        checkMap.computeIfAbsent(tName, k -> new ArrayList<>()).add(cName + ": " + cond.trim());
                    }
                }
            } catch (Exception ignored) {}

            // 5. Tables and their columns
            Map<String, List<SchemaMetadataDto.ColumnMetadata>> tableCols = new LinkedHashMap<>();
            String colSql = """
                SELECT TABLE_NAME, COLUMN_NAME, DATA_TYPE, DATA_LENGTH, NULLABLE, DATA_DEFAULT
                FROM USER_TAB_COLUMNS
                ORDER BY TABLE_NAME, COLUMN_ID
            """;
            try (Statement s = conn.createStatement(); ResultSet rs = s.executeQuery(colSql)) {
                while (rs.next()) {
                    String tName = rs.getString("TABLE_NAME");
                    String cName = rs.getString("COLUMN_NAME");
                    String dType = rs.getString("DATA_TYPE");
                    int dLen = rs.getInt("DATA_LENGTH");
                    boolean nullable = "Y".equalsIgnoreCase(rs.getString("NULLABLE"));
                    boolean isPk = pkColumns.contains(tName + "." + cName);
                    boolean isUq = uniqueColumns.contains(tName + "." + cName);
                    String fkRef = fkMap.get(tName + "." + cName);
                    String defVal = null;
                    try {
                        defVal = rs.getString("DATA_DEFAULT");
                        if (defVal != null) defVal = defVal.trim();
                    } catch (Exception ignored) {}

                    tableCols.computeIfAbsent(tName, k -> new ArrayList<>())
                            .add(new SchemaMetadataDto.ColumnMetadata(cName, dType, dLen, nullable, isPk, isUq, fkRef, defVal));
                }
            } catch (Exception ignored) {}

            // 6. Tables with row counts
            String tableSql = "SELECT TABLE_NAME, NUM_ROWS FROM USER_TABLES ORDER BY TABLE_NAME";
            try (Statement s = conn.createStatement(); ResultSet rs = s.executeQuery(tableSql)) {
                while (rs.next()) {
                    String tName = rs.getString("TABLE_NAME");
                    long rowCount = rs.getLong("NUM_ROWS");
                    if (rs.wasNull()) rowCount = -1;
                    List<SchemaMetadataDto.ColumnMetadata> cols = tableCols.getOrDefault(tName, List.of());
                    List<String> checks = checkMap.getOrDefault(tName, List.of());
                    List<String> uniques = uniqueMap.getOrDefault(tName, List.of());
                    tables.add(new SchemaMetadataDto.TableMetadata(tName, rowCount, cols, checks, uniques));
                }
            } catch (Exception ignored) {}

            // 7. Views
            String viewSql = "SELECT VIEW_NAME FROM USER_VIEWS ORDER BY VIEW_NAME";
            try (Statement s = conn.createStatement(); ResultSet rs = s.executeQuery(viewSql)) {
                while (rs.next()) {
                    views.add(new SchemaMetadataDto.ViewMetadata(rs.getString("VIEW_NAME")));
                }
            } catch (Exception ignored) {}

            // 8. Procedures and Functions
            String objSql = "SELECT OBJECT_NAME, OBJECT_TYPE, STATUS FROM USER_OBJECTS WHERE OBJECT_TYPE IN ('PROCEDURE', 'FUNCTION') ORDER BY OBJECT_NAME";
            try (Statement s = conn.createStatement(); ResultSet rs = s.executeQuery(objSql)) {
                while (rs.next()) {
                    String name = rs.getString("OBJECT_NAME");
                    String type = rs.getString("OBJECT_TYPE");
                    String status = rs.getString("STATUS");
                    if ("PROCEDURE".equalsIgnoreCase(type)) {
                        procedures.add(new SchemaMetadataDto.ProcedureMetadata(name, status));
                    } else if ("FUNCTION".equalsIgnoreCase(type)) {
                        functions.add(new SchemaMetadataDto.FunctionMetadata(name, status));
                    }
                }
            } catch (Exception ignored) {}

            // 9. Triggers
            String trgSql = "SELECT TRIGGER_NAME, TABLE_NAME, TRIGGERING_EVENT, STATUS FROM USER_TRIGGERS ORDER BY TRIGGER_NAME";
            try (Statement s = conn.createStatement(); ResultSet rs = s.executeQuery(trgSql)) {
                while (rs.next()) {
                    triggers.add(new SchemaMetadataDto.TriggerMetadata(
                            rs.getString("TRIGGER_NAME"),
                            rs.getString("TABLE_NAME"),
                            rs.getString("TRIGGERING_EVENT"),
                            rs.getString("STATUS")
                    ));
                }
            } catch (Exception ignored) {}

        } catch (Exception e) {
            log.error("Failed to load schema metadata: {}", e.getMessage(), e);
        }

        return new SchemaMetadataDto(tables, views, procedures, functions, triggers);
    }

    /**
     * Retrieves database connection and version info without exposing passwords.
     */
    public DatabaseConnectionInfoDto getConnectionInfo() {
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            String prod = meta.getDatabaseProductName();
            String ver = meta.getDatabaseProductVersion();
            String user = meta.getUserName();
            String driverVer = meta.getDriverVersion();

            return new DatabaseConnectionInfoDto(
                    prod != null ? prod : "Oracle Database",
                    ver != null ? ver : "Oracle Database 23c Free",
                    user != null ? user : "C##PLACEMENT_ADMIN",
                    "localhost",
                    1521,
                    "FREEPDB1",
                    "CONNECTED",
                    driverVer
            );
        } catch (Exception e) {
            return new DatabaseConnectionInfoDto(
                    "Oracle Database",
                    "Oracle Database 23c Free",
                    "C##PLACEMENT_ADMIN",
                    "localhost",
                    1521,
                    "FREEPDB1",
                    "DISCONNECTED",
                    "ojdbc11"
            );
        }
    }
}
