package edu.pict.mcpservice.stratagies.blocking;

import edu.pict.mcpservice.kafkaEvents.LogEvent;
import edu.pict.mcpservice.kafkaEvents.SecurityAlertEvent;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Component
@Order(5) // Run after pattern matching
public class BurstTrafficStrategy implements ThreatStrategy {

    @Override
    public boolean isAvailable(SecurityAlertEvent alert, List<LogEvent> history) {
        if (history.size() < 10) return false;

        // Check if the last 20 requests happened in under 5 seconds
        long startTime = history.get(0).getTimestamp();
        long endTime = history.get(history.size() - 1).getTimestamp();

        return (endTime - startTime) < 5000 && history.size() >= 20;
    }

    @Override
    public Duration getBlockDuration() {
        return Duration.ofMinutes(30);
    }

    @Override
    public String getReason() {
        return "BURST_TRAFFIC_DETECTED_BOT_SUSPECT";
    }
}