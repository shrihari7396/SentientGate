package edu.pict.mcpservice.stratagies.blocking;

import edu.pict.mcpservice.kafkaEvents.LogEvent;
import edu.pict.mcpservice.kafkaEvents.SecurityAlertEvent;
import java.time.Duration;
import java.util.List;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class PatternMatchStrategy implements ThreatStrategy {

    private static final List<String> MALICIOUS_PATTERNS =
            List.of(
                    "../",
                    "etc/passwd",
                    "select",
                    "union",
                    "insert",
                    "drop",
                    "--",
                    "' or ",
                    "1=1",
                    "<script>",
                    "alert(",
                    "waitfor delay");

    @Override
    public boolean isAvailable(SecurityAlertEvent alert, List<LogEvent> history) {
        String path = alert.getAttemptedPath().toLowerCase();
        return MALICIOUS_PATTERNS.stream().anyMatch(path::contains);
    }

    @Override
    public Duration getBlockDuration() {
        return Duration.ofDays(1);
    }

    @Override
    public String getReason() {
        return "CRITICAL_INJECTION_ATTEMPT";
    }
}
