package com.placement.portal.admin;

import java.util.Map;

/**
 * Data transfer object representing the execution result and entity breakdown
 * of the demonstration mock data seeding engine.
 */
public record DemoDataSummaryDto(
        boolean success,
        boolean dryRun,
        String message,
        int studentsTargeted,
        int studentsInserted,
        int studentsSkipped,
        int companiesTargeted,
        int companiesInserted,
        int companiesSkipped,
        int drivesTargeted,
        int drivesInserted,
        int drivesSkipped,
        int applicationsTargeted,
        int applicationsInserted,
        int applicationsSkipped,
        int interviewsTargeted,
        int interviewsInserted,
        int interviewsSkipped,
        int offersTargeted,
        int offersInserted,
        int offersSkipped,
        long executionTimeMs,
        Map<String, Object> databaseCounts
) {}
