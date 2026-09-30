package apps.sarafrika.elimika.tenancy.search;

import apps.sarafrika.elimika.shared.search.SearchFilter;
import apps.sarafrika.elimika.shared.search.SearchScope;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;

/**
 * Who may search the {@code people} index, and over what. Mirrors the guards on the database
 * routes it serves:
 * <ul>
 *     <li>a platform admin searches everyone ({@code /users/search}, {@code /admin/users/eligible});</li>
 *     <li>on an organisation's roster, a manager of that organisation searches its active members by
 *     name and email - the roster DTO already shows them their members' emails, so this reveals
 *     nothing new. A query containing {@code @} is an exact match on {@code email_normalized};
 *     username and user number stay admin-only;</li>
 *     <li>anyone else is refused.</li>
 * </ul>
 */
final class PeopleSearchScopes {

    /** Name attributes an organisation manager may match on in global search; no email, username or user number. */
    static final List<String> NAME_ATTRIBUTES = List.of("full_name", "first_name", "last_name");

    /** What a manager may match on their own roster: names and email, never username or user number. */
    static final List<String> ROSTER_MANAGER_ATTRIBUTES = List.of("full_name", "first_name", "last_name", "email");

    private PeopleSearchScopes() {
    }

    /**
     * A scope plus the attributes the query text may match ({@code null} for all searchable ones).
     * {@code exactEmail} turns a query containing {@code @} into an exact filter on
     * {@code email_normalized}, so a manager matches one address, not every member sharing its tokens.
     */
    record PeopleScope(SearchScope scope, List<String> searchOn, boolean exactEmail) {

        PeopleScope(SearchScope scope, List<String> searchOn) {
            this(scope, searchOn, false);
        }

        /** The exact email filter for {@code query}, or {@code null} when it does not apply. */
        SearchFilter emailFilter(String query) {
            if (!exactEmail || query == null || query.indexOf('@') < 0) {
                return null;
            }
            return SearchFilter.eq("email_normalized", query.trim().toLowerCase(Locale.ROOT));
        }
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
            return new PeopleScope(SearchScope.of(members, "org-manager:" + organisationUuid),
                    ROSTER_MANAGER_ATTRIBUTES, true);
        }
        throw new AccessDeniedException("Caller does not manage organisation " + organisationUuid);
    }
}
