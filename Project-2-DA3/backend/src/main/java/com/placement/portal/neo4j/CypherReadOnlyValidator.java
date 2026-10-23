package com.placement.portal.neo4j;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Server-side validation engine strictly enforcing read-only Cypher policies.
 * Prevents graph mutation, destructive drops, CSV loading, and procedural bypasses.
 */
public final class CypherReadOnlyValidator {

    private static final Pattern BLOCK_COMMENT = Pattern.compile("/\\*[\\s\\S]*?\\*/");
    private static final Pattern LINE_COMMENT_SLASH = Pattern.compile("//[^\r\n]*");
    private static final Pattern LINE_COMMENT_HASH = Pattern.compile("#[^\r\n]*");

    private static final Pattern WRITE_KEYWORDS = Pattern.compile(
            "\\b(CREATE|MERGE|SET|DELETE|DETACH\\s+DELETE|REMOVE|DROP|ALTER|FOREACH|LOAD\\s+CSV)\\b",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern DISALLOWED_CALLS = Pattern.compile(
            "\\bCALL\\s+(?!db\\.(labels|relationshipTypes|propertyKeys|schema|indexes|constraints))",
            Pattern.CASE_INSENSITIVE
    );

    private CypherReadOnlyValidator() {}

    /**
     * Validates that the supplied Cypher query is a single, safe read-only statement.
     * Throws IllegalArgumentException on any violation.
     */
    public static void validate(String rawCypher) {
        if (rawCypher == null || rawCypher.isBlank()) {
            throw new IllegalArgumentException("Cypher query cannot be empty.");
        }

        // 1. Strip comments
        String cleaned = BLOCK_COMMENT.matcher(rawCypher).replaceAll(" ");
        cleaned = LINE_COMMENT_SLASH.matcher(cleaned).replaceAll(" ");
        cleaned = LINE_COMMENT_HASH.matcher(cleaned).replaceAll(" ").trim();

        if (cleaned.isEmpty()) {
            throw new IllegalArgumentException("Cypher query contains only comments.");
        }

        // 2. Reject multiple statements: check for semicolons outside string literals
        validateSingleStatement(cleaned);

        // 3. Mask string literals so keyword detection does not false-positive on string content
        String masked = maskLiterals(cleaned);

        // 4. Reject any mutating write keywords
        Matcher writeMatcher = WRITE_KEYWORDS.matcher(masked);
        if (writeMatcher.find()) {
            throw new IllegalArgumentException(
                    "Cypher query rejected: Mutating keyword '" + writeMatcher.group(1).toUpperCase(Locale.ROOT) +
                    "' is strictly prohibited. The compiler only permits read-only MATCH/RETURN queries."
            );
        }

        // 5. Reject unauthorized procedure calls
        Matcher callMatcher = DISALLOWED_CALLS.matcher(masked);
        if (callMatcher.find()) {
            throw new IllegalArgumentException(
                    "Cypher query rejected: Unauthorized CALL procedure. Only read-only schema introspection (CALL db.*) is permitted."
            );
        }

        // 6. Ensure query starts with an allowed read-only clause
        String firstWord = getFirstWord(masked).toUpperCase(Locale.ROOT);
        if (!firstWord.equals("MATCH") &&
            !firstWord.equals("OPTIONAL") &&
            !firstWord.equals("RETURN") &&
            !firstWord.equals("WITH") &&
            !firstWord.equals("UNWIND") &&
            !firstWord.equals("CALL") &&
            !firstWord.equals("SHOW")) {
            throw new IllegalArgumentException(
                    "Cypher query rejected: Must start with a read-only clause (MATCH, OPTIONAL MATCH, RETURN, WITH, UNWIND, CALL db.*, SHOW)."
            );
        }
    }

    private static void validateSingleStatement(String cypher) {
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean inBacktick = false;

        for (int i = 0; i < cypher.length(); i++) {
            char c = cypher.charAt(i);
            if (c == '\'' && !inDoubleQuote && !inBacktick) {
                if (i == 0 || cypher.charAt(i - 1) != '\\') {
                    inSingleQuote = !inSingleQuote;
                }
            } else if (c == '"' && !inSingleQuote && !inBacktick) {
                if (i == 0 || cypher.charAt(i - 1) != '\\') {
                    inDoubleQuote = !inDoubleQuote;
                }
            } else if (c == '`' && !inSingleQuote && !inDoubleQuote) {
                inBacktick = !inBacktick;
            } else if (c == ';' && !inSingleQuote && !inDoubleQuote && !inBacktick) {
                // If there are non-whitespace characters following the semicolon, reject
                String remainder = cypher.substring(i + 1).trim();
                if (!remainder.isEmpty()) {
                    throw new IllegalArgumentException(
                            "Cypher query rejected: Multiple statements are not permitted in the query compiler."
                    );
                }
            }
        }
    }

    private static String maskLiterals(String cypher) {
        StringBuilder sb = new StringBuilder(cypher.length());
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;

        for (int i = 0; i < cypher.length(); i++) {
            char c = cypher.charAt(i);
            if (c == '\'' && !inDoubleQuote) {
                if (i == 0 || cypher.charAt(i - 1) != '\\') {
                    inSingleQuote = !inSingleQuote;
                }
                sb.append(' ');
            } else if (c == '"' && !inSingleQuote) {
                if (i == 0 || cypher.charAt(i - 1) != '\\') {
                    inDoubleQuote = !inDoubleQuote;
                }
                sb.append(' ');
            } else if (inSingleQuote || inDoubleQuote) {
                sb.append(' ');
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String getFirstWord(String cypher) {
        String trimmed = cypher.trim();
        int spaceIdx = -1;
        for (int i = 0; i < trimmed.length(); i++) {
            if (Character.isWhitespace(trimmed.charAt(i))) {
                spaceIdx = i;
                break;
            }
        }
        return spaceIdx == -1 ? trimmed : trimmed.substring(0, spaceIdx);
    }
}
