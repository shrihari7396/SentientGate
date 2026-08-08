package edu.pict.mcpservice.stratagies.blocking;

import static org.junit.jupiter.api.Assertions.*;

import edu.pict.mcpservice.kafkaEvents.LogEvent;
import edu.pict.mcpservice.kafkaEvents.SecurityAlertEvent;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RateLimitCoolDownStrategyTest {

    private final RateLimitCoolDownStrategy strategy = new RateLimitCoolDownStrategy();
    private final List<LogEvent> emptyHistory = Collections.emptyList();

    private SecurityAlertEvent alertWithErrorCode(int errorCode) {
        return SecurityAlertEvent.builder()
                .attemptedPath("/api/resource")
                .errorCode(errorCode)
                .build();
    }

    // ═══════════════════════════════════════════════════════════════════
    // Contract tests
    // ═══════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("implements ThreatStrategy interface")
    void implementsThreatStrategy() {
        assertInstanceOf(ThreatStrategy.class, strategy);
    }

    @Test
    @DisplayName("block duration is 15 minutes")
    void blockDurationIs15Minutes() {
        assertEquals(Duration.ofMinutes(15), strategy.getBlockDuration());
    }

    @Test
    @DisplayName("reason matches expected string")
    void reasonString() {
        assertEquals("Aggressive polling detected. 15m cool-down.", strategy.getReason());
    }

    // ═══════════════════════════════════════════════════════════════════
    // 429 triggers
    // ═══════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("triggers on 429 error code")
    void triggersOn429() {
        assertTrue(strategy.isAvailable(alertWithErrorCode(429), emptyHistory));
    }

    // ═══════════════════════════════════════════════════════════════════
    // Non-429 codes should NOT trigger
    // ═══════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("does NOT trigger on 200")
    void doesNotTriggerOn200() {
        assertFalse(strategy.isAvailable(alertWithErrorCode(200), emptyHistory));
    }

    @Test
    @DisplayName("does NOT trigger on 400")
    void doesNotTriggerOn400() {
        assertFalse(strategy.isAvailable(alertWithErrorCode(400), emptyHistory));
    }

    @Test
    @DisplayName("does NOT trigger on 403")
    void doesNotTriggerOn403() {
        assertFalse(strategy.isAvailable(alertWithErrorCode(403), emptyHistory));
    }

    @Test
    @DisplayName("does NOT trigger on 500")
    void doesNotTriggerOn500() {
        assertFalse(strategy.isAvailable(alertWithErrorCode(500), emptyHistory));
    }

    @Test
    @DisplayName("does NOT trigger on 0 (default)")
    void doesNotTriggerOnZero() {
        assertFalse(strategy.isAvailable(alertWithErrorCode(0), emptyHistory));
    }

    // ═══════════════════════════════════════════════════════════════════
    // History is ignored
    // ═══════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("history does not affect the decision")
    void historyIgnored() {
        LogEvent log = LogEvent.builder().statusCode(200).timestamp(1000L).build();
        assertTrue(strategy.isAvailable(alertWithErrorCode(429), List.of(log)));
        assertFalse(strategy.isAvailable(alertWithErrorCode(200), List.of(log)));
    }
}
