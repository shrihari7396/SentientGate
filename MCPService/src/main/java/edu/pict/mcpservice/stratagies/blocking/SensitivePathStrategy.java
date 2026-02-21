package edu.pict.mcpservice.stratagies.blocking;

import edu.pict.mcpservice.kafkaEvents.LogEvent;
import edu.pict.mcpservice.kafkaEvents.SecurityAlertEvent;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Component
@Order(2) // High priority, run early
public class SensitivePathStrategy implements ThreatStrategy {

    private static final List<String> FORBIDDEN_PATHS = List.of(
            "/wp-admin", "/.env", "/config.php", "/admin/login", "/.git", "/actuator"
    );

    @Override
    public boolean isAvailable(SecurityAlertEvent alert, List<LogEvent> history) {
        String path = alert.getAttemptedPath().toLowerCase();
        return FORBIDDEN_PATHS.stream().anyMatch(path::startsWith);
    }

    @Override
    public Duration getBlockDuration() {
        return Duration.ofDays(7); // Very aggressive for direct recon
    }

    @Override
    public String getReason() {
        return "SENSITIVE_PATH_RECONNAISSANCE";
    }
}