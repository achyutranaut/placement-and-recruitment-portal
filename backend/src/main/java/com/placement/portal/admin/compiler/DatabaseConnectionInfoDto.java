package com.placement.portal.admin.compiler;

public class DatabaseConnectionInfoDto {
    private String databaseProduct;
    private String databaseVersion;
    private String user;
    private String host;
    private int port;
    private String serviceName;
    private String status;
    private String driverVersion;

    public DatabaseConnectionInfoDto() {}

    public DatabaseConnectionInfoDto(String databaseProduct, String databaseVersion, String user, String host, int port, String serviceName, String status, String driverVersion) {
        this.databaseProduct = databaseProduct;
        this.databaseVersion = databaseVersion;
        this.user = user;
        this.host = host;
        this.port = port;
        this.serviceName = serviceName;
        this.status = status;
        this.driverVersion = driverVersion;
    }

    public String getDatabaseProduct() { return databaseProduct; }
    public void setDatabaseProduct(String databaseProduct) { this.databaseProduct = databaseProduct; }

    public String getDatabaseVersion() { return databaseVersion; }
    public void setDatabaseVersion(String databaseVersion) { this.databaseVersion = databaseVersion; }

    public String getUser() { return user; }
    public void setUser(String user) { this.user = user; }

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDriverVersion() { return driverVersion; }
    public void setDriverVersion(String driverVersion) { this.driverVersion = driverVersion; }
}
