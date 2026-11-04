package apps.sarafrika.elimika.shared.spi;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/** Display names only - never contact details - for modules that hold just a user or organisation uuid. */
public interface PartyNameLookupService {

    /** A person's display name per known user uuid; unknown uuids are left out. */
    Map<UUID, String> findUserNames(Collection<UUID> userUuids);

    /** An organisation's name per known organisation uuid; unknown uuids are left out. */
    Map<UUID, String> findOrganisationNames(Collection<UUID> organisationUuids);
}
