package edu.pict.mcpservice.util;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.regex.Pattern;

/**
 * Shared input normalization pipeline for threat-detection strategies.
 *
 * <p>Applies the following transformations in order:
 *
 * <ol>
 *   <li>Iterative URL-decode (handles double/triple encoding, capped at 3 passes to avoid DoS)
 *   <li>Strip inline SQL comment sequences ({@code /* ... * /})
 *   <li>Unicode NFKC normalization (collapses fullwidth chars, compatibility decompositions)
 *   <li>Lowercase
 * </ol>
 *
 * <p>This class is stateless and thread-safe. All methods are static.
 */
public final class InputNormalizer {

    private InputNormalizer() {} // utility class — no instantiation

    private static final int MAX_DECODE_PASSES = 3;

    private static final Pattern SQL_COMMENT_PATTERN =
            Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);

    /**
     * Full normalization pipeline: iterative URL-decode → strip SQL comments → NFKC → lowercase.
     *
     * @param input the raw path or query string; null is treated as empty
     * @return the normalized, lowercase result
     */
    public static String normalize(String input) {
        if (input == null) {
            return "";
        }

        String result = iterativeDecode(input);
        result = SQL_COMMENT_PATTERN.matcher(result).replaceAll("");
        result = Normalizer.normalize(result, Normalizer.Form.NFKC);
        return result.toLowerCase();
    }

    /**
     * Lightweight normalization: iterative URL-decode → NFKC → lowercase. Skips SQL comment
     * stripping — useful for path-only checks where SQL comments are irrelevant.
     *
     * @param input the raw path; null is treated as empty
     * @return the normalized, lowercase result
     */
    public static String normalizePath(String input) {
        if (input == null) {
            return "";
        }

        String result = iterativeDecode(input);
        result = Normalizer.normalize(result, Normalizer.Form.NFKC);
        return result.toLowerCase();
    }

    private static String iterativeDecode(String input) {
        String current = input;
        for (int i = 0; i < MAX_DECODE_PASSES; i++) {
            String decoded = urlDecode(current);
            if (decoded.equals(current)) {
                break;
            }
            current = decoded;
        }
        return current;
    }

    private static String urlDecode(String input) {
        try {
            return URLDecoder.decode(input, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return input; // malformed percent-encoding — return as-is
        }
    }
}
