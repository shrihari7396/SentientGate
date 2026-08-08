package edu.pict.mcpservice.stratagies.blocking;

import edu.pict.mcpservice.kafkaEvents.LogEvent;
import edu.pict.mcpservice.kafkaEvents.SecurityAlertEvent;
import java.time.Duration;
import java.util.List;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(3)
public class RateLimitCoolDownStrategy implements ThreatStrategy {

    private static final int RATE_LIMIT_HISTORY_THRESHOLD = 3;

    @Override
    public boolean isAvailable(SecurityAlertEvent alert, List<LogEvent> history) {
        // Current alert is a 429 — immediate trigger
        if (alert.getErrorCode() == 429) {
            return true;
        }

        // Check history for repeated rate-limit violations
        long rateLimitCount =
                history.stream().filter(log -> log.getStatusCode() == 429).count();
        return rateLimitCount >= RATE_LIMIT_HISTORY_THRESHOLD;
    }

    @Override
    public Duration getBlockDuration() {
        return Duration.ofMinutes(15);
    }

    @Override
    public String getReason() {
        return "Aggressive polling detected. 15m cool-down.";
    }
}
