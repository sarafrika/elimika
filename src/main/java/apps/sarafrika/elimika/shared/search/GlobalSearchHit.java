package apps.sarafrika.elimika.shared.search;

import com.fasterxml.jackson.annotation.JsonInclude;
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
 * @param distanceBand on a near-me search only, how far away the result is as a coarse band
 *                     ({@code "<2 km"} ... {@code ">25 km"}, see {@link NearMe#distanceBand}); never metres
 * @param context   where the hit lives, for types the UI links inside a parent (a lesson item inside
 *                  its course, a class of a course), or {@code null}; built from stored attributes
 */
public record GlobalSearchHit(
        @JsonProperty("type") String type,
        @JsonProperty("uuid") UUID uuid,
        @JsonProperty("title") String title,
        @JsonProperty("subtitle") String subtitle,
        @JsonProperty("image_url") String imageUrl,
        @JsonProperty("highlight") String highlight,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonProperty("distance_band") String distanceBand,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonProperty("context") Context context
) {

    public GlobalSearchHit(String type, UUID uuid, String title, String subtitle, String imageUrl, String highlight) {
        this(type, uuid, title, subtitle, imageUrl, highlight, null, null);
    }

    public GlobalSearchHit(String type, UUID uuid, String title, String subtitle, String imageUrl, String highlight,
                           String distanceBand) {
        this(type, uuid, title, subtitle, imageUrl, highlight, distanceBand, null);
    }

    /** This hit carrying the given distance band. */
    public GlobalSearchHit withDistanceBand(String band) {
        return new GlobalSearchHit(type, uuid, title, subtitle, imageUrl, highlight, band, context);
    }

    /** This hit carrying the given context; an empty context is dropped. */
    public GlobalSearchHit withContext(Context newContext) {
        Context kept = newContext == null || newContext.isEmpty() ? null : newContext;
        return new GlobalSearchHit(type, uuid, title, subtitle, imageUrl, highlight, distanceBand, kept);
    }

    /**
     * Where a hit sits, so the UI can deep-link it: {@code course_content} carries its course and
     * lesson, a class its course. Absent fields are left out of the JSON.
     *
     * @param courseUuid the course the hit belongs to
     * @param lessonUuid the lesson the hit belongs to (a lesson's own uuid for a lesson hit)
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Context(
            @JsonProperty("course_uuid") UUID courseUuid,
            @JsonProperty("lesson_uuid") UUID lessonUuid
    ) {

        public static Context ofCourse(UUID courseUuid) {
            return new Context(courseUuid, null);
        }

        boolean isEmpty() {
            return courseUuid == null && lessonUuid == null;
        }
    }

    /** A stored attribute as a UUID, or {@code null} when it is absent or not a UUID. */
    public static UUID uuid(Map<String, Object> document, String attribute) {
        if (document == null) {
            return null;
        }
        Object value = document.get(attribute);
        if (value instanceof UUID id) {
            return id;
        }
        if (value == null) {
            return null;
        }
        try {
            return UUID.fromString(value.toString());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

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
