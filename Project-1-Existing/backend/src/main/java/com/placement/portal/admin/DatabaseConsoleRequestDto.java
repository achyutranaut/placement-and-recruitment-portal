package com.placement.portal.admin;

public class DatabaseConsoleRequestDto {
    private String queryKey;
    private String customSql;

    public DatabaseConsoleRequestDto() {}

    public DatabaseConsoleRequestDto(String queryKey, String customSql) {
        this.queryKey = queryKey;
        this.customSql = customSql;
    }

    public String getQueryKey() {
        return queryKey;
    }

    public void setQueryKey(String queryKey) {
        this.queryKey = queryKey;
    }

    public String getCustomSql() {
        return customSql;
    }

    public void setCustomSql(String customSql) {
        this.customSql = customSql;
    }
}
