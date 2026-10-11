package com.placement.portal.admin.compiler;

import java.util.List;
import java.util.Map;

public interface ISqlCompilerService {
    SqlExecuteResponseDto execute(SqlExecuteRequestDto request);
    SchemaMetadataDto getSchemaMetadata();
    DatabaseConnectionInfoDto getConnectionInfo();
    List<SqlExecutionAuditDto> getExecutionHistory(int limit);
    Map<String, Object> getBackendInfo();
}
