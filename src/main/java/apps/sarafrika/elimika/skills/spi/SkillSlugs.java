package apps.sarafrika.elimika.skills.spi;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * The one slug rule, shared by the taxonomy, name resolution and the backfill migration.
 * <p>
 * Lower-cases the name and collapses every run of characters outside {@code [a-z0-9]} to a single
 * hyphen, trimming hyphens at both ends: {@code "Java  Programming!"} becomes {@code java-programming}.
 * It is exactly the SQL used by the seed migration,
 * {@code TRIM(BOTH '-' FROM REGEXP_REPLACE(LOWER(name), '[^a-z0-9]+', '-', 'g'))}, so a name resolves
 * at runtime to the same skill the backfill linked it to. Accented letters are dropped, not folded,
 * for the same reason.
 */
public final class SkillSlugs {

    private static final Pattern NON_SLUG = Pattern.compile("[^a-z0-9]+");

    private SkillSlugs() {
    }

    /** The slug of a name, or an empty string when the name has no character in {@code [a-z0-9]}. */
    public static String slugify(String name) {
        if (name == null) {
            return "";
        }
        String slug = NON_SLUG.matcher(name.toLowerCase(Locale.ROOT)).replaceAll("-");
        int start = 0;
        int end = slug.length();
        while (start < end && slug.charAt(start) == '-') {
            start++;
        }
        while (end > start && slug.charAt(end - 1) == '-') {
            end--;
        }
        return slug.substring(start, end);
    }
}
