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

    @Override
    public boolean isAvailable(SecurityAlertEvent alert, List<LogEvent> history) {
        return alert.getErrorCode() == 429;
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
