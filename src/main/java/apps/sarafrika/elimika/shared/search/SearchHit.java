package apps.sarafrika.elimika.shared.search;

import java.util.Map;
import java.util.UUID;

/**
 * One document returned by a search.
 *
 * @param uuid      the document's primary key - the UUID of the row it projects
 * @param document  the stored document as a JSON-shaped map
 * @param formatted the highlighted copy of the document, or {@code null} when no text was searched
 */
public record SearchHit(UUID uuid, Map<String, Object> document, Map<String, Object> formatted) {
}
