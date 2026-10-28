package com.placement.portal.admin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Startup hook for demonstration mock data.
 * Safe for production: Strictly disabled by default (PORTAL_DEMO_DATA_ENABLED=false).
 */
@Component
@Order(5)
public class DemoDataCommandLineRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataCommandLineRunner.class);

    private final DemoDataSeederService seederService;

    public DemoDataCommandLineRunner(DemoDataSeederService seederService) {
        this.seederService = seederService;
    }

    @Override
    public void run(String... args) {
        if (seederService.isDemoDataEnabled()) {
            log.info("PORTAL_DEMO_DATA_ENABLED=true detected. Commencing automated demonstration data seeding & graph synchronization...");
            DemoDataSummaryDto result = seederService.seedDemoData(false, true);
            log.info("Demonstration data startup result: success={}, message={}", result.success(), result.message());
        } else {
            log.info("Demonstration mock data auto-seeding is DISABLED (portal.demo-data.enabled=false). " +
                     "Database state preserved. Enable with PORTAL_DEMO_DATA_ENABLED=true or trigger via /api/v1/admin/demo-data/seed.");
        }
    }
}
