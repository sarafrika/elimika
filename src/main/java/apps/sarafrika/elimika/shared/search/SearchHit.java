package apps.sarafrika.elimika.shared.search;

import java.util.Map;
import java.util.UUID;

/**
 * One document returned by a search.
 *
 * @param uuid              the document's primary key - the UUID of the row it projects
 * @param document          the stored document as a JSON-shaped map, without engine metadata
 *                          (keys starting with {@code _}) other than {@code _geo}
 * @param formatted         the highlighted copy of the document, or {@code null} when no text was searched
 * @param rankingScore      the engine's relevance score in {@code [0, 1]}, or {@code null} when not reported
 * @param geoDistanceMeters metres from the geo sort's or geo filter's point, or {@code null} without one
 */
public record SearchHit(
        UUID uuid,
        Map<String, Object> document,
        Map<String, Object> formatted,
        Double rankingScore,
        Integer geoDistanceMeters
) {

    public SearchHit(UUID uuid, Map<String, Object> document, Map<String, Object> formatted) {
        this(uuid, document, formatted, null, null);
    }
}
