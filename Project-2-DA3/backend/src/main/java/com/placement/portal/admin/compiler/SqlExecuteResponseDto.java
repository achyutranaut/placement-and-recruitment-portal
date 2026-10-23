package com.placement.portal.admin.compiler;

import java.util.List;

public class SqlExecuteResponseDto {
    private boolean success;
    private String statementType;
    private List<String> columns;
    private List<List<Object>> rows;
    private Integer rowCount;
    private Integer rowsAffected;
    private String message;
    private String dbmsOutput;
    private long executionTimeMs;
    private String errorCode;
    private String errorMessage;
    private String fullError;
    private Integer errorLineNumber;
    private String sql;
    private List<SqlExecuteResponseDto> scriptResults;
    private String transactionState = "NONE"; // COMMITTED, ROLLED_BACK, NONE
    private boolean validationPassed = true;
    private String validationMessage;

    public SqlExecuteResponseDto() {}

    public static SqlExecuteResponseDto selectResult(
            List<String> columns,
            List<List<Object>> rows,
            int rowCount,
            String dbmsOutput,
            long executionTimeMs
    ) {
        SqlExecuteResponseDto dto = new SqlExecuteResponseDto();
        dto.setSuccess(true);
        dto.setStatementType("SELECT");
        dto.setColumns(columns);
        dto.setRows(rows);
        dto.setRowCount(rowCount);
        dto.setDbmsOutput(dbmsOutput);
        dto.setExecutionTimeMs(executionTimeMs);
        dto.setMessage("Query executed successfully. Returned " + rowCount + " row(s).");
        return dto;
    }

    public static SqlExecuteResponseDto dmlResult(
            String statementType,
            int rowsAffected,
            String dbmsOutput,
            long executionTimeMs
    ) {
        SqlExecuteResponseDto dto = new SqlExecuteResponseDto();
        dto.setSuccess(true);
        dto.setStatementType(statementType);
        dto.setRowsAffected(rowsAffected);
        dto.setDbmsOutput(dbmsOutput);
        dto.setExecutionTimeMs(executionTimeMs);
        dto.setMessage("Statement executed successfully. Rows affected: " + rowsAffected);
        return dto;
    }

    public static SqlExecuteResponseDto ddlResult(
            String statementType,
            String message,
            String dbmsOutput,
            long executionTimeMs
    ) {
        SqlExecuteResponseDto dto = new SqlExecuteResponseDto();
        dto.setSuccess(true);
        dto.setStatementType(statementType != null ? statementType : "DDL");
        dto.setMessage(message != null ? message : "Command executed successfully.");
        dto.setDbmsOutput(dbmsOutput);
        dto.setExecutionTimeMs(executionTimeMs);
        return dto;
    }

    public static SqlExecuteResponseDto plsqlResult(
            String dbmsOutput,
            long executionTimeMs
    ) {
        SqlExecuteResponseDto dto = new SqlExecuteResponseDto();
        dto.setSuccess(true);
        dto.setStatementType("PLSQL");
        dto.setMessage("PL/SQL procedure successfully completed.");
        dto.setDbmsOutput(dbmsOutput);
        dto.setExecutionTimeMs(executionTimeMs);
        return dto;
    }

    public static SqlExecuteResponseDto tclResult(
            String statementType,
            String message,
            long executionTimeMs
    ) {
        SqlExecuteResponseDto dto = new SqlExecuteResponseDto();
        dto.setSuccess(true);
        dto.setStatementType(statementType != null ? statementType : "TCL");
        dto.setMessage(message != null ? message : "Transaction executed successfully.");
        dto.setExecutionTimeMs(executionTimeMs);
        return dto;
    }

