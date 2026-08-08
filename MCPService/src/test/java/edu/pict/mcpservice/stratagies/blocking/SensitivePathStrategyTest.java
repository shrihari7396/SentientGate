package edu.pict.mcpservice.stratagies.blocking;

import static org.junit.jupiter.api.Assertions.*;

import edu.pict.mcpservice.kafkaEvents.LogEvent;
import edu.pict.mcpservice.kafkaEvents.SecurityAlertEvent;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SensitivePathStrategyTest {

    private final SensitivePathStrategy strategy = new SensitivePathStrategy();
    private final List<LogEvent> emptyHistory = Collections.emptyList();

    private SecurityAlertEvent alertWithPath(String path) {
        return SecurityAlertEvent.builder().attemptedPath(path).build();
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
    @DisplayName("block duration is 7 days")
    void blockDurationIsSevenDays() {
        assertEquals(Duration.ofDays(7), strategy.getBlockDuration());
    }

    @Test
    @DisplayName("reason is SENSITIVE_PATH_RECONNAISSANCE")
    void reasonString() {
        assertEquals("SENSITIVE_PATH_RECONNAISSANCE", strategy.getReason());
    }

    // ═══════════════════════════════════════════════════════════════════
    // Forbidden paths that should trigger
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Forbidden paths")
    class ForbiddenPaths {

        @ParameterizedTest(name = "blocks: {0}")
        @ValueSource(
                strings = {
                    "/wp-admin",
                    "/wp-admin/install.php",
                    "/.env",
                    "/config.php",
                    "/config.php?debug=1",
                    "/admin/login",
                    "/admin/login?redirect=/dashboard",
                    "/.git",
                    "/.git/config",
                    "/actuator",
                    "/actuator/health",
                    "/actuator/env",
                })
        void shouldBlockForbiddenPaths(String path) {
            assertTrue(
                    strategy.isAvailable(alertWithPath(path), emptyHistory),
                    "Expected block for path: " + path);
        }

        @Test
        @DisplayName("case-insensitive matching")
        void caseInsensitive() {
            assertTrue(strategy.isAvailable(alertWithPath("/WP-ADMIN"), emptyHistory));
            assertTrue(strategy.isAvailable(alertWithPath("/.ENV"), emptyHistory));
            assertTrue(strategy.isAvailable(alertWithPath("/ACTUATOR/health"), emptyHistory));
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // Legitimate paths that should NOT trigger
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Legitimate paths (no false positives)")
    class LegitPaths {

        @ParameterizedTest(name = "allows: {0}")
        @ValueSource(
                strings = {
                    "/api/v1/users",
                    "/products/123",
                    "/health",
                    "/dashboard",
                    "/api/admin-data", // does NOT start with /admin/login
                    "/login",
                    "/api/configuration", // doesn't start with /config.php
                })
        void shouldNotBlockLegitPaths(String path) {
            assertFalse(
                    strategy.isAvailable(alertWithPath(path), emptyHistory),
                    "Should NOT block legitimate path: " + path);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // Edge cases
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("history parameter is ignored")
        void historyIgnored() {
            LogEvent log =
                    LogEvent.builder()
                            .path("/api/v1/users")
                            .statusCode(200)
                            .timestamp(1000L)
                            .build();
            // Even with history present, result depends only on the path
            assertTrue(strategy.isAvailable(alertWithPath("/.env"), List.of(log)));
            assertFalse(strategy.isAvailable(alertWithPath("/api/safe"), List.of(log)));
        }

        @Test
        @DisplayName("path with trailing slash matches")
        void trailingSlash() {
            assertTrue(strategy.isAvailable(alertWithPath("/.git/"), emptyHistory));
        }
    }
}
