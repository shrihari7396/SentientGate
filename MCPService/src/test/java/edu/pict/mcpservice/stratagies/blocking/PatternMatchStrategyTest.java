package edu.pict.mcpservice.stratagies.blocking;

import static org.junit.jupiter.api.Assertions.*;

import edu.pict.mcpservice.kafkaEvents.SecurityAlertEvent;
import java.time.Duration;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PatternMatchStrategyTest {

    private PatternMatchStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new PatternMatchStrategy();
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "/api/v1/users?id=1' or 1=1",
                "/api/v1/products/../../etc/passwd",
                "/search?q=<script>alert(1)</script>",
                "/login?user=admin'--",
                "/data?query=select * from users",
                "/admin/drop table patients"
            })
    void isAvailable_ShouldReturnTrue_ForMaliciousPatterns(String path) {
        SecurityAlertEvent alert = SecurityAlertEvent.builder().attemptedPath(path).build();

        assertTrue(strategy.isAvailable(alert, Collections.emptyList()));
    }

    @Test
    void isAvailable_ShouldReturnFalse_ForSafePath() {
        SecurityAlertEvent alert =
                SecurityAlertEvent.builder().attemptedPath("/api/v1/health").build();

        assertFalse(strategy.isAvailable(alert, Collections.emptyList()));
    }

    @Test
    void getBlockDuration_ShouldReturnOneDay() {
        assertEquals(Duration.ofDays(1), strategy.getBlockDuration());
    }

    @Test
    void getReason_ShouldReturnCorrectReason() {
        assertEquals("CRITICAL_INJECTION_ATTEMPT", strategy.getReason());
    }
}
