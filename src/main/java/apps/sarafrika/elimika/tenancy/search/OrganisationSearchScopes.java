package apps.sarafrika.elimika.tenancy.search;

import apps.sarafrika.elimika.shared.search.SearchFilter;
import apps.sarafrika.elimika.shared.search.SearchScope;
import apps.sarafrika.elimika.tenancy.entity.Organisation;
import org.springframework.security.access.AccessDeniedException;

/**
 * Who may see which organisations in search: a platform admin sees every (non-deleted) one,
 * including the pending-verification queue; everyone else only active, verified organisations.
 */
final class OrganisationSearchScopes {

    private OrganisationSearchScopes() {
    }

    static SearchScope forCaller(boolean callerIsPlatformAdmin) {
        if (callerIsPlatformAdmin) {
            return SearchScope.unrestricted("platform-admin");
        }
        return SearchScope.of(SearchFilter.and(
                SearchFilter.eq("active", true),
                SearchFilter.eq("admin_verified", true)), "public-organisations");
    }

    static SearchScope pendingQueue(boolean callerIsPlatformAdmin) {
        if (!callerIsPlatformAdmin) {
            throw new AccessDeniedException("The pending organisation queue is restricted to platform administrators");
        }
        return SearchScope.unrestricted("platform-admin:pending");
    }

    /** The same rule as {@link #forCaller}, re-applied to hydrated rows. */
    static boolean visible(Organisation organisation, boolean callerIsPlatformAdmin) {
        if (organisation.isDeleted()) {
            return false;
        }
        return callerIsPlatformAdmin
                || organisation.isActive() && Boolean.TRUE.equals(organisation.getAdminVerified());
    }
}
