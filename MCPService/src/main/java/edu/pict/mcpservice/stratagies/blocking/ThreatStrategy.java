package edu.pict.mcpservice.stratagies.blocking;

import edu.pict.mcpservice.kafkaEvents.LogEvent;
import edu.pict.mcpservice.kafkaEvents.SecurityAlertEvent;
import java.time.Duration;
import java.util.List;

public interface ThreatStrategy {
    boolean isAvailable(SecurityAlertEvent alert, List<LogEvent> history);

    Duration getBlockDuration();

    String getReason();
}
