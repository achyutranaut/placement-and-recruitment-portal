package com.placement.portal.admin;

import java.util.List;
import java.util.Map;

public class DatabaseConsoleResponseDto {
    private String category;
    private String queryTitle;
    private String sql;
    private List<String> columns;
    private List<Map<String, Object>> rows;
    private int rowCount;
    private long executionTimeMs;
    private String status;
    private String notes;

    public DatabaseConsoleResponseDto() {}

    public DatabaseConsoleResponseDto(String category, String queryTitle, String sql, List<String> columns, List<Map<String, Object>> rows, int rowCount, long executionTimeMs, String status, String notes) {
        this.category = category;
        this.queryTitle = queryTitle;
        this.sql = sql;
        this.columns = columns;
        this.rows = rows;
        this.rowCount = rowCount;
        this.executionTimeMs = executionTimeMs;
        this.status = status;
        this.notes = notes;
    }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getQueryTitle() { return queryTitle; }
    public void setQueryTitle(String queryTitle) { this.queryTitle = queryTitle; }
    public String getSql() { return sql; }
    public void setSql(String sql) { this.sql = sql; }
    public List<String> getColumns() { return columns; }
    public void setColumns(List<String> columns) { this.columns = columns; }
    public List<Map<String, Object>> getRows() { return rows; }
    public void setRows(List<Map<String, Object>> rows) { this.rows = rows; }
    public int getRowCount() { return rowCount; }
    public void setRowCount(int rowCount) { this.rowCount = rowCount; }
    public long getExecutionTimeMs() { return executionTimeMs; }
    public void setExecutionTimeMs(long executionTimeMs) { this.executionTimeMs = executionTimeMs; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
