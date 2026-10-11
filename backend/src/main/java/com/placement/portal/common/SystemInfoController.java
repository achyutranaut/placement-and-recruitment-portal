package com.placement.portal.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/system")
public class SystemInfoController {

    private final Environment environment;

    public SystemInfoController(Environment environment) {
        this.environment = environment;
    }

    @GetMapping("/backend-info")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getBackendInfo() {
        String[] activeProfiles = environment.getActiveProfiles();
        boolean isNeo4j = Arrays.asList(activeProfiles).contains("neo4j");

        Map<String, Object> info = new HashMap<>();
        if (isNeo4j) {
            info.put("activeBackend", "neo4j");
            info.put("databaseEngine", "NEO4J");
            info.put("databaseProduct", "Neo4j Graph Database 5.x");
            info.put("supportsGraph", true);
            info.put("supportsCypher", true);
            info.put("supportsSql", false);
        } else {
            info.put("activeBackend", "oracle");
            info.put("databaseEngine", "ORACLE");
            info.put("databaseProduct", "Oracle 23ai Free");
            info.put("supportsGraph", false);
            info.put("supportsCypher", false);
            info.put("supportsSql", true);
        }
        info.put("activeProfiles", activeProfiles);
        return ResponseEntity.ok(ApiResponse.ok(info));
    }
}
