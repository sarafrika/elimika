package apps.sarafrika.elimika.profile.spi;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Reads and writes one section of the user-owned profile. Writes are scoped to the owning user, so a
 * caller cannot reach another user's item by its UUID; an identical claim is updated, never duplicated.
 */
public interface ProfileSectionService<D> {

    List<D> list(UUID userUuid);

    /** Batch read in insertion order, for search documents and matching. */
    List<D> listForUsers(Collection<UUID> userUuids);

    Optional<D> find(UUID itemUuid);

    D create(UUID userUuid, D item);

    D update(UUID userUuid, UUID itemUuid, D item);

    void delete(UUID userUuid, UUID itemUuid);

    /** Filters with the shared search operators; {@code restrictToUsers}, when not null, bounds the owners. */
    Page<D> search(Map<String, String> searchParams, Collection<UUID> restrictToUsers, Pageable pageable);

    long count(UUID userUuid);
}
