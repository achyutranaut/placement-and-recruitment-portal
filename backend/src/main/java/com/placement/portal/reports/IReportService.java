package com.placement.portal.reports;

import java.util.List;

public interface IReportService {
    PlacementStatsDto getPlacementSummary();
    OverviewReportDto getOverviewReport();
    List<CompanyHiringStatDto> getCompanyHiringStats();
}
