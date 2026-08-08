package edu.pict.mcpservice.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class InputNormalizerTest {

    // ═══════════════════════════════════════════════════════════════════
    // Full normalize() — URL decode + SQL comment strip + NFKC + lowercase
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Full normalization pipeline")
    class FullNormalize {

        @Test
        @DisplayName("null input normalizes to empty string")
        void nullInput() {
            assertEquals("", InputNormalizer.normalize(null));
        }

        @Test
        @DisplayName("single URL-decode pass")
        void singleUrlDecode() {
            assertEquals("select", InputNormalizer.normalize("%73elect"));
        }

        @Test
        @DisplayName("double URL-decode pass")
        void doubleUrlDecode() {
            assertEquals("select", InputNormalizer.normalize("%2573elect"));
        }

        @Test
        @DisplayName("strips inline SQL comments")
        void stripsComments() {
            assertEquals("select", InputNormalizer.normalize("sel/**/ect"));
        }

        @Test
        @DisplayName("strips multi-line SQL comments")
        void stripsMultiLineComments() {
            assertEquals("select", InputNormalizer.normalize("sel/*\ncomment\n*/ect"));
        }

        @Test
        @DisplayName("NFKC normalization collapses fullwidth characters")
        void nfkcNormalization() {
            assertEquals("select", InputNormalizer.normalize("\uFF33elect"));
        }

        @Test
        @DisplayName("lowercases everything")
        void lowercasing() {
            assertEquals("select", InputNormalizer.normalize("SELECT"));
        }

        @Test
        @DisplayName("malformed percent-encoding is handled gracefully")
        void malformedEncoding() {
            assertDoesNotThrow(() -> InputNormalizer.normalize("/path?q=%ZZbad"));
        }

        @Test
        @DisplayName("combined: double-encoded + SQL comment + uppercase")
        void combinedNormalization() {
            // %2573 → %73 → s, then sel/**/ect → select, then uppercase → lowercase
            assertEquals("select", InputNormalizer.normalize("%2573el/**/ECT"));
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // normalizePath() — URL decode + NFKC + lowercase (no SQL comment strip)
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Path-only normalization")
    class PathNormalize {

        @Test
        @DisplayName("null input normalizes to empty string")
        void nullInput() {
            assertEquals("", InputNormalizer.normalizePath(null));
        }

        @Test
        @DisplayName("URL-decodes encoded paths")
        void urlDecodes() {
            assertEquals("/.env", InputNormalizer.normalizePath("/%2eenv"));
        }

        @Test
        @DisplayName("double-decodes paths")
        void doubleDecodes() {
            assertEquals("/.env", InputNormalizer.normalizePath("/%252eenv"));
        }

        @Test
        @DisplayName("lowercases paths")
        void lowercases() {
            assertEquals("/wp-admin", InputNormalizer.normalizePath("/WP-ADMIN"));
        }

        @Test
        @DisplayName("does NOT strip SQL comments (by design)")
        void doesNotStripComments() {
            // normalizePath intentionally skips SQL comment stripping
            String result = InputNormalizer.normalizePath("/path/**/test");
            assertTrue(result.contains("/**/"));
        }
    }
}
