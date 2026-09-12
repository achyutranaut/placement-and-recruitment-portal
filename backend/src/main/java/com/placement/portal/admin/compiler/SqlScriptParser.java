package com.placement.portal.admin.compiler;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class SqlScriptParser {

    private static final Pattern PLSQL_START_PATTERN = Pattern.compile(
            "^\\s*(DECLARE|BEGIN|CREATE\\s+(OR\\s+REPLACE\\s+)?(PROCEDURE|FUNCTION|TRIGGER|PACKAGE))\\b",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * Splits a multi-statement SQL/PLSQL script into individual executable statements.
     * Accurately handles PL/SQL blocks that contain internal semicolons and are terminated by '/'.
     */
    public static List<String> splitScript(String script) {
        List<String> statements = new ArrayList<>();
        if (script == null || script.trim().isEmpty()) {
            return statements;
        }

        String[] lines = script.split("\\r?\\n");
        StringBuilder currentStmt = new StringBuilder();
        boolean inPlsql = false;
        boolean inBlockComment = false;

        for (String line : lines) {
            String trimmedLine = line.trim();

            // Check for block comments
            if (trimmedLine.startsWith("/*")) {
                inBlockComment = true;
            }
            if (inBlockComment) {
                currentStmt.append(line).append("\n");
                if (trimmedLine.endsWith("*/") || trimmedLine.contains("*/")) {
                    inBlockComment = false;
                }
                continue;
            }

            // Standalone '/' delimiter
            if (trimmedLine.equals("/")) {
                if (currentStmt.length() > 0) {
                    String stmt = cleanStatement(currentStmt.toString(), inPlsql);
                    if (!stmt.isEmpty()) {
                        statements.add(stmt);
                    }
                    currentStmt.setLength(0);
                    inPlsql = false;
                }
                continue;
            }

            // Check if this line begins a PL/SQL block
            if (!inPlsql && currentStmt.toString().trim().isEmpty() && PLSQL_START_PATTERN.matcher(trimmedLine).find()) {
                inPlsql = true;
            }

            if (inPlsql) {
                // Inside PL/SQL block: accumulate lines until standalone '/' or end of script
                currentStmt.append(line).append("\n");
            } else {
                // Normal SQL statement: semicolons separate statements
                // We scan character by character to avoid semicolons inside quotes or comments
                boolean inSingleQuote = false;
                StringBuilder lineBuilder = new StringBuilder();

                for (int i = 0; i < line.length(); i++) {
                    char c = line.charAt(i);

                    // Check for single-line comment '--'
                    if (!inSingleQuote && c == '-' && i + 1 < line.length() && line.charAt(i + 1) == '-') {
                        // Append remainder of line as comment
                        lineBuilder.append(line.substring(i));
                        break;
                    }

                    if (c == '\'') {
                        // Check for escaped quote ''
                        if (inSingleQuote && i + 1 < line.length() && line.charAt(i + 1) == '\'') {
                            lineBuilder.append("''");
                            i++;
                            continue;
                        }
                        inSingleQuote = !inSingleQuote;
                    }

                    if (c == ';' && !inSingleQuote) {
                        currentStmt.append(lineBuilder);
                        String stmt = cleanStatement(currentStmt.toString(), false);
                        if (!stmt.isEmpty()) {
                            statements.add(stmt);
                        }
                        currentStmt.setLength(0);
                        lineBuilder.setLength(0);
                    } else {
                        lineBuilder.append(c);
                    }
                }

                if (lineBuilder.length() > 0) {
                    currentStmt.append(lineBuilder).append("\n");
                }
            }
        }

        // Remaining content at end of script
        if (currentStmt.length() > 0) {
            String stmt = cleanStatement(currentStmt.toString(), inPlsql);
            if (!stmt.isEmpty()) {
                statements.add(stmt);
            }
        }

        return statements;
    }

    /**
     * Prepares a single SQL/PLSQL statement for JDBC execution:
     * - Trims leading/trailing whitespace
     * - Removes trailing '/' delimiter if present
     * - Removes trailing semicolon for standard SQL (Oracle JDBC throws ORA-00911 if standard SQL ends with ';')
     * - Preserves internal semicolons for PL/SQL
     */
    public static String cleanStatement(String stmt, boolean isPlsqlHint) {
        if (stmt == null) return "";
        String s = stmt.trim();

        // Remove trailing slash if present
        if (s.endsWith("/")) {
            s = s.substring(0, s.length() - 1).trim();
        }

        boolean isPlsql = isPlsqlHint || isPlsql(s);

        if (!isPlsql) {
            // Standard SQL: remove trailing semicolon if present
            while (s.endsWith(";")) {
                s = s.substring(0, s.length() - 1).trim();
            }
        }

        return s;
    }

    /**
     * Detects if statement is PL/SQL
     */
    public static boolean isPlsql(String sql) {
        if (sql == null) return false;
        String clean = sql.replaceAll("--.*", "").trim();
        return PLSQL_START_PATTERN.matcher(clean).find();
    }
}
