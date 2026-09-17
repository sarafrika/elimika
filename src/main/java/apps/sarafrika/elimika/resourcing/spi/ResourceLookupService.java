package apps.sarafrika.elimika.resourcing.spi;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Read-only resource lookups for other modules.
 */
public interface ResourceLookupService {

    Optional<ResourceSummary> getResource(UUID resourceUuid);

    boolean belongsToOrganisation(UUID resourceUuid, UUID organisationUuid);

    /** Several resources in one query; unknown uuids are simply absent from the result. */
    List<ResourceListing> findResources(Collection<UUID> resourceUuids);
}
