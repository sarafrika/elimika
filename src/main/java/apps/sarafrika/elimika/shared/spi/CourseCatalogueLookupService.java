package apps.sarafrika.elimika.shared.spi;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/**
 * Public course attributes, for modules that list courses they do not own.
 * <p>
 * The catalogue is the caller this exists for. It holds price entries keyed by course uuid and
 * nothing else, so a client rendering a catalogue page had to fetch every course itself — one
 * request per row, against an endpoint that requires a token, which left the public catalogue
 * rendering empty for anonymous visitors while the page's own counters still reported courses.
 * <p>
 * Batch by construction. There is no single-uuid method here on purpose: a lookup per row is the
 * shape this interface exists to remove, and offering one would invite it straight back.
 */
public interface CourseCatalogueLookupService {

    /**
     * The public projection of each course that exists, keyed by uuid.
     * <p>
     * Missing uuids are simply absent from the map rather than mapped to null, so a caller iterating
     * the result skips deleted courses without a null check. An empty or null input returns an empty
     * map without touching the database.
     *
     * @param courseUuids the courses to describe
     * @return public attributes by course uuid, never null
     */
    Map<UUID, CourseCatalogueSnapshot> findPublicByUuids(Collection<UUID> courseUuids);
}
