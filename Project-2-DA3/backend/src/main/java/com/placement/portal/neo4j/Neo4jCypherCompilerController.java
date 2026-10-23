package com.placement.portal.neo4j;

import com.placement.portal.admin.compiler.DatabaseConnectionInfoDto;
import com.placement.portal.admin.compiler.SchemaMetadataDto;
import com.placement.portal.admin.compiler.SqlExecuteRequestDto;
import com.placement.portal.admin.compiler.SqlExecuteResponseDto;
import com.placement.portal.admin.compiler.SqlExecutionAuditDto;
import com.placement.portal.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/cypher")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Cypher Compiler", description = "Dedicated Neo4j Aura Cypher Compiler environment for authorized administrators")
public class Neo4jCypherCompilerController {

    private final Neo4jCypherService cypherService;

    public Neo4jCypherCompilerController(Neo4jCypherService cypherService) {
        this.cypherService = cypherService;
    }

    @PostMapping("/execute")
    @Operation(summary = "Execute declarative Cypher query against Neo4j Aura graph database")
    public ResponseEntity<ApiResponse<SqlExecuteResponseDto>> executeCypher(@RequestBody SqlExecuteRequestDto request) {
        String cypher = request != null ? request.getSql() : "";
        SqlExecuteResponseDto response = cypherService.execute(cypher);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/schema")
    @Operation(summary = "Fetch Neo4j Aura graph schema metadata (node labels, relationship types, constraints)")
    public ResponseEntity<ApiResponse<SchemaMetadataDto>> getSchemaMetadata() {
        SchemaMetadataDto schema = cypherService.getSchemaMetadata();
        return ResponseEntity.ok(ApiResponse.ok(schema));
    }

    @GetMapping("/connection-info")
    @Operation(summary = "Fetch Neo4j Aura database connection details without secrets")
    public ResponseEntity<ApiResponse<DatabaseConnectionInfoDto>> getConnectionInfo() {
        DatabaseConnectionInfoDto info = cypherService.getConnectionInfo();
        return ResponseEntity.ok(ApiResponse.ok(info));
    }

    @GetMapping("/history")
    @Operation(summary = "Fetch execution audit history of Cypher queries executed against Aura")
    public ResponseEntity<ApiResponse<List<SqlExecutionAuditDto>>> getExecutionHistory(
            @RequestParam(name = "limit", defaultValue = "50") int limit) {
        List<SqlExecutionAuditDto> history = cypherService.getExecutionHistory(limit);
        return ResponseEntity.ok(ApiResponse.ok(history));
    }
}
