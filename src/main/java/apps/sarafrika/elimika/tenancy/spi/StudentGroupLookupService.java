package apps.sarafrika.elimika.tenancy.spi;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Student Group Lookup Service Provider Interface
 * <p>
 * Read-only access to organisation student groups (cohorts / streams) for other modules.
 * The Classes module uses this to validate and label the target groups a marketplace job
 * is aimed at, without reaching into tenancy entities or repositories.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2026-07-28
 */
public interface StudentGroupLookupService {

    /**
     * Filters the supplied group identifiers down to the ones that exist and belong to the
     * organisation, preserving the caller's ordering and dropping duplicates.
     *
     * @param organisationUuid The owning organisation
     * @param groupUuids       Candidate group identifiers
     * @return Group identifiers owned by the organisation (empty when none match)
     */
    List<UUID> filterGroupsInOrganisation(UUID organisationUuid, Collection<UUID> groupUuids);

    /**
     * Resolves the display names of the supplied groups, preserving the caller's ordering
     * and skipping identifiers that no longer resolve.
     *
     * @param groupUuids Group identifiers
     * @return Group names (empty when none resolve)
     */
    List<String> getGroupNames(Collection<UUID> groupUuids);
}
