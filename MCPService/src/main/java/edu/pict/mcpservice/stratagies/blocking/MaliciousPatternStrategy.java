package edu.pict.mcpservice.stratagies.blocking;

import edu.pict.mcpservice.kafkaEvents.LogEvent;
import edu.pict.mcpservice.kafkaEvents.SecurityAlertEvent;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Component
public class MaliciousPatternStrategy implements BlockingStrategy {
    @Override
    public boolean isAvailable(SecurityAlertEvent alert, List<LogEvent> history) {
        return history.stream().anyMatch(log -> log.getPath().contains(".."));
    }

    @Override
    public Duration getBlockDuration() {
        return Duration.ofDays(1);
    }

    @Override
    public String getReason() {
        return "Malicious injection attempt. 24h ban.";
    }
}
