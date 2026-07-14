package apps.sarafrika.elimika.resourcing.spi;

import java.util.Optional;
import java.util.UUID;

/**
 * Read-only resource lookups for other modules.
 */
public interface ResourceLookupService {

    Optional<ResourceSummary> getResource(UUID resourceUuid);

    boolean belongsToOrganisation(UUID resourceUuid, UUID organisationUuid);
}
