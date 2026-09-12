package com.placement.portal.application;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("h2")
@Transactional
class ApplicationServiceTests {

    @Autowired
    private ApplicationService applicationService;

    @Test
    void testApplyForDriveAndFetch() {
        LocalDate date = LocalDate.of(2026, 1, 10);
        ApplicationDto app = applicationService.applyForDrive("STU001", "DRV001", date);

        assertNotNull(app);
        assertNotNull(app.getApplicationId());
        assertEquals("STU001", app.getStudentId());
        assertEquals("APPLIED", app.getStatus());

        List<ApplicationDto> studentApps = applicationService.getApplicationsByStudent("STU001");
        assertFalse(studentApps.isEmpty());
    }

    @Test
    void testDailyThrottlingConflict() {
        LocalDate date = LocalDate.of(2026, 1, 15);
        applicationService.applyForDrive("STU002", "DRV002", date);

        Exception ex = assertThrows(Exception.class, () ->
                applicationService.applyForDrive("STU002", "DRV003", date));
        assertTrue(ex.getMessage().contains("Daily limit reached") || 
                   (ex.getCause() != null && ex.getCause().getMessage().contains("Daily limit reached")));
    }
}
