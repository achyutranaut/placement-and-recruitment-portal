package com.placement.portal.admin.compiler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SqlDataIntegrityValidator {

    private static final Logger log = LoggerFactory.getLogger(SqlDataIntegrityValidator.class);

    private static final Pattern INSERT_PATTERN = Pattern.compile(
            "(?i)^\\s*INSERT\\s+INTO\\s+([\"a-zA-Z0-9_#$]+)\\s*(?:\\(([^)]+)\\))?\\s*(?:VALUES\\s*\\((.*)\\)|SELECT\\b.*)?$",
            Pattern.DOTALL
    );

    private static final Pattern UPDATE_PATTERN = Pattern.compile(
            "(?i)^\\s*UPDATE\\s+([\"a-zA-Z0-9_#$]+)\\s+SET\\s+(.*?)(?:\\s+WHERE\\b.*)?$",
            Pattern.DOTALL
    );

    public static class ValidationResult {
        private final boolean passed;
        private final String message;
        private final String diagnosticDetail;

        private ValidationResult(boolean passed, String message, String diagnosticDetail) {
            this.passed = passed;
            this.message = message;
            this.diagnosticDetail = diagnosticDetail;
        }

        public static ValidationResult passed() {
            return new ValidationResult(true, "Validation passed", null);
        }

        public static ValidationResult blocked(String message, String diagnosticDetail) {
            return new ValidationResult(false, message, diagnosticDetail);
        }

        public boolean isPassed() { return passed; }
        public String getMessage() { return message; }
        public String getDiagnosticDetail() { return diagnosticDetail; }
    }

    public static class ColumnInfo {
        private final String name;
        private final boolean nullable;
        private final boolean hasDefault;
        private final boolean isIdentity;

        public ColumnInfo(String name, boolean nullable, boolean hasDefault, boolean isIdentity) {
            this.name = name;
            this.nullable = nullable;
            this.hasDefault = hasDefault;
            this.isIdentity = isIdentity;
        }

        public String getName() { return name; }
        public boolean isNullable() { return nullable; }
        public boolean hasDefault() { return hasDefault; }
        public boolean isIdentity() { return isIdentity; }
    }

    /**
     * Pre-validates SQL statement against Oracle dictionary data integrity constraints.
     */
    public static ValidationResult validate(Connection conn, String rawSql, boolean strictMode) {
        if (!strictMode || rawSql == null) {
            return ValidationResult.passed();
        }

        String sql = rawSql.replaceAll("--.*", "").trim();
        String upper = sql.toUpperCase();

        if (upper.startsWith("INSERT")) {
            return validateInsert(conn, sql);
        } else if (upper.startsWith("UPDATE")) {
            return validateUpdate(conn, sql);
        }

        return ValidationResult.passed();
    }

    private static ValidationResult validateInsert(Connection conn, String sql) {
        Matcher matcher = INSERT_PATTERN.matcher(sql);
        if (!matcher.find()) {
            return ValidationResult.passed();
        }

        String rawTableName = matcher.group(1).replace("\"", "").trim();
        String rawColumns = matcher.group(2);
        String rawValues = matcher.group(3);

        Map<String, ColumnInfo> tableCols = fetchTableColumns(conn, rawTableName);
        if (tableCols.isEmpty()) {
            return ValidationResult.passed(); // Custom or non-existent table, let Oracle engine evaluate
        }

        if ("OFFER_LETTER".equalsIgnoreCase(rawTableName)) {
            ValidationResult smResult = validateOfferLetterInsert(conn, sql, rawColumns, rawValues);
            if (!smResult.isPassed()) {
                return smResult;
            }
        }

        if (rawColumns != null && !rawColumns.isBlank()) {
            List<String> suppliedCols = parseCommaTokens(rawColumns);
            List<String> normalizedSuppliedCols = suppliedCols.stream()
                    .map(c -> c.replace("\"", "").trim().toUpperCase())
                    .toList();

            // 1. Check for missing NOT NULL columns without database defaults
            for (ColumnInfo col : tableCols.values()) {
                if (!col.isNullable() && !col.hasDefault() && !col.isIdentity()) {
                    if (!normalizedSuppliedCols.contains(col.getName().toUpperCase())) {
                        String msg = String.format(
                                "EXECUTION BLOCKED: Strict Data Integrity Violation\n\n" +
                                "Missing mandatory column:\n%s\n\n" +
                                "%s.%s is defined as NOT NULL and has no database default.\n" +
                                "No database operation was executed.",
                                col.getName(), rawTableName.toUpperCase(), col.getName()
                        );
                        return ValidationResult.blocked(msg, "Missing NOT NULL column: " + col.getName());
                    }
                }
            }

            // 2. Check for explicit NULL in values for NOT NULL columns
            if (rawValues != null && !rawValues.isBlank()) {
                List<String> suppliedVals = parseCommaTokens(rawValues);
                int limit = Math.min(normalizedSuppliedCols.size(), suppliedVals.size());
                for (int i = 0; i < limit; i++) {
                    String colName = normalizedSuppliedCols.get(i);
                    String val = suppliedVals.get(i).trim();

                    if ("NULL".equalsIgnoreCase(val)) {
                        ColumnInfo col = tableCols.get(colName);
                        if (col != null && !col.isNullable()) {
                            String msg = String.format(
                                    "EXECUTION BLOCKED: Strict Data Integrity Violation\n\n" +
                                    "Column %s.%s does not allow NULL values.\n\n" +
                                    "No database changes were made.",
                                    rawTableName.toUpperCase(), col.getName()
                            );
                            return ValidationResult.blocked(msg, "Explicit NULL assigned to NOT NULL column: " + col.getName());
                        }
                    }
                }
            }
        } else if (rawValues != null && !rawValues.isBlank()) {
            // INSERT INTO table VALUES (...) without column list
            List<String> suppliedVals = parseCommaTokens(rawValues);
            List<ColumnInfo> orderedCols = new ArrayList<>(tableCols.values());

            int limit = Math.min(orderedCols.size(), suppliedVals.size());
            for (int i = 0; i < limit; i++) {
                String val = suppliedVals.get(i).trim();
                ColumnInfo col = orderedCols.get(i);

                if ("NULL".equalsIgnoreCase(val) && !col.isNullable()) {
                    String msg = String.format(
                            "EXECUTION BLOCKED: Strict Data Integrity Violation\n\n" +
                            "Column %s.%s does not allow NULL values.\n\n" +
                            "No database changes were made.",
                            rawTableName.toUpperCase(), col.getName()
                    );
                    return ValidationResult.blocked(msg, "Explicit NULL assigned to NOT NULL column: " + col.getName());
                }
            }
        }

        return ValidationResult.passed();
    }

    private static ValidationResult validateUpdate(Connection conn, String sql) {
        Matcher matcher = UPDATE_PATTERN.matcher(sql);
        if (!matcher.find()) {
            return ValidationResult.passed();
        }

        String rawTableName = matcher.group(1).replace("\"", "").trim();
        String setClause = matcher.group(2);

        Map<String, ColumnInfo> tableCols = fetchTableColumns(conn, rawTableName);
        if (tableCols.isEmpty()) {
            return ValidationResult.passed();
        }

        if ("APPLICATION".equalsIgnoreCase(rawTableName)) {
            ValidationResult smResult = validateApplicationStatusUpdate(conn, sql, setClause);
            if (!smResult.isPassed()) {
                return smResult;
            }
        }

        List<String> assignments = parseCommaTokens(setClause);
        for (String assign : assignments) {
            String[] parts = assign.split("=", 2);
            if (parts.length == 2) {
                String colName = parts[0].replace("\"", "").trim().toUpperCase();
                String val = parts[1].trim();

                if ("NULL".equalsIgnoreCase(val)) {
                    ColumnInfo col = tableCols.get(colName);
                    if (col != null && !col.isNullable()) {
                        String msg = String.format(
                                "EXECUTION BLOCKED: Strict Data Integrity Violation\n\n" +
                                "Column %s.%s is defined as NOT NULL and cannot be updated to NULL.\n\n" +
                                "No database changes were made.",
                                rawTableName.toUpperCase(), col.getName()
                        );
                        return ValidationResult.blocked(msg, "Attempted to UPDATE NOT NULL column to NULL: " + col.getName());
                    }
                }
            }
        }

        return ValidationResult.passed();
    }

    private static ValidationResult validateApplicationStatusUpdate(Connection conn, String sql, String setClause) {
        String targetStatus = null;
        List<String> assignments = parseCommaTokens(setClause);
        for (String assign : assignments) {
            String[] parts = assign.split("=", 2);
            if (parts.length == 2 && parts[0].replace("\"", "").trim().equalsIgnoreCase("STATUS")) {
                targetStatus = parts[1].replace("'", "").replace("\"", "").trim().toUpperCase();
                break;
            }
        }

        if (targetStatus == null) {
            return ValidationResult.passed();
        }

        // Extract application ID if present in WHERE clause
        Matcher appMatcher = Pattern.compile("(?i)Application_Id\\s*=\\s*'?([a-zA-Z0-9_-]+)'?").matcher(sql);
        String appId = appMatcher.find() ? appMatcher.group(1).trim() : null;

        if (appId != null) {
            String currStatus = null;
            try (PreparedStatement ps = conn.prepareStatement("SELECT Status FROM APPLICATION WHERE Application_Id = ?")) {
                ps.setString(1, appId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        currStatus = rs.getString(1);
                    }
                }
            } catch (Exception ignored) {}

            if (currStatus != null) {
                currStatus = currStatus.toUpperCase();

                if ("SELECTED".equals(targetStatus)) {
                    if (!"INTERVIEWING".equals(currStatus)) {
                        String msg = String.format(
                                "EXECUTION BLOCKED: Placement Pipeline State Machine Violation\n\n" +
                                "Cannot transition application %s from %s to SELECTED.\n" +
                                "Candidate must progress through SHORTLISTED -> INTERVIEWING first.",
                                appId, currStatus
                        );
                        return ValidationResult.blocked(msg, "Invalid transition: " + currStatus + " -> SELECTED");
                    }

                    // Check PL/SQL readiness if available
                    try (PreparedStatement ps = conn.prepareStatement("SELECT GET_APPLICATION_READINESS(?) FROM DUAL")) {
                        ps.setString(1, appId);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) {
                                String readiness = rs.getString(1);
                                if (readiness != null && !readiness.startsWith("READY")) {
                                    String msg = String.format(
                                            "EXECUTION BLOCKED: Placement Pipeline State Machine Violation\n\n" +
                                            "Candidate %s is not eligible for selection:\n%s\n\n" +
                                            "All mandatory interview rounds must be completed with passing results.",
                                            appId, readiness
                                    );
                                    return ValidationResult.blocked(msg, readiness);
                                }
                            }
                        }
                    } catch (Exception ignored) {}
                } else if ("OFFERED".equals(targetStatus) && !"SELECTED".equals(currStatus)) {
                    String msg = String.format(
                            "EXECUTION BLOCKED: Placement Pipeline State Machine Violation\n\n" +
                            "Cannot transition application %s from %s to OFFERED.\n" +
                            "An offer can only be released after candidate is formally SELECTED.",
                            appId, currStatus
                    );
                    return ValidationResult.blocked(msg, "Invalid transition: " + currStatus + " -> OFFERED");
                } else if ("ACCEPTED".equals(targetStatus) && !"OFFERED".equals(currStatus)) {
                    String msg = String.format(
                            "EXECUTION BLOCKED: Placement Pipeline State Machine Violation\n\n" +
                            "Cannot transition application %s from %s to ACCEPTED.\n" +
                            "An offer must exist in OFFERED status before it can be accepted.",
                            appId, currStatus
                    );
                    return ValidationResult.blocked(msg, "Invalid transition: " + currStatus + " -> ACCEPTED");
                }
            }
        }

        return ValidationResult.passed();
    }

    private static ValidationResult validateOfferLetterInsert(Connection conn, String sql, String rawColumns, String rawValues) {
        String appId = null;
        if (rawColumns != null && rawValues != null) {
            List<String> cols = parseCommaTokens(rawColumns);
            List<String> vals = parseCommaTokens(rawValues);
            for (int i = 0; i < Math.min(cols.size(), vals.size()); i++) {
                if (cols.get(i).replace("\"", "").trim().equalsIgnoreCase("APPLICATION_ID")) {
                    appId = vals.get(i).replace("'", "").replace("\"", "").trim();
                    break;
                }
            }
        }

        if (appId == null) {
            Matcher m = Pattern.compile("(?i)'?(APP[0-9]+)'?").matcher(sql);
            if (m.find()) {
                appId = m.group(1).trim();
            }
        }

        if (appId != null) {
            String appStatus = null;
            try (PreparedStatement ps = conn.prepareStatement("SELECT Status FROM APPLICATION WHERE Application_Id = ?")) {
                ps.setString(1, appId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        appStatus = rs.getString(1);
                    }
                }
            } catch (Exception ignored) {}

            if (appStatus != null && !"SELECTED".equalsIgnoreCase(appStatus)) {
                String msg = String.format(
                        "EXECUTION BLOCKED: Placement Pipeline State Machine Violation\n\n" +
                        "Cannot issue offer for application %s: Current status is %s, but must be SELECTED.\n" +
                        "Direct offer creation bypassing the interview process is strictly prohibited.",
                        appId, appStatus
                );
                return ValidationResult.blocked(msg, "Cannot issue offer for non-SELECTED application");
            }
        }

        return ValidationResult.passed();
    }

    private static Map<String, ColumnInfo> fetchTableColumns(Connection conn, String tableName) {
        Map<String, ColumnInfo> cols = new LinkedHashMap<>();
        String normalizedTable = tableName.trim().toUpperCase();

        // 1. Try Oracle Dictionary USER_TAB_COLUMNS
        String oraSql = """
            SELECT COLUMN_NAME, NULLABLE, DATA_DEFAULT, IDENTITY_COLUMN
            FROM USER_TAB_COLUMNS
            WHERE TABLE_NAME = ?
            ORDER BY COLUMN_ID
        """;
        try (PreparedStatement ps = conn.prepareStatement(oraSql)) {
            ps.setString(1, normalizedTable);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String col = rs.getString("COLUMN_NAME").toUpperCase();
                    boolean nullable = "Y".equalsIgnoreCase(rs.getString("NULLABLE"));
                    String def = rs.getString("DATA_DEFAULT");
                    boolean hasDef = def != null && !def.isBlank();
                    String identity = rs.getString("IDENTITY_COLUMN");
                    boolean isIdent = "YES".equalsIgnoreCase(identity);

                    cols.put(col, new ColumnInfo(col, nullable, hasDef, isIdent));
                }
            }
        } catch (Exception e) {
            log.debug("USER_TAB_COLUMNS query failed: {}", e.getMessage());
        }

        // 2. Fallback to DatabaseMetaData if empty or non-Oracle dialect (e.g. H2 test profile)
        if (cols.isEmpty()) {
            try {
                DatabaseMetaData meta = conn.getMetaData();
                try (ResultSet rs = meta.getColumns(null, null, normalizedTable, null)) {
                    while (rs.next()) {
                        String col = rs.getString("COLUMN_NAME").toUpperCase();
                        int nullableInt = rs.getInt("NULLABLE");
                        boolean nullable = (nullableInt != DatabaseMetaData.columnNoNulls);
                        String def = rs.getString("COLUMN_DEF");
                        boolean hasDef = def != null && !def.isBlank();
                        String isAuto = null;
                        try {
                            isAuto = rs.getString("IS_AUTOINCREMENT");
                        } catch (Exception ignored) {}
                        boolean isIdent = "YES".equalsIgnoreCase(isAuto);

                        cols.put(col, new ColumnInfo(col, nullable, hasDef, isIdent));
                    }
                }
            } catch (Exception e) {
                log.debug("DatabaseMetaData.getColumns fallback failed: {}", e.getMessage());
            }
        }

        return cols;
    }

    /**
     * Splits comma-separated items while respecting single and double quoted literals and nested parentheses.
     */
    public static List<String> parseCommaTokens(String text) {
        List<String> tokens = new ArrayList<>();
        if (text == null) return tokens;

        StringBuilder sb = new StringBuilder();
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        int parenDepth = 0;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (c == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
                sb.append(c);
            } else if (c == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
                sb.append(c);
            } else if (!inSingleQuote && !inDoubleQuote) {
                if (c == '(') {
                    parenDepth++;
                    sb.append(c);
                } else if (c == ')') {
                    parenDepth--;
                    sb.append(c);
                } else if (c == ',' && parenDepth == 0) {
                    tokens.add(sb.toString().trim());
                    sb.setLength(0);
                } else {
                    sb.append(c);
                }
            } else {
                sb.append(c);
            }
        }

        if (sb.length() > 0) {
            tokens.add(sb.toString().trim());
        }

        return tokens;
    }
}
