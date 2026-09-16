package apps.sarafrika.elimika.tenancy.spi;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Read-only training branch lookups so other modules can validate and label branches.
 */
public interface TrainingBranchLookupService {

    /**
     * Resolves a non-deleted branch owned by the organisation; empty when either argument is null or nothing matches.
     */
    Optional<BranchLocation> findBranch(UUID organisationUuid, UUID branchUuid);

    /**
     * Maps branch uuids to names, skipping nulls; deleted branches keep their label for historical records.
     */
    Map<UUID, String> findBranchNames(Collection<UUID> branchUuids);
}
