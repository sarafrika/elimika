package apps.sarafrika.elimika.shared.tracking;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Masks free-text and personal values in a raw query string before it is persisted to the request audit log.
 *
 * <p>Only the value of a sensitive parameter is replaced, with {@code [redacted:<length>]} where the length is
 * that of the decoded value, so search volume and term length stay measurable. Keys, separators, parameter order
 * and every non-sensitive pair (for example {@code action=approve} or {@code user_uuid_eq=...}) are kept
 * byte-identical, so audit consumers that match on them keep working.
 *
 * <p>A key is sensitive, compared case-insensitively after URL-decoding, when it is one of {@code q},
 * {@code search}, {@code near}, {@code lat} or {@code lng}; when it contains {@code email} (covering
 * {@code email}, {@code recipient_email} and filters such as {@code email_eq}); or when it ends in one of the
 * free-text operators {@code _like}, {@code _startswith} or {@code _endswith}.
 */
public final class QueryStringRedactor {

    private static final Set<String> SENSITIVE_KEYS = Set.of("q", "search", "near", "lat", "lng");
    private static final String[] SENSITIVE_SUFFIXES = {"_like", "_startswith", "_endswith"};
    private static final Pattern ALREADY_REDACTED = Pattern.compile("\\[redacted:\\d+]");

    private QueryStringRedactor() {
    }

    public static String redact(String queryString) {
        if (queryString == null || queryString.isEmpty()) {
            return queryString;
        }
        StringBuilder out = new StringBuilder(queryString.length());
        int start = 0;
        while (start <= queryString.length()) {
            int end = queryString.indexOf('&', start);
            if (end < 0) {
                end = queryString.length();
            }
            out.append(redactPair(queryString.substring(start, end)));
            if (end < queryString.length()) {
                out.append('&');
            }
            start = end + 1;
        }
        return out.toString();
    }

    private static String redactPair(String pair) {
        int eq = pair.indexOf('=');
        if (eq < 0) {
            return pair;
        }
        String rawValue = pair.substring(eq + 1);
        if (rawValue.isEmpty() || !isSensitive(decode(pair.substring(0, eq)))) {
            return pair;
        }
        if (ALREADY_REDACTED.matcher(rawValue).matches()) {
            // Keeps redaction a fixed point, so the historical backfill can run over rows twice.
            return pair;
        }
        return pair.substring(0, eq + 1) + "[redacted:" + decode(rawValue).length() + "]";
    }

    static boolean isSensitive(String key) {
        String normalized = key.trim().toLowerCase(Locale.ROOT);
        if (SENSITIVE_KEYS.contains(normalized) || normalized.contains("email")) {
            return true;
        }
        for (String suffix : SENSITIVE_SUFFIXES) {
            if (normalized.endsWith(suffix)) {
                return true;
            }
        }
        return false;
    }

    private static String decode(String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException malformed) {
            return value;
        }
    }
}
