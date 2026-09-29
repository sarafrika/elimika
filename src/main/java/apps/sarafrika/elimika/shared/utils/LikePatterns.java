package apps.sarafrika.elimika.shared.utils;

import java.util.Locale;

/**
 * Helpers for building SQL {@code LIKE} patterns from user input.
 * <p>
 * User-supplied text must never be interpreted as a pattern: {@code %} and {@code _} are
 * wildcards and {@code \} is the escape character, so all three are escaped. Every
 * {@code LIKE} built from these patterns must declare {@link #ESCAPE_CHAR} as its escape
 * character ({@code criteriaBuilder.like(expr, pattern, LikePatterns.ESCAPE_CHAR)} or
 * {@code LIKE :pattern ESCAPE '\'} in JPQL).
 */
public final class LikePatterns {

    public static final char ESCAPE_CHAR = '\\';

    private LikePatterns() {
    }

    /**
     * Escapes {@code \}, {@code %} and {@code _} so the value matches literally.
     */
    public static String escape(String value) {
        if (value == null) {
            return null;
        }
        StringBuilder escaped = new StringBuilder(value.length() + 8);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == ESCAPE_CHAR || c == '%' || c == '_') {
                escaped.append(ESCAPE_CHAR);
            }
            escaped.append(c);
        }
        return escaped.toString();
    }

    /**
     * Lower-cases and escapes the value, for comparison against a lower-cased column.
     */
    public static String escapeLower(String value) {
        return value == null ? null : escape(value.toLowerCase(Locale.ROOT));
    }

    /**
     * {@code %value%}, lower-cased and escaped.
     */
    public static String containsLower(String value) {
        return "%" + escapeLower(value) + "%";
    }

    /**
     * {@code value%}, lower-cased and escaped.
     */
    public static String startsWithLower(String value) {
        return escapeLower(value) + "%";
    }

    /**
     * {@code %value}, lower-cased and escaped.
     */
    public static String endsWithLower(String value) {
        return "%" + escapeLower(value);
    }
}