    public static SqlExecuteResponseDto scriptResult(
            List<SqlExecuteResponseDto> results,
            long totalExecutionTimeMs,
            String combinedDbmsOutput
    ) {
        SqlExecuteResponseDto dto = new SqlExecuteResponseDto();
        boolean allSuccess = results.stream().allMatch(SqlExecuteResponseDto::isSuccess);
        dto.setSuccess(allSuccess);
        dto.setStatementType("SCRIPT");
        dto.setScriptResults(results);
        dto.setExecutionTimeMs(totalExecutionTimeMs);
        dto.setDbmsOutput(combinedDbmsOutput);
        dto.setMessage("Script execution completed: " + results.size() + " statement(s) executed.");
        return dto;
    }

    public static SqlExecuteResponseDto errorResult(
            String errorCode,
            String errorMessage,
            String fullError,
            Integer errorLineNumber,
            String dbmsOutput,
            long executionTimeMs
    ) {
        SqlExecuteResponseDto dto = new SqlExecuteResponseDto();
        dto.setSuccess(false);
        dto.setStatementType("ERROR");
        dto.setErrorCode(errorCode);
        dto.setErrorMessage(errorMessage);
        dto.setFullError(fullError);
        dto.setErrorLineNumber(errorLineNumber);
        dto.setDbmsOutput(dbmsOutput);
        dto.setExecutionTimeMs(executionTimeMs);
        dto.setMessage(fullError != null ? fullError : errorMessage);
        dto.setTransactionState("ROLLED_BACK");
        return dto;
    }

    public static SqlExecuteResponseDto blockedResult(String message, String validationDetail, String sql) {
        SqlExecuteResponseDto dto = new SqlExecuteResponseDto();
        dto.setSuccess(false);
        dto.setStatementType("BLOCKED");
        dto.setErrorCode("ORA-VALIDATION-BLOCKED");
        dto.setErrorMessage(message);
        dto.setFullError(message);
        dto.setMessage(message);
        dto.setValidationPassed(false);
        dto.setValidationMessage(validationDetail);
        dto.setTransactionState("NONE");
        dto.setSql(sql);
        dto.setExecutionTimeMs(0);
        return dto;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getStatementType() { return statementType; }
    public void setStatementType(String statementType) { this.statementType = statementType; }

    public List<String> getColumns() { return columns; }
    public void setColumns(List<String> columns) { this.columns = columns; }

    public List<List<Object>> getRows() { return rows; }
    public void setRows(List<List<Object>> rows) { this.rows = rows; }

    public Integer getRowCount() { return rowCount; }
    public void setRowCount(Integer rowCount) { this.rowCount = rowCount; }

    public Integer getRowsAffected() { return rowsAffected; }
    public void setRowsAffected(Integer rowsAffected) { this.rowsAffected = rowsAffected; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getDbmsOutput() { return dbmsOutput; }
    public void setDbmsOutput(String dbmsOutput) { this.dbmsOutput = dbmsOutput; }

    public long getExecutionTimeMs() { return executionTimeMs; }
    public void setExecutionTimeMs(long executionTimeMs) { this.executionTimeMs = executionTimeMs; }

    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public String getFullError() { return fullError; }
    public void setFullError(String fullError) { this.fullError = fullError; }

    public Integer getErrorLineNumber() { return errorLineNumber; }
    public void setErrorLineNumber(Integer errorLineNumber) { this.errorLineNumber = errorLineNumber; }

    public String getSql() { return sql; }
    public void setSql(String sql) { this.sql = sql; }

    public List<SqlExecuteResponseDto> getScriptResults() { return scriptResults; }
    public void setScriptResults(List<SqlExecuteResponseDto> scriptResults) { this.scriptResults = scriptResults; }

    public String getTransactionState() { return transactionState; }
    public void setTransactionState(String transactionState) { this.transactionState = transactionState; }

    public boolean isValidationPassed() { return validationPassed; }
    public void setValidationPassed(boolean validationPassed) { this.validationPassed = validationPassed; }

    public String getValidationMessage() { return validationMessage; }
    public void setValidationMessage(String validationMessage) { this.validationMessage = validationMessage; }
}
