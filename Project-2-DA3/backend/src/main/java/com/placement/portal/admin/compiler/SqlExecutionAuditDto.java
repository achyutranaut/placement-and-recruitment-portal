package com.placement.portal.admin.compiler;

import java.time.LocalDateTime;

public class SqlExecutionAuditDto {
    private Long auditId;
    private String adminId;
    private LocalDateTime executedAt;
    private String statementType;
    private String sqlText;
    private String status;
    private Integer affectedRows;
    private Integer returnedRows;
    private Long executionTimeMs;
    private String errorCode;
    private String errorMessage;

    public SqlExecutionAuditDto() {}

    public SqlExecutionAuditDto(
            Long auditId,
            String adminId,
            LocalDateTime executedAt,
            String statementType,
            String sqlText,
            String status,
            Integer affectedRows,
            Integer returnedRows,
            Long executionTimeMs,
            String errorCode,
            String errorMessage
    ) {
        this.auditId = auditId;
        this.adminId = adminId;
        this.executedAt = executedAt;
        this.statementType = statementType;
        this.sqlText = sqlText;
        this.status = status;
        this.affectedRows = affectedRows;
        this.returnedRows = returnedRows;
        this.executionTimeMs = executionTimeMs;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public Long getAuditId() { return auditId; }
    public void setAuditId(Long auditId) { this.auditId = auditId; }

    public String getAdminId() { return adminId; }
    public void setAdminId(String adminId) { this.adminId = adminId; }

    public LocalDateTime getExecutedAt() { return executedAt; }
    public void setExecutedAt(LocalDateTime executedAt) { this.executedAt = executedAt; }

    public String getStatementType() { return statementType; }
    public void setStatementType(String statementType) { this.statementType = statementType; }

    public String getSqlText() { return sqlText; }
    public void setSqlText(String sqlText) { this.sqlText = sqlText; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getAffectedRows() { return affectedRows; }
    public void setAffectedRows(Integer affectedRows) { this.affectedRows = affectedRows; }

    public Integer getReturnedRows() { return returnedRows; }
    public void setReturnedRows(Integer returnedRows) { this.returnedRows = returnedRows; }

    public Long getExecutionTimeMs() { return executionTimeMs; }
    public void setExecutionTimeMs(Long executionTimeMs) { this.executionTimeMs = executionTimeMs; }

    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
