package com.placement.portal.admin.compiler;

public class SqlExecuteRequestDto {
    private String sql;
    private String mode; // "QUERY" or "SCRIPT"

    public SqlExecuteRequestDto() {}

    public SqlExecuteRequestDto(String sql, String mode) {
        this.sql = sql;
        this.mode = mode;
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
}
