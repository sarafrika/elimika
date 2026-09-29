package apps.sarafrika.elimika.classes.search;

import apps.sarafrika.elimika.classes.util.enums.ClassMarketplaceJobStatus;
import apps.sarafrika.elimika.shared.search.SearchFilter;
import apps.sarafrika.elimika.shared.search.SearchScope;

import java.util.UUID;

/**
 * The {@code marketplace_jobs} index boundary, the same rule as the SQL job listing: platform admins
 * see every job; staff of the organisation the caller filtered by see that organisation's jobs in any
 * status; everyone else sees OPEN jobs only.
 */
public final class MarketplaceJobSearchScopes {

    private MarketplaceJobSearchScopes() {
    }

    /**
     * @param platformAdmin      whether the caller is a platform admin
     * @param organisationUuid   the organisation the request filters by, or {@code null}
     * @param staffsOrganisation whether the caller staffs {@code organisationUuid}
     */
    public static SearchScope forCaller(boolean platformAdmin, UUID organisationUuid, boolean staffsOrganisation) {
        if (platformAdmin) {
            return SearchScope.unrestricted("platform-admin");
        }
        if (organisationUuid != null && staffsOrganisation) {
            return SearchScope.of(SearchFilter.eq("organisation_uuid", organisationUuid), "org-staff:" + organisationUuid);
        }
        return SearchScope.of(SearchFilter.eq("status", ClassMarketplaceJobStatus.OPEN.name()), "open-jobs");
    }
}
