package edu.pict.mcpservice.stratagies.blocking;

import static org.junit.jupiter.api.Assertions.*;

import edu.pict.mcpservice.kafkaEvents.LogEvent;
import edu.pict.mcpservice.kafkaEvents.SecurityAlertEvent;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BurstTrafficStrategyTest {

    private BurstTrafficStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new BurstTrafficStrategy();
    }

    @Test
    void isAvailable_ShouldReturnTrue_WhenBurstDetected() {
        List<LogEvent> history = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (int i = 0; i < 20; i++) {
            history.add(
                    LogEvent.builder()
                            .timestamp(now + i * 100)
                            .build()); // 20 requests in 2 seconds
        }

        SecurityAlertEvent alert = SecurityAlertEvent.builder().build();
        assertTrue(strategy.isAvailable(alert, history));
    }

    @Test
    void isAvailable_ShouldReturnFalse_WhenInsufficientRequests() {
        List<LogEvent> history = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (int i = 0; i < 9; i++) {
            history.add(LogEvent.builder().timestamp(now + i * 100).build());
        }

        SecurityAlertEvent alert = SecurityAlertEvent.builder().build();
        assertFalse(strategy.isAvailable(alert, history));
    }

    @Test
    void isAvailable_ShouldReturnFalse_WhenTrafficIsSlow() {
        List<LogEvent> history = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (int i = 0; i < 20; i++) {
            history.add(
                    LogEvent.builder()
                            .timestamp(now + i * 1000)
                            .build()); // 20 requests in 19 seconds
        }

        SecurityAlertEvent alert = SecurityAlertEvent.builder().build();
        assertFalse(strategy.isAvailable(alert, history));
    }

    @Test
    void getBlockDuration_ShouldReturnThirtyMinutes() {
        assertEquals(Duration.ofMinutes(30), strategy.getBlockDuration());
    }

    @Test
    void getReason_ShouldReturnCorrectReason() {
        assertEquals("BURST_TRAFFIC_DETECTED_BOT_SUSPECT", strategy.getReason());
    }
}
