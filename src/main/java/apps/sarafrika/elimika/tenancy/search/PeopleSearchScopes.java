package apps.sarafrika.elimika.tenancy.search;

import apps.sarafrika.elimika.shared.search.SearchFilter;
import apps.sarafrika.elimika.shared.search.SearchScope;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;

/**
 * Who may search the {@code people} index, and over what. Mirrors the guards on the database
 * routes it serves:
 * <ul>
 *     <li>a platform admin searches everyone ({@code /users/search}, {@code /admin/users/eligible});</li>
 *     <li>on an organisation's roster, a manager of that organisation searches its active members by
 *     name only - never by email, so a manager cannot probe whether an address belongs to one of
 *     their members;</li>
 *     <li>anyone else is refused.</li>
 * </ul>
 */
final class PeopleSearchScopes {

    /** Name attributes an organisation manager may match on; no email, username or user number. */
    static final List<String> NAME_ATTRIBUTES = List.of("full_name", "first_name", "last_name");

    private PeopleSearchScopes() {
    }

    /** A scope plus the attributes the query text may match ({@code null} for all searchable ones). */
    record PeopleScope(SearchScope scope, List<String> searchOn) {
    }

    static PeopleScope platformWide(boolean callerIsPlatformAdmin) {
        if (!callerIsPlatformAdmin) {
            throw new AccessDeniedException("People search is restricted to platform administrators");
        }
        return new PeopleScope(SearchScope.unrestricted("platform-admin"), null);
    }

    static PeopleScope organisationRoster(UUID organisationUuid, boolean callerIsPlatformAdmin, boolean callerManagesOrganisation) {
        SearchFilter members = SearchFilter.eq("organisation_uuids", organisationUuid);
        if (callerIsPlatformAdmin) {
            return new PeopleScope(SearchScope.of(members, "platform-admin:org:" + organisationUuid), null);
        }
        if (callerManagesOrganisation) {
            return new PeopleScope(SearchScope.of(members, "org-manager:" + organisationUuid), NAME_ATTRIBUTES);
        }
        throw new AccessDeniedException("Caller does not manage organisation " + organisationUuid);
    }
}
