package com.placement.portal.admin.compiler;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SqlSanitizer {

    private static final Pattern SENSITIVE_KV_PATTERN = Pattern.compile(
            "(?i)\\b(password|password_hash|passwd|secret|jwt_secret|token|api_key)\\b\\s*=\\s*('[^']*'|\"[^\"]*\"|\\S+)"
    );

    private static final Pattern IDENTIFIED_BY_PATTERN = Pattern.compile(
            "(?i)\\bIDENTIFIED\\s+BY\\s+('[^']*'|\"[^\"]*\"|\\S+)"
    );

    private static final Pattern INSERT_PATTERN = Pattern.compile(
            "(?i)\\bINSERT\\s+INTO\\s+([A-Za-z0-9_#$.\"]+)\\s*\\(([^)]+)\\)\\s*VALUES\\s*\\(([^)]+)\\)",
            Pattern.DOTALL
    );

    private static final Pattern SENSITIVE_COL_NAME = Pattern.compile(
            "(?i).*(password|secret|token|api_key|passwd).*"
    );

    /**
     * Sanitizes SQL text by masking passwords, secrets, and auth tokens with '[REDACTED]'.
     */
    public static String sanitize(String sql) {
        if (sql == null || sql.isBlank()) {
            return sql;
        }

        String result = sql;

        // 1. Redact key-value pairs (e.g. SET PASSWORD = 'xxx')
        Matcher kvMatcher = SENSITIVE_KV_PATTERN.matcher(result);
        StringBuffer sbKv = new StringBuffer();
        while (kvMatcher.find()) {
            String key = kvMatcher.group(1);
            kvMatcher.appendReplacement(sbKv, Matcher.quoteReplacement(key + " = '[REDACTED]'"));
        }
        kvMatcher.appendTail(sbKv);
        result = sbKv.toString();

        // 2. Redact IDENTIFIED BY 'xxx'
        Matcher idMatcher = IDENTIFIED_BY_PATTERN.matcher(result);
        StringBuffer sbId = new StringBuffer();
        while (idMatcher.find()) {
            idMatcher.appendReplacement(sbId, "IDENTIFIED BY '[REDACTED]'");
        }
        idMatcher.appendTail(sbId);
        result = sbId.toString();

        // 3. Redact INSERT INTO table (col1, password, ...) VALUES (val1, 'secret', ...)
        Matcher insertMatcher = INSERT_PATTERN.matcher(result);
        StringBuffer sbInsert = new StringBuffer();
        while (insertMatcher.find()) {
            String table = insertMatcher.group(1);
            String colsStr = insertMatcher.group(2);
            String valsStr = insertMatcher.group(3);

            List<String> cols = splitCsv(colsStr);
            List<String> vals = splitCsv(valsStr);

            if (cols.size() == vals.size()) {
                StringBuilder sanitizedVals = new StringBuilder();
                for (int i = 0; i < cols.size(); i++) {
                    if (i > 0) sanitizedVals.append(", ");
                    String col = cols.get(i).trim();
                    if (SENSITIVE_COL_NAME.matcher(col).matches()) {
                        sanitizedVals.append("'[REDACTED]'");
                    } else {
                        sanitizedVals.append(vals.get(i).trim());
                    }
                }
                insertMatcher.appendReplacement(sbInsert, Matcher.quoteReplacement(
                        "INSERT INTO " + table + " (" + colsStr + ") VALUES (" + sanitizedVals + ")"
                ));
            } else {
                insertMatcher.appendReplacement(sbInsert, Matcher.quoteReplacement(insertMatcher.group(0)));
            }
        }
        insertMatcher.appendTail(sbInsert);
        result = sbInsert.toString();

        // 4. Redact raw hex JWT secrets (64 hex characters)
        result = result.replaceAll("(?i)'[0-9a-f]{64}'", "'[REDACTED_SECRET]'");

        return result;
    }

    private static List<String> splitCsv(String text) {
        List<String> list = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        int parenDepth = 0;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
                cur.append(c);
            } else if (c == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
                cur.append(c);
            } else if (c == '(' && !inSingleQuote && !inDoubleQuote) {
                parenDepth++;
                cur.append(c);
            } else if (c == ')' && !inSingleQuote && !inDoubleQuote) {
                parenDepth--;
                cur.append(c);
            } else if (c == ',' && !inSingleQuote && !inDoubleQuote && parenDepth == 0) {
                list.add(cur.toString().trim());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        if (cur.length() > 0) {
            list.add(cur.toString().trim());
        }
        return list;
    }
}
