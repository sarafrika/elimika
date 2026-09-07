package apps.sarafrika.elimika.tenancy.spi;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Organisation Lookup Service Provider Interface
 * <p>
 * Read-only access to organisation identity for other modules. Timetabling uses it to name the
 * organisation behind a scheduled session: an instructor hired by an organisation must see whose
 * work a booking on their calendar is, not just that the time is taken.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2026-08-31
 */
public interface OrganisationLookupService {

    /**
     * Resolves the display name of one organisation.
     *
     * @param organisationUuid the organisation identifier
     * @return the organisation name, or empty when it does not resolve
     */
    Optional<String> findOrganisationName(UUID organisationUuid);

    /**
     * Resolves the display names of several organisations in one query.
     *
     * @param organisationUuids candidate organisation identifiers; nulls are ignored
     * @return names keyed by organisation UUID, omitting identifiers that do not resolve
     */
    Map<UUID, String> findOrganisationNames(Collection<UUID> organisationUuids);

    /**
     * Resolves the town each organisation gives as its base, for several organisations in one query.
     * <p>
     * The town, and nothing finer. An organisation record also holds latitude and longitude, and a
     * directory that says where a training provider operates has no business publishing a point on
     * a map — so this returns the human-readable {@code location} field only, and callers wanting to
     * show "where" must use it rather than formatting the coordinates themselves.
     *
     * @param organisationUuids candidate organisation identifiers; nulls are ignored
     * @return town keyed by organisation UUID, omitting organisations that name no town
     */
    Map<UUID, String> findOrganisationTowns(Collection<UUID> organisationUuids);
}
