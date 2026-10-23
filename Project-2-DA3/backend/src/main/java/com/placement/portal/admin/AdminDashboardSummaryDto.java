package com.placement.portal.admin;

import com.placement.portal.reports.PlacementStatsDto;

public class AdminDashboardSummaryDto {
    private PlacementStatsDto stats;
    private long totalPrograms;
    private long totalBatches;

    public AdminDashboardSummaryDto() {}

    public AdminDashboardSummaryDto(PlacementStatsDto stats, long totalPrograms, long totalBatches) {
        this.stats = stats;
        this.totalPrograms = totalPrograms;
        this.totalBatches = totalBatches;
    }

    public PlacementStatsDto getStats() {
        return stats;
    }

    public void setStats(PlacementStatsDto stats) {
        this.stats = stats;
    }

    public long getTotalPrograms() {
        return totalPrograms;
    }

    public void setTotalPrograms(long totalPrograms) {
        this.totalPrograms = totalPrograms;
    }

    public long getTotalBatches() {
        return totalBatches;
    }

    public void setTotalBatches(long totalBatches) {
        this.totalBatches = totalBatches;
    }
}
