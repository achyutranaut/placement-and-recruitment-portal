package com.placement.portal.admin.compiler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.Reader;
import java.sql.*;
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

    /**
     * Executes SQL or PL/SQL in either QUERY mode (single statement) or SCRIPT mode (multi-statement script).
     */
    public SqlExecuteResponseDto execute(SqlExecuteRequestDto request) {
        if (request == null || request.getSql() == null || request.getSql().trim().isEmpty()) {
            return SqlExecuteResponseDto.errorResult("ORA-EMPTY", "SQL statement cannot be empty", "SQL statement cannot be empty", null, null, 0);
        }

        String rawSql = request.getSql().trim();
        String mode = request.getMode();

        List<String> statements = SqlScriptParser.splitScript(rawSql);
        if (statements.isEmpty()) {
            return SqlExecuteResponseDto.errorResult("ORA-EMPTY", "No executable statement found", "No executable statement found", null, null, 0);
        }

        // Automatically execute as multi-statement script if more than 1 statement is present or if explicitly requested
        if (statements.size() > 1 || "SCRIPT".equalsIgnoreCase(mode)) {
            return executeScript(rawSql);
        } else {
            return executeSingleQuery(statements.get(0));
        }
    }

    /**
     * Executes a single SQL or PL/SQL statement.
     */
    private SqlExecuteResponseDto executeSingleQuery(String rawSql) {
        long startTime = System.currentTimeMillis();
        String sql = SqlScriptParser.cleanStatement(rawSql, false);
        if (sql.isEmpty()) {
            return SqlExecuteResponseDto.errorResult("ORA-EMPTY", "No executable statement found", "No executable statement found", null, null, 0);
        }

        try (Connection conn = dataSource.getConnection()) {
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
                        }
                    } else {
                        int updateCount = stmt.getUpdateCount();
                        String upper = sql.replaceAll("--.*", "").trim().toUpperCase();

                        if (upper.startsWith("INSERT")) {
                            dto = SqlExecuteResponseDto.dmlResult("INSERT", Math.max(updateCount, 0), dbmsOutput, elapsed);
                        } else if (upper.startsWith("UPDATE")) {
                            dto = SqlExecuteResponseDto.dmlResult("UPDATE", Math.max(updateCount, 0), dbmsOutput, elapsed);
                        } else if (upper.startsWith("DELETE")) {
                            dto = SqlExecuteResponseDto.dmlResult("DELETE", Math.max(updateCount, 0), dbmsOutput, elapsed);
                        } else if (upper.startsWith("DECLARE") || upper.startsWith("BEGIN")) {
                            dto = SqlExecuteResponseDto.plsqlResult(dbmsOutput, elapsed);
                        } else if (upper.startsWith("CREATE") || upper.startsWith("ALTER") || upper.startsWith("DROP") || upper.startsWith("TRUNCATE")) {
                            String type = upper.split("\\s+")[0];
                            dto = SqlExecuteResponseDto.ddlResult(type, "Command executed successfully.", dbmsOutput, elapsed);
                        } else if (upper.startsWith("COMMIT")) {
                            dto = SqlExecuteResponseDto.tclResult("COMMIT", "Commit complete.", elapsed);
                        } else if (upper.startsWith("ROLLBACK")) {
                            dto = SqlExecuteResponseDto.tclResult("ROLLBACK", "Rollback complete.", elapsed);
                        } else if (upper.startsWith("SAVEPOINT")) {
                            dto = SqlExecuteResponseDto.tclResult("SAVEPOINT", "Savepoint created.", elapsed);
                        } else {
                            dto = SqlExecuteResponseDto.ddlResult("STATEMENT", "Statement executed successfully.", dbmsOutput, elapsed);
                        }
                    }
                    if (dto != null) {
                        dto.setSql(sql);
                    }
                    return dto;
                }
            }
        } catch (SQLException e) {
            long elapsed = System.currentTimeMillis() - startTime;
            String oraCode = formatErrorCode(e.getErrorCode());
            String fullError = e.getMessage();
            SqlExecuteResponseDto dto = SqlExecuteResponseDto.errorResult(oraCode, e.getMessage(), fullError, null, null, elapsed);
            dto.setSql(sql);
            return dto;
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            SqlExecuteResponseDto dto = SqlExecuteResponseDto.errorResult("ERROR", e.getMessage(), e.getMessage(), null, null, elapsed);
            dto.setSql(sql);
            return dto;
        }
    }

    /**
     * Executes a multi-statement script sequentially against the Oracle connection.
     */
    private SqlExecuteResponseDto executeScript(String rawScript) {
        long totalStart = System.currentTimeMillis();
        List<String> statements = SqlScriptParser.splitScript(rawScript);

        if (statements.isEmpty()) {
            return SqlExecuteResponseDto.errorResult("ORA-EMPTY", "No executable statements found in script", "No executable statements found in script", null, null, 0);
        }

        List<SqlExecuteResponseDto> results = new ArrayList<>();
        StringBuilder combinedDbms = new StringBuilder();

        try (Connection conn = dataSource.getConnection()) {
            enableDbmsOutput(conn);

            for (String stmtSql : statements) {
                long stmtStart = System.currentTimeMillis();
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
                            }
                        } else {
                            int updateCount = stmt.getUpdateCount();
                            String upper = stmtSql.replaceAll("--.*", "").trim().toUpperCase();

                            if (upper.startsWith("INSERT")) {
                                stmtResult = SqlExecuteResponseDto.dmlResult("INSERT", Math.max(updateCount, 0), out, elapsed);
                            } else if (upper.startsWith("UPDATE")) {
                                stmtResult = SqlExecuteResponseDto.dmlResult("UPDATE", Math.max(updateCount, 0), out, elapsed);
                            } else if (upper.startsWith("DELETE")) {
                                stmtResult = SqlExecuteResponseDto.dmlResult("DELETE", Math.max(updateCount, 0), out, elapsed);
                            } else if (upper.startsWith("DECLARE") || upper.startsWith("BEGIN")) {
                                stmtResult = SqlExecuteResponseDto.plsqlResult(out, elapsed);
                            } else if (upper.startsWith("CREATE") || upper.startsWith("ALTER") || upper.startsWith("DROP") || upper.startsWith("TRUNCATE")) {
                                String type = upper.split("\\s+")[0];
                                stmtResult = SqlExecuteResponseDto.ddlResult(type, "Command executed successfully.", out, elapsed);
                            } else if (upper.startsWith("COMMIT")) {
                                stmtResult = SqlExecuteResponseDto.tclResult("COMMIT", "Commit complete.", elapsed);
                            } else if (upper.startsWith("ROLLBACK")) {
                                stmtResult = SqlExecuteResponseDto.tclResult("ROLLBACK", "Rollback complete.", elapsed);
                            } else {
                                stmtResult = SqlExecuteResponseDto.ddlResult("STATEMENT", "Statement executed successfully.", out, elapsed);
                            }
                        }
                    }

                    if (stmtResult != null) {
                        stmtResult.setSql(stmtSql);
                        results.add(stmtResult);
                    }
                } catch (SQLException e) {
                    long elapsed = System.currentTimeMillis() - stmtStart;
                    String oraCode = formatErrorCode(e.getErrorCode());
                    SqlExecuteResponseDto errDto = SqlExecuteResponseDto.errorResult(oraCode, e.getMessage(), e.getMessage(), null, null, elapsed);
                    errDto.setSql(stmtSql);
                    results.add(errDto);
                    // Stop script on fatal error
                    break;
                }
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
                java.sql.Timestamp ts = rs.getTimestamp(colIndex);
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

            // 3. Tables and their columns
            Map<String, List<SchemaMetadataDto.ColumnMetadata>> tableCols = new LinkedHashMap<>();
            String colSql = """
                SELECT TABLE_NAME, COLUMN_NAME, DATA_TYPE, DATA_LENGTH, NULLABLE
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
                    String fkRef = fkMap.get(tName + "." + cName);

                    tableCols.computeIfAbsent(tName, k -> new ArrayList<>())
                            .add(new SchemaMetadataDto.ColumnMetadata(cName, dType, dLen, nullable, isPk, fkRef));
                }
            } catch (Exception ignored) {}

            // 4. Tables with row counts
            String tableSql = "SELECT TABLE_NAME, NUM_ROWS FROM USER_TABLES ORDER BY TABLE_NAME";
            try (Statement s = conn.createStatement(); ResultSet rs = s.executeQuery(tableSql)) {
                while (rs.next()) {
                    String tName = rs.getString("TABLE_NAME");
                    long rowCount = rs.getLong("NUM_ROWS");
                    if (rs.wasNull()) rowCount = -1;
                    List<SchemaMetadataDto.ColumnMetadata> cols = tableCols.getOrDefault(tName, List.of());
                    tables.add(new SchemaMetadataDto.TableMetadata(tName, rowCount, cols));
                }
            } catch (Exception ignored) {}

            // 5. Views
            String viewSql = "SELECT VIEW_NAME FROM USER_VIEWS ORDER BY VIEW_NAME";
            try (Statement s = conn.createStatement(); ResultSet rs = s.executeQuery(viewSql)) {
                while (rs.next()) {
                    views.add(new SchemaMetadataDto.ViewMetadata(rs.getString("VIEW_NAME")));
                }
            } catch (Exception ignored) {}

            // 6. Procedures and Functions
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

            // 7. Triggers
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
