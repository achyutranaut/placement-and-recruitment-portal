package com.placement.portal.neo4j;

import com.placement.portal.admin.compiler.SqlExecuteResponseDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("h2")
class CypherSecurityTests {

    @Autowired
    private Neo4jCypherService cypherService;

    @Test
    @DisplayName("Safe read-only Cypher queries should pass validation")
    void testSafeCypherQueriesPass() {
        assertDoesNotThrow(() -> CypherReadOnlyValidator.validate("MATCH (s:Student) RETURN s LIMIT 10"));
        assertDoesNotThrow(() -> CypherReadOnlyValidator.validate("OPTIONAL MATCH (s:Student)-[r:APPLIED_FOR]->(d:Drive) RETURN s, r, d"));
        assertDoesNotThrow(() -> CypherReadOnlyValidator.validate("WITH 1 AS num RETURN num"));
        assertDoesNotThrow(() -> CypherReadOnlyValidator.validate("UNWIND [1, 2, 3] AS x RETURN x"));
        assertDoesNotThrow(() -> CypherReadOnlyValidator.validate("CALL db.labels() YIELD label RETURN label"));
        assertDoesNotThrow(() -> CypherReadOnlyValidator.validate("CALL db.relationshipTypes() YIELD relationshipType RETURN relationshipType"));
        assertDoesNotThrow(() -> CypherReadOnlyValidator.validate("SHOW CONSTRAINTS"));
    }

    @Test
    @DisplayName("Mutating Cypher statements must be rejected")
    void testMutatingCypherRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                CypherReadOnlyValidator.validate("CREATE (n:TestNode {name: 'Malicious'})"));

        assertThrows(IllegalArgumentException.class, () ->
                CypherReadOnlyValidator.validate("MERGE (n:Student {studentId: 'STU999'})"));

        assertThrows(IllegalArgumentException.class, () ->
                CypherReadOnlyValidator.validate("MATCH (n:Student) SET n.cgpa = 10.0"));

        assertThrows(IllegalArgumentException.class, () ->
                CypherReadOnlyValidator.validate("MATCH (n) DELETE n"));

        assertThrows(IllegalArgumentException.class, () ->
                CypherReadOnlyValidator.validate("MATCH (n) DETACH DELETE n"));

        assertThrows(IllegalArgumentException.class, () ->
                CypherReadOnlyValidator.validate("MATCH (n:Student) REMOVE n.cgpa"));

        assertThrows(IllegalArgumentException.class, () ->
                CypherReadOnlyValidator.validate("DROP CONSTRAINT student_id_unique"));

        assertThrows(IllegalArgumentException.class, () ->
                CypherReadOnlyValidator.validate("ALTER CURRENT USER SET PASSWORD FROM 'a' TO 'b'"));

        assertThrows(IllegalArgumentException.class, () ->
                CypherReadOnlyValidator.validate("LOAD CSV FROM 'file:///etc/passwd' AS row RETURN row"));

        assertThrows(IllegalArgumentException.class, () ->
                CypherReadOnlyValidator.validate("FOREACH (x in [1,2] | CREATE (n:Node))"));
    }

    @Test
    @DisplayName("Unauthorized procedure calls must be rejected")
    void testUnauthorizedCallsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                CypherReadOnlyValidator.validate("CALL apoc.load.json('http://attacker.com')"));

        assertThrows(IllegalArgumentException.class, () ->
                CypherReadOnlyValidator.validate("CALL dbms.security.createUser('attacker', 'pass', false)"));
    }

    @Test
    @DisplayName("Multiple Cypher statements must be rejected")
    void testMultipleStatementsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                CypherReadOnlyValidator.validate("MATCH (s:Student) RETURN s; MATCH (c:Company) RETURN c;"));

        assertThrows(IllegalArgumentException.class, () ->
                CypherReadOnlyValidator.validate("MATCH (s:Student) RETURN s; DROP CONSTRAINT c;"));
    }

    @Test
    @DisplayName("Comment obfuscation attempts must not bypass write validation")
    void testCommentBypassAttempts() {
        assertThrows(IllegalArgumentException.class, () ->
                CypherReadOnlyValidator.validate("MATCH (s:Student) /* safe comment */ DELETE s"));

        assertThrows(IllegalArgumentException.class, () ->
                CypherReadOnlyValidator.validate("// first comment\nCREATE (s:Fake) RETURN s"));

        assertThrows(IllegalArgumentException.class, () ->
                CypherReadOnlyValidator.validate("# hash comment\nMERGE (s:Fake) RETURN s"));
    }

    @Test
    @DisplayName("CypherService returns CYPHER-POLICY-BLOCKED for write queries")
    void testCypherServiceReturnsPolicyBlocked() {
        SqlExecuteResponseDto response = cypherService.execute("CREATE (s:MaliciousNode) RETURN s");
        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals("CYPHER-POLICY-BLOCKED", response.getErrorCode());
        assertEquals("BLOCKED", response.getStatementType());
        assertTrue(response.getErrorMessage().contains("prohibited"));
    }
}
