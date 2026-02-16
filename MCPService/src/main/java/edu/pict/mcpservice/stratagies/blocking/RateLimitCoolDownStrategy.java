package edu.pict.mcpservice.stratagies.blocking;

import edu.pict.mcpservice.kafkaEvents.LogEvent;
import edu.pict.mcpservice.kafkaEvents.SecurityAlertEvent;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Component
public class RateLimitCoolDownStrategy implements BlockingStrategy {

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
