package com.placement.portal.vector;

import com.placement.portal.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/semantic-search")
@Tag(name = "Semantic Vector Search", description = "Qdrant Vector Database endpoints for semantic recruitment search and candidate discovery")
public class SemanticSearchController {

    private final SemanticSearchService semanticSearchService;
    private final QdrantClientService qdrantClientService;
    private final EmbeddingService embeddingService;

    @Autowired
    public SemanticSearchController(
            SemanticSearchService semanticSearchService,
            QdrantClientService qdrantClientService,
            EmbeddingService embeddingService
    ) {
        this.semanticSearchService = semanticSearchService;
        this.qdrantClientService = qdrantClientService;
        this.embeddingService = embeddingService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('STUDENT', 'RECRUITER', 'ADMIN')")
    @Operation(summary = "Execute semantic vector search against candidate profiles, job descriptions, and skills")
    public ResponseEntity<ApiResponse<SemanticSearchResponse>> executeSearch(
            @Valid @RequestBody SemanticSearchRequest request,
            Authentication authentication
    ) {
        String role = resolvePrimaryRole(authentication);
        SemanticSearchResponse response = semanticSearchService.search(request, role);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/status")
    @PreAuthorize("hasAnyRole('STUDENT', 'RECRUITER', 'ADMIN')")
    @Operation(summary = "Get Qdrant Vector Database connection status, collection metadata, and embedding configuration")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStatus() {
        Map<String, Object> status = new LinkedHashMap<>(qdrantClientService.getStatus());
        status.put("embeddingProvider", embeddingService.getProvider());
        status.put("embeddingModel", embeddingService.getModelName());
        status.put("embeddingDimension", embeddingService.getDimension());
        return ResponseEntity.ok(ApiResponse.ok(status));
    }

    @PostMapping("/index")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Synchronize Oracle source-of-truth records into Qdrant Vector Database collection (Admin only)")
    public ResponseEntity<ApiResponse<IndexSummaryDto>> reindexAll() {
        IndexSummaryDto summary = semanticSearchService.reindexAll();
        return ResponseEntity.ok(ApiResponse.ok(summary));
    }

    @PostMapping("/reconnect")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Re-verify connectivity to Qdrant Vector Database instance (Admin only)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> reconnect() {
        boolean connected = qdrantClientService.reconnect();
        Map<String, Object> result = new LinkedHashMap<>(qdrantClientService.getStatus());
        result.put("reconnectSuccessful", connected);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PostMapping("/sync-drive/{driveId}")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    @Operation(summary = "Synchronize a specific placement drive into Qdrant collection")
    public ResponseEntity<ApiResponse<Map<String, Object>>> syncDrive(@PathVariable String driveId) {
        boolean success = semanticSearchService.indexDrive(driveId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("driveId", driveId);
        result.put("synced", success);
        result.put("message", success ? "Drive indexed into Qdrant successfully." : "Failed to index drive.");
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    private String resolvePrimaryRole(Authentication auth) {
        if (auth == null) return "ROLE_ANONYMOUS";
        for (GrantedAuthority ga : auth.getAuthorities()) {
            if ("ROLE_ADMIN".equals(ga.getAuthority())) return "ROLE_ADMIN";
            if ("ROLE_RECRUITER".equals(ga.getAuthority())) return "ROLE_RECRUITER";
            if ("ROLE_STUDENT".equals(ga.getAuthority())) return "ROLE_STUDENT";
        }
        return "ROLE_STUDENT";
    }
}
