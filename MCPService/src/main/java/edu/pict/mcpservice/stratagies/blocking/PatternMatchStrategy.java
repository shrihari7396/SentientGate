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
        String payload =
                String.join(
                                " ",
                                nullSafe(alert.getAttemptedPath()),
                                nullSafe(alert.getReason()),
                                nullSafe(alert.getMethod()))
                        .toLowerCase();
        boolean alertMatch = MALICIOUS_PATTERNS.stream().anyMatch(payload::contains);
        if (alertMatch) {
            return true;
        }

        if (history == null || history.isEmpty()) {
            return false;
        }

        return history.stream()
                .map(
                        log ->
                                (nullSafe(log.getPath())
                                                + " "
                                                + nullSafe(log.getQueryParams())
                                                + " "
                                                + nullSafe(log.getUserAgent()))
                                        .toLowerCase())
                .anyMatch(candidate -> MALICIOUS_PATTERNS.stream().anyMatch(candidate::contains));
    }

    @Override
    public Duration getBlockDuration() {
        return Duration.ofDays(1);
    }

    @Override
    public String getReason() {
        return "CRITICAL_INJECTION_ATTEMPT";
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
