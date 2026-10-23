package com.placement.portal.admin.compiler;

public class SqlExecuteRequestDto {
    private String sql;
    private String mode; // "QUERY" or "SCRIPT"
    private Boolean strictMode = true; // Strict Data Integrity Mode (Default: true)

    public SqlExecuteRequestDto() {}

    public SqlExecuteRequestDto(String sql, String mode) {
        this.sql = sql;
        this.mode = mode;
        this.strictMode = true;
    }

    public SqlExecuteRequestDto(String sql, String mode, Boolean strictMode) {
        this.sql = sql;
        this.mode = mode;
        this.strictMode = strictMode != null ? strictMode : true;
    }

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    public String getMode() {
        return mode != null ? mode.toUpperCase() : "QUERY";
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public boolean isStrictMode() {
        return strictMode == null || strictMode;
    }

    public void setStrictMode(Boolean strictMode) {
        this.strictMode = strictMode;
    }
}
