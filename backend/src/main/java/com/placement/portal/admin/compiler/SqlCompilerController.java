package com.placement.portal.admin.compiler;

import com.placement.portal.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/sql")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin SQL Compiler", description = "Dedicated Oracle SQL/PLSQL Compiler environment for authorized administrators")
public class SqlCompilerController {

    private final SqlCompilerService sqlCompilerService;

    public SqlCompilerController(SqlCompilerService sqlCompilerService) {
        this.sqlCompilerService = sqlCompilerService;
    }

    @PostMapping("/execute")
    @Operation(summary = "Execute real SQL or PL/SQL statement/script against Oracle database")
    public ResponseEntity<ApiResponse<SqlExecuteResponseDto>> executeSql(@RequestBody SqlExecuteRequestDto request) {
        SqlExecuteResponseDto response = sqlCompilerService.execute(request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/schema")
    @Operation(summary = "Fetch real Oracle dictionary schema metadata (tables, views, procedures, functions, triggers)")
    public ResponseEntity<ApiResponse<SchemaMetadataDto>> getSchemaMetadata() {
        SchemaMetadataDto schema = sqlCompilerService.getSchemaMetadata();
        return ResponseEntity.ok(ApiResponse.ok(schema));
    }

    @GetMapping("/connection-info")
    @Operation(summary = "Fetch Oracle database connection and version metadata without credentials")
    public ResponseEntity<ApiResponse<DatabaseConnectionInfoDto>> getConnectionInfo() {
        DatabaseConnectionInfoDto info = sqlCompilerService.getConnectionInfo();
        return ResponseEntity.ok(ApiResponse.ok(info));
    }
}
