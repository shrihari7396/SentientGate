package edu.pict.mcpservice.stratagies.blocking;

import edu.pict.mcpservice.kafkaEvents.LogEvent;
import edu.pict.mcpservice.kafkaEvents.SecurityAlertEvent;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.Duration;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class PatternMatchStrategy implements ThreatStrategy {

    // ── Maximum URL-decode iterations to catch double/triple encoding ──
    private static final int MAX_DECODE_PASSES = 3;

    // ── Inline SQL comment pattern: /* ... */ (non-greedy) ──
    private static final Pattern SQL_COMMENT_PATTERN =
            Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);

    // ── Word-boundary-aware patterns for SQL / command keywords ──
    // Custom lookaround treats hyphens as word characters so that
    // hyphenated path segments like /select-category or /insert-user-note
    // do NOT false-positive. Standard \b considers '-' a boundary.
    private static final List<Pattern> KEYWORD_PATTERNS =
            List.of(
                    Pattern.compile("(?<![\\w-])select(?![\\w-])", Pattern.CASE_INSENSITIVE),
                    Pattern.compile("(?<![\\w-])union(?![\\w-])", Pattern.CASE_INSENSITIVE),
                    Pattern.compile("(?<![\\w-])insert(?![\\w-])", Pattern.CASE_INSENSITIVE),
                    Pattern.compile("(?<![\\w-])drop(?![\\w-])", Pattern.CASE_INSENSITIVE),
                    Pattern.compile(
                            "(?<![\\w-])waitfor\\s+delay(?![\\w-])",
                            Pattern.CASE_INSENSITIVE));

    // ── Unambiguous payload markers — plain substring match is sufficient ──
    // These strings are almost never legitimate path components.
    private static final List<String> SUBSTRING_MARKERS =
            List.of("../", "etc/passwd", "--", "' or ", "1=1", "<script>", "alert(");

    @Override
    public boolean isAvailable(SecurityAlertEvent alert, List<LogEvent> history) {
        String normalized = normalize(alert.getAttemptedPath());

        // Check unambiguous substring markers first (cheap)
        for (String marker : SUBSTRING_MARKERS) {
            if (normalized.contains(marker)) {
                return true;
            }
        }

        // Check word-boundary-aware keyword patterns
        for (Pattern pattern : KEYWORD_PATTERNS) {
            if (pattern.matcher(normalized).find()) {
                return true;
            }
        }

        return false;
    }

    @Override
    public Duration getBlockDuration() {
        return Duration.ofDays(1);
    }

    @Override
    public String getReason() {
        return "CRITICAL_INJECTION_ATTEMPT";
    }

    // ── Internal normalization pipeline ──────────────────────────────────
    // 1. Iterative URL-decode (handles double/triple encoding)
    // 2. Strip inline SQL comments (/* ... */)
    // 3. Unicode NFKC normalization (collapses fullwidth chars, etc.)
    // 4. Lowercase

    /**
     * Normalizes the input through iterative URL-decoding, SQL comment removal,
     * Unicode NFKC normalization, and lowercasing. Package-private for testability.
     */
    String normalize(String input) {
        if (input == null) {
            return "";
        }

        // Step 1: Iterative URL-decode (capped to avoid DoS on crafted inputs)
        String decoded = iterativeDecode(input);

        // Step 2: Strip inline SQL comment sequences
        decoded = SQL_COMMENT_PATTERN.matcher(decoded).replaceAll("");

        // Step 3: Unicode NFKC normalization
        decoded = Normalizer.normalize(decoded, Normalizer.Form.NFKC);

        // Step 4: Lowercase
        return decoded.toLowerCase();
    }

    private String iterativeDecode(String input) {
        String current = input;
        for (int i = 0; i < MAX_DECODE_PASSES; i++) {
            String decoded = urlDecode(current);
            if (decoded.equals(current)) {
                break; // No further decoding possible
            }
            current = decoded;
        }
        return current;
    }

    private String urlDecode(String input) {
        try {
            return URLDecoder.decode(input, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            // Malformed percent-encoding — return as-is
            return input;
        }
    }
}
