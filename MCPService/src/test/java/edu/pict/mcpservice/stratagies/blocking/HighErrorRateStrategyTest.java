package edu.pict.mcpservice.stratagies.blocking;

import static org.junit.jupiter.api.Assertions.*;

import edu.pict.mcpservice.kafkaEvents.LogEvent;
import edu.pict.mcpservice.kafkaEvents.SecurityAlertEvent;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HighErrorRateStrategyTest {

    private HighErrorRateStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new HighErrorRateStrategy();
    }

    @Test
    void isAvailable_ShouldReturnTrue_WhenErrorRateHigh() {
        List<LogEvent> history = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            history.add(LogEvent.builder().statusCode(i < 8 ? 404 : 200).build()); // 80% error rate
        }

        SecurityAlertEvent alert = SecurityAlertEvent.builder().build();
        assertTrue(strategy.isAvailable(alert, history));
    }

    @Test
    void isAvailable_ShouldReturnFalse_WhenErrorRateLow() {
        List<LogEvent> history = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            history.add(LogEvent.builder().statusCode(i < 2 ? 404 : 200).build()); // 20% error rate
        }

        SecurityAlertEvent alert = SecurityAlertEvent.builder().build();
        assertFalse(strategy.isAvailable(alert, history));
    }

    @Test
    void isAvailable_ShouldReturnFalse_WhenHistoryTooSmall() {
        List<LogEvent> history = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            history.add(LogEvent.builder().statusCode(404).build());
        }

        SecurityAlertEvent alert = SecurityAlertEvent.builder().build();
        assertFalse(strategy.isAvailable(alert, history));
    }

    @Test
    void getBlockDuration_ShouldReturnTwoHours() {
        assertEquals(Duration.ofHours(2), strategy.getBlockDuration());
    }

    @Test
    void getReason_ShouldReturnCorrectReason() {
        assertEquals("HIGH_ERROR_RATE_SCANNER_DETECTED", strategy.getReason());
    }
}
