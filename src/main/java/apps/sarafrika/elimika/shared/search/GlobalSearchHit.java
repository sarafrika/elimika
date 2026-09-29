package apps.sarafrika.elimika.shared.search;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * One row of the global search box: just enough to render a result and link to it. Built by a
 * {@link GlobalSearchProvider} straight from the stored document, without a database query, so it
 * only ever carries attributes the index already stores.
 *
 * @param type      the result type, e.g. {@code courses}; see {@link GlobalSearchProvider#type()}
 * @param uuid      the entity's UUID, the key the UI links with
 * @param title     the primary label (course name, class title, person's full name)
 * @param subtitle  a secondary label (creator, organisation, headline), or {@code null}
 * @param imageUrl  a public image URL, or {@code null}
 * @param highlight the matched text with the engine's {@code <em>} markers, or {@code null}
 */
public record GlobalSearchHit(
        @JsonProperty("type") String type,
        @JsonProperty("uuid") UUID uuid,
        @JsonProperty("title") String title,
        @JsonProperty("subtitle") String subtitle,
        @JsonProperty("image_url") String imageUrl,
        @JsonProperty("highlight") String highlight
) {

    private static final String MARK = "<em>";

    /** A stored attribute as text: strings as-is, lists joined with commas, blanks as {@code null}. */
    public static String text(Map<String, Object> document, String attribute) {
        if (document == null) {
            return null;
        }
        Object value = document.get(attribute);
        if (value == null) {
            return null;
        }
        String text = value instanceof Collection<?> values
                ? values.stream().filter(Objects::nonNull).map(Object::toString).collect(Collectors.joining(", "))
                : value.toString();
        return text.isBlank() ? null : text;
    }

    /**
     * The first of {@code attributes} whose highlighted copy contains a match marker, or {@code null}
     * when the text matched none of them (or nothing was searched).
     */
    public static String highlight(SearchHit hit, String... attributes) {
        if (hit == null || hit.formatted() == null) {
            return null;
        }
        for (String attribute : attributes) {
            String formatted = text(hit.formatted(), attribute);
            if (formatted != null && formatted.contains(MARK)) {
                return formatted;
            }
        }
        return null;
    }
}
