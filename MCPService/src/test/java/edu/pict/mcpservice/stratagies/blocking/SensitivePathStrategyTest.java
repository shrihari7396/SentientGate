package edu.pict.mcpservice.stratagies.blocking;

import edu.pict.mcpservice.kafkaEvents.SecurityAlertEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class SensitivePathStrategyTest {

    private SensitivePathStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new SensitivePathStrategy();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/wp-admin/index.php",
            "/.env",
            "/config.php",
            "/admin/login",
            "/.git/config",
            "/actuator/health"
    })
    void isAvailable_ShouldReturnTrue_ForSensitivePaths(String path) {
        SecurityAlertEvent alert = SecurityAlertEvent.builder()
                .attemptedPath(path)
                .build();

        assertTrue(strategy.isAvailable(alert, Collections.emptyList()));
    }

    @Test
    void isAvailable_ShouldReturnFalse_ForNormalPath() {
        SecurityAlertEvent alert = SecurityAlertEvent.builder()
                .attemptedPath("/api/v1/products")
                .build();

        assertFalse(strategy.isAvailable(alert, Collections.emptyList()));
    }

    @Test
    void getBlockDuration_ShouldReturnSevenDays() {
        assertEquals(Duration.ofDays(7), strategy.getBlockDuration());
    }

    @Test
    void getReason_ShouldReturnCorrectReason() {
        assertEquals("SENSITIVE_PATH_RECONNAISSANCE", strategy.getReason());
    }
}
