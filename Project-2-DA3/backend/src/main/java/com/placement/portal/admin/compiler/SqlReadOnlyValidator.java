package com.placement.portal.admin.compiler;

import java.util.*;

/**
 * Server-side validator enforcing read-only query policy for the SQL editor.
 * Guarantees that only a single, read-only SELECT statement (including safe CTEs)
 * can be executed, regardless of any client-supplied parameters such as strictMode.
 */
public class SqlReadOnlyValidator {

    private static final Set<String> FORBIDDEN_KEYWORDS = Set.of(
            "INSERT", "UPDATE", "DELETE", "MERGE",
            "DROP", "ALTER", "CREATE", "TRUNCATE", "RENAME",
            "GRANT", "REVOKE",
            "EXEC", "EXECUTE", "CALL",
            "DECLARE", "BEGIN", "END",
            "COMMIT", "ROLLBACK", "SAVEPOINT",
            "LOCK", "SHUTDOWN"
    );

    public record ValidationResult(boolean passed, String errorMessage) {
        public static ValidationResult ok() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult fail(String msg) {
            return new ValidationResult(false, msg);
        }
    }

    public static ValidationResult validate(String rawSql) {
        if (rawSql == null || rawSql.trim().isEmpty()) {
            return ValidationResult.fail("SQL statement cannot be empty.");
        }

        // 1. Strip comments while respecting string literals and quoted identifiers
        String stripped = stripComments(rawSql).trim();
        if (stripped.isEmpty()) {
            return ValidationResult.fail("No executable SQL statement found.");
        }

        // 2. Tokenize and check for multiple statements (semicolon outside quotes)
        List<String> tokens = tokenize(stripped);
        if (tokens.isEmpty()) {
            return ValidationResult.fail("No executable SQL tokens found.");
        }

        // 3. Reject multiple statements separated by semicolons
        int semicolonCount = 0;
        for (int i = 0; i < tokens.size(); i++) {
            if (tokens.get(i).equals(";")) {
                semicolonCount++;
                // If there are meaningful tokens after the semicolon, reject multiple statements
                if (i < tokens.size() - 1 && !tokens.get(i + 1).equals(";")) {
                    return ValidationResult.fail("Execution blocked: Multiple statements are not permitted in the SQL editor. Only a single read-only SELECT statement is allowed.");
                }
            }
        }

        // 4. Inspect the starting keyword (first token)
        String firstToken = tokens.get(0).toUpperCase(Locale.ROOT);

        if (!firstToken.equals("SELECT") && !firstToken.equals("WITH")) {
            if (FORBIDDEN_KEYWORDS.contains(firstToken)) {
                return ValidationResult.fail("Execution blocked: " + firstToken + " statements are strictly prohibited in the read-only SQL editor.");
            }
            return ValidationResult.fail("Execution blocked: Only read-only SELECT queries or safe Common Table Expressions (WITH ... SELECT) are permitted in the SQL editor.");
        }

        // 5. Scan all tokens for forbidden DML, DDL, PL/SQL, or TCL keywords
        for (int i = 0; i < tokens.size(); i++) {
            String token = tokens.get(i).toUpperCase(Locale.ROOT);

            if (FORBIDDEN_KEYWORDS.contains(token)) {
                return ValidationResult.fail("Execution blocked: Prohibited keyword detected in statement: '" + token + "'. Only read-only queries are permitted.");
            }

            // Check for SELECT ... INTO ... (PL/SQL or table creation construct)
            if (token.equals("INTO")) {
                return ValidationResult.fail("Execution blocked: 'INTO' clause is prohibited in the read-only SQL editor.");
            }

            // Check for FOR UPDATE row locking
            if (token.equals("FOR") && i + 1 < tokens.size() && tokens.get(i + 1).equalsIgnoreCase("UPDATE")) {
                return ValidationResult.fail("Execution blocked: 'FOR UPDATE' locking clause is prohibited in the read-only SQL editor.");
            }
        }

        // 6. If CTE (WITH ...), ensure it culminates in a SELECT statement
        if (firstToken.equals("WITH")) {
            boolean hasSelect = tokens.stream().anyMatch(t -> t.equalsIgnoreCase("SELECT"));
            if (!hasSelect) {
                return ValidationResult.fail("Execution blocked: Common Table Expression (WITH) must culminate in a SELECT statement.");
            }
        }

        return ValidationResult.ok();
    }

    /**
     * Strips SQL block comments /* ... * / and line comments -- ...
     * while preserving quoted strings '...' and quoted identifiers "...".
     */
    public static String stripComments(String sql) {
        StringBuilder sb = new StringBuilder();
        int len = sql.length();
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean inBlockComment = false;
        boolean inLineComment = false;

        for (int i = 0; i < len; i++) {
            char c = sql.charAt(i);
            char next = (i + 1 < len) ? sql.charAt(i + 1) : '\0';

            if (inBlockComment) {
                if (c == '*' && next == '/') {
                    inBlockComment = false;
                    i++;
                }
                continue;
            }

            if (inLineComment) {
                if (c == '\n' || c == '\r') {
                    inLineComment = false;
                    sb.append(c);
                }
                continue;
            }

            if (inSingleQuote) {
                sb.append(c);
                if (c == '\'') {
                    if (next == '\'') {
                        sb.append(next);
                        i++;
                    } else {
                        inSingleQuote = false;
                    }
                }
                continue;
            }

            if (inDoubleQuote) {
                sb.append(c);
                if (c == '"') {
                    inDoubleQuote = false;
                }
                continue;
            }

            // Check for start of block comment
            if (c == '/' && next == '*') {
                inBlockComment = true;
                i++;
                continue;
            }

            // Check for start of line comment
            if (c == '-' && next == '-') {
                inLineComment = true;
                i++;
                continue;
            }

            if (c == '\'') {
                inSingleQuote = true;
                sb.append(c);
                continue;
            }

            if (c == '"') {
                inDoubleQuote = true;
                sb.append(c);
                continue;
            }

            sb.append(c);
        }

        return sb.toString();
    }

    /**
     * Splits SQL into tokens outside of single-quoted string literals.
     */
    private static List<String> tokenize(String sql) {
        List<String> tokens = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inSingleQuote = false;

        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);

            if (c == '\'') {
                if (inSingleQuote && i + 1 < sql.length() && sql.charAt(i + 1) == '\'') {
                    i++; // skip escaped quote
                    continue;
                }
                inSingleQuote = !inSingleQuote;
                if (!inSingleQuote && cur.length() > 0) {
                    cur.setLength(0);
                }
                continue;
            }

            if (inSingleQuote) {
                continue; // Ignore characters inside literal strings
            }

            if (Character.isWhitespace(c)) {
                if (cur.length() > 0) {
                    tokens.add(cur.toString());
                    cur.setLength(0);
                }
            } else if (c == ';' || c == '(' || c == ')' || c == ',') {
                if (cur.length() > 0) {
                    tokens.add(cur.toString());
                    cur.setLength(0);
                }
                tokens.add(String.valueOf(c));
            } else {
                cur.append(c);
            }
        }

        if (cur.length() > 0) {
            tokens.add(cur.toString());
        }

        return tokens;
    }
}
