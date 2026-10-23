package com.placement.portal.config;

import com.placement.portal.common.ApiResponse;
import com.placement.portal.neo4j.Neo4jDriverService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/system")
@Tag(name = "System Info", description = "System and dual database backend status")
public class SystemInfoController {

    private final Neo4jDriverService neo4jDriverService;

    public SystemInfoController(Neo4jDriverService neo4jDriverService) {
        this.neo4jDriverService = neo4jDriverService;
    }

    @GetMapping("/backend-info")
    @Operation(summary = "Get system and dual database backend status (Oracle transactional + Neo4j Aura)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getBackendInfo() {
        Map<String, Object> neo4jStatus = neo4jDriverService.getStatus();
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("activeBackend", "oracle");
        info.put("databaseEngine", "ORACLE");
        info.put("databaseProduct", "Oracle Database 23ai Free");
        info.put("oracleAuthoritative", true);
        info.put("supportsGraph", true);
        info.put("supportsCypher", true);
        info.put("supportsSql", true);
        info.put("neo4j", neo4jStatus);
        return ResponseEntity.ok(ApiResponse.ok(info));
    }
}
