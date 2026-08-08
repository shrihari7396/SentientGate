package edu.pict.mcpservice.stratagies.blocking;

import static org.junit.jupiter.api.Assertions.*;

import edu.pict.mcpservice.kafkaEvents.LogEvent;
import edu.pict.mcpservice.kafkaEvents.SecurityAlertEvent;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class PatternMatchStrategyTest {

    private final PatternMatchStrategy strategy = new PatternMatchStrategy();
    private final List<LogEvent> emptyHistory = Collections.emptyList();

    private SecurityAlertEvent alertWithPath(String path) {
        return SecurityAlertEvent.builder().attemptedPath(path).build();
    }

    // ═══════════════════════════════════════════════════════════════════
    // Contract tests — interface, order, block duration, reason
    // ═══════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("implements ThreatStrategy interface")
    void implementsThreatStrategy() {
        assertInstanceOf(ThreatStrategy.class, strategy);
    }

    @Test
    @DisplayName("block duration is 1 day")
    void blockDurationIsOneDay() {
        assertEquals(Duration.ofDays(1), strategy.getBlockDuration());
    }

    @Test
    @DisplayName("reason is CRITICAL_INJECTION_ATTEMPT")
    void reasonIsCriticalInjection() {
        assertEquals("CRITICAL_INJECTION_ATTEMPT", strategy.getReason());
    }

    // ═══════════════════════════════════════════════════════════════════
    // Original plain-text payloads — regression guard
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Plain-text payloads (regression)")
    class PlainTextPayloads {

        @ParameterizedTest(name = "blocks: {0}")
        @ValueSource(
                strings = {
                    "/api/../etc/passwd",
                    "/search?q=select * from users",
                    "/path?q=union all select 1",
                    "/path?q=insert into users",
                    "/admin?q=drop table users",
                    "/api/data?a=1--comment",
                    "/login?user=' or 1=1",
                    "/page?x=<script>alert(1)</script>",
                    "/path?x=alert(document.cookie)",
                    "/api?q=waitfor delay '0:0:5'",
                    "/search?q=1=1",
                })
        void shouldBlockPlainTextPayloads(String path) {
            assertTrue(
                    strategy.isAvailable(alertWithPath(path), emptyHistory),
                    "Expected block for path: " + path);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // URL-encoded and double-encoded payloads
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Encoding-based bypasses")
    class EncodingBypasses {

        @ParameterizedTest(name = "blocks encoded: {0}")
        @ValueSource(
                strings = {
                    // Single URL-encoded
                    "/api/%2e%2e%2fetc/passwd", // ../etc/passwd
                    "/search?q=%73elect%20*%20from%20users", // select * from users
                    "/search?q=%75nion%20all", // union all
                    // Double URL-encoded
                    "/api/%252e%252e%252f", // double-encoded ../
                    "/search?q=%2573elect", // double-encoded select
                    "/search?q=%2575nion", // double-encoded union
                    // Mixed encoding
                    "/path?q=%73elect%20%2a%20from%20users", // select * from users
                })
        void shouldBlockEncodedPayloads(String path) {
            assertTrue(
                    strategy.isAvailable(alertWithPath(path), emptyHistory),
                    "Expected block for encoded path: " + path);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // Inline SQL comment bypass
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("SQL comment injection bypasses")
    class SqlCommentBypasses {

        @ParameterizedTest(name = "blocks comment-injected: {0}")
        @ValueSource(
                strings = {
                    "/search?q=sel/**/ect * from users",
                    "/search?q=uni/**/on all select 1",
                    "/search?q=ins/**/ert into users",
                    "/search?q=dr/**/op table users",
                    "/search?q=sel/*comment*/ect users",
                    "/search?q=uni/*bypass*/on%20sel/*x*/ect",
                })
        void shouldBlockCommentInjectedPayloads(String path) {
            assertTrue(
                    strategy.isAvailable(alertWithPath(path), emptyHistory),
                    "Expected block for comment-injected path: " + path);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // Combined encoding + comment attacks
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Combined encoding and comment attacks")
    class CombinedAttacks {

        @ParameterizedTest(name = "blocks combined: {0}")
        @ValueSource(
                strings = {
                    // URL-encoded comment markers + keyword
                    "/search?q=%73el%2f%2a%2a%2fect", // sel/**/ect URL-encoded
                    // Double-encoded path traversal
                    "/api/%252e%252e%252fetc%252fpasswd",
                })
        void shouldBlockCombinedAttacks(String path) {
            assertTrue(
                    strategy.isAvailable(alertWithPath(path), emptyHistory),
                    "Expected block for combined attack: " + path);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // Legitimate paths — must NOT trigger (false-positive checks)
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Legitimate paths (no false positives)")
    class LegitimatePathsShouldPass {

        @ParameterizedTest(name = "allows: {0}")
        @ValueSource(
                strings = {
                    "/products/select-category",
                    "/api/insert-user-note",
                    "/reports/dropdown-menu",
                    "/docs/selection-guide",
                    "/catalog/unionist-history",
                    "/blog/selector-patterns-in-css",
                    "/api/v1/users/123",
                    "/health",
                    "/api/products?page=1&size=10",
                    "/dashboard/overview",
                })
        void shouldNotBlockLegitimatePathsWithSubstrings(String path) {
            assertFalse(
                    strategy.isAvailable(alertWithPath(path), emptyHistory),
                    "Should NOT block legitimate path: " + path);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // Normalization unit tests (internal pipeline)
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Normalization pipeline")
    class NormalizationTests {

        @Test
        @DisplayName("null input normalizes to empty string")
        void nullInput() {
            assertEquals("", strategy.normalize(null));
        }

        @Test
        @DisplayName("single URL-decode pass")
        void singleUrlDecode() {
            assertEquals("select", strategy.normalize("%73elect"));
        }

        @Test
        @DisplayName("double URL-decode pass")
        void doubleUrlDecode() {
            assertEquals("select", strategy.normalize("%2573elect"));
        }

        @Test
        @DisplayName("strips inline SQL comments")
        void stripsComments() {
            String result = strategy.normalize("sel/**/ect");
            assertEquals("select", result);
        }

        @Test
        @DisplayName("NFKC normalization collapses fullwidth characters")
        void nfkcNormalization() {
            // Fullwidth 'Ｓ' (U+FF33) should normalize to 's' after NFKC + lowercase
            String result = strategy.normalize("\uFF33elect");
            assertEquals("select", result);
        }

        @Test
        @DisplayName("lowercases everything")
        void lowercasing() {
            assertEquals("select", strategy.normalize("SELECT"));
        }

        @Test
        @DisplayName("malformed percent-encoding is handled gracefully")
        void malformedEncoding() {
            // %ZZ is not valid percent-encoding — should not throw
            assertDoesNotThrow(() -> strategy.normalize("/path?q=%ZZbad"));
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // Edge cases
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("case-insensitive keyword matching")
        void caseInsensitive() {
            assertTrue(strategy.isAvailable(alertWithPath("/q=SELECT * FROM users"), emptyHistory));
            assertTrue(strategy.isAvailable(alertWithPath("/q=SeLeCt * FROM users"), emptyHistory));
        }

        @Test
        @DisplayName("path traversal with backslashes")
        void backslashTraversal() {
            // ../ is the marker — backslash variant doesn't apply unless normalized
            assertFalse(
                    strategy.isAvailable(alertWithPath("/api/..\\etc\\passwd"), emptyHistory));
        }

        @Test
        @DisplayName("XSS with mixed case does not bypass substring check")
        void xssMixedCase() {
            // <script> is checked after lowercasing
            assertTrue(strategy.isAvailable(alertWithPath("/page?x=<SCRIPT>"), emptyHistory));
        }

        @Test
        @DisplayName("SQL keyword at word boundary in query param")
        void sqlKeywordInQueryParam() {
            assertTrue(
                    strategy.isAvailable(
                            alertWithPath("/api/data?q=union select 1,2,3"), emptyHistory));
        }

        @Test
        @DisplayName("SQL keyword NOT at word boundary should not trigger")
        void sqlKeywordNotAtBoundary() {
            assertFalse(
                    strategy.isAvailable(alertWithPath("/reselection/overview"), emptyHistory));
        }
    }
}
