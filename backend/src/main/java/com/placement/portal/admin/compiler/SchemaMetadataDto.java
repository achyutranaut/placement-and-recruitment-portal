package com.placement.portal.admin.compiler;

import java.util.List;

public class SchemaMetadataDto {
    private List<TableMetadata> tables;
    private List<ViewMetadata> views;
    private List<ProcedureMetadata> procedures;
    private List<FunctionMetadata> functions;
    private List<TriggerMetadata> triggers;

    public SchemaMetadataDto() {}

    public SchemaMetadataDto(
            List<TableMetadata> tables,
            List<ViewMetadata> views,
            List<ProcedureMetadata> procedures,
            List<FunctionMetadata> functions,
            List<TriggerMetadata> triggers
    ) {
        this.tables = tables;
        this.views = views;
        this.procedures = procedures;
        this.functions = functions;
        this.triggers = triggers;
    }

    public List<TableMetadata> getTables() { return tables; }
    public void setTables(List<TableMetadata> tables) { this.tables = tables; }

    public List<ViewMetadata> getViews() { return views; }
    public void setViews(List<ViewMetadata> views) { this.views = views; }

    public List<ProcedureMetadata> getProcedures() { return procedures; }
    public void setProcedures(List<ProcedureMetadata> procedures) { this.procedures = procedures; }

    public List<FunctionMetadata> getFunctions() { return functions; }
    public void setFunctions(List<FunctionMetadata> functions) { this.functions = functions; }

    public List<TriggerMetadata> getTriggers() { return triggers; }
    public void setTriggers(List<TriggerMetadata> triggers) { this.triggers = triggers; }

    public static class TableMetadata {
        private String tableName;
        private Long rowCount;
        private List<ColumnMetadata> columns;

        public TableMetadata() {}

        public TableMetadata(String tableName, Long rowCount, List<ColumnMetadata> columns) {
            this.tableName = tableName;
            this.rowCount = rowCount;
            this.columns = columns;
        }

        public String getTableName() { return tableName; }
        public void setTableName(String tableName) { this.tableName = tableName; }

        public Long getRowCount() { return rowCount; }
        public void setRowCount(Long rowCount) { this.rowCount = rowCount; }

        public List<ColumnMetadata> getColumns() { return columns; }
        public void setColumns(List<ColumnMetadata> columns) { this.columns = columns; }
    }

    public static class ColumnMetadata {
        private String columnName;
        private String dataType;
        private Integer dataLength;
        private boolean nullable;
        private boolean isPrimaryKey;
        private String foreignKeyRef;

        public ColumnMetadata() {}

        public ColumnMetadata(String columnName, String dataType, Integer dataLength, boolean nullable, boolean isPrimaryKey, String foreignKeyRef) {
            this.columnName = columnName;
            this.dataType = dataType;
            this.dataLength = dataLength;
            this.nullable = nullable;
            this.isPrimaryKey = isPrimaryKey;
            this.foreignKeyRef = foreignKeyRef;
        }

        public String getColumnName() { return columnName; }
        public void setColumnName(String columnName) { this.columnName = columnName; }

        public String getDataType() { return dataType; }
        public void setDataType(String dataType) { this.dataType = dataType; }

        public Integer getDataLength() { return dataLength; }
        public void setDataLength(Integer dataLength) { this.dataLength = dataLength; }

        public boolean isNullable() { return nullable; }
        public void setNullable(boolean nullable) { this.nullable = nullable; }

        public boolean isPrimaryKey() { return isPrimaryKey; }
        public void setPrimaryKey(boolean isPrimaryKey) { this.isPrimaryKey = isPrimaryKey; }

        public String getForeignKeyRef() { return foreignKeyRef; }
        public void setForeignKeyRef(String foreignKeyRef) { this.foreignKeyRef = foreignKeyRef; }
    }

    public static class ViewMetadata {
        private String viewName;

        public ViewMetadata() {}
        public ViewMetadata(String viewName) { this.viewName = viewName; }

        public String getViewName() { return viewName; }
        public void setViewName(String viewName) { this.viewName = viewName; }
    }

    public static class ProcedureMetadata {
        private String procedureName;
        private String status;

        public ProcedureMetadata() {}
        public ProcedureMetadata(String procedureName, String status) {
            this.procedureName = procedureName;
            this.status = status;
        }

        public String getProcedureName() { return procedureName; }
        public void setProcedureName(String procedureName) { this.procedureName = procedureName; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    public static class FunctionMetadata {
        private String functionName;
        private String status;

        public FunctionMetadata() {}
        public FunctionMetadata(String functionName, String status) {
            this.functionName = functionName;
            this.status = status;
        }

        public String getFunctionName() { return functionName; }
        public void setFunctionName(String functionName) { this.functionName = functionName; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    public static class TriggerMetadata {
        private String triggerName;
        private String tableName;
        private String triggeringEvent;
        private String status;

        public TriggerMetadata() {}
        public TriggerMetadata(String triggerName, String tableName, String triggeringEvent, String status) {
            this.triggerName = triggerName;
            this.tableName = tableName;
            this.triggeringEvent = triggeringEvent;
            this.status = status;
        }

        public String getTriggerName() { return triggerName; }
        public void setTriggerName(String triggerName) { this.triggerName = triggerName; }

        public String getTableName() { return tableName; }
        public void setTableName(String tableName) { this.tableName = tableName; }

        public String getTriggeringEvent() { return triggeringEvent; }
        public void setTriggeringEvent(String triggeringEvent) { this.triggeringEvent = triggeringEvent; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
}
