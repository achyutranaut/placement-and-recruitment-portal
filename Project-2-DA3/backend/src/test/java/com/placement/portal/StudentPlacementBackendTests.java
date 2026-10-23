package com.placement.portal;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("h2")
class StudentPlacementBackendTests {

    @Test
    void contextLoads() {
        // Verifies Spring ApplicationContext starts and all beans/repositories initialize cleanly
    }
}
