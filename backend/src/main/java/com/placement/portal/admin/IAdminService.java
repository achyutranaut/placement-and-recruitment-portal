package com.placement.portal.admin;

import java.util.List;

public interface IAdminService {
    AdminDashboardSummaryDto getDashboardSummary();
    List<AuditLogDto> getAuditLogs();
    DatabaseConsoleResponseDto executeConsoleQuery(DatabaseConsoleRequestDto request);
}
