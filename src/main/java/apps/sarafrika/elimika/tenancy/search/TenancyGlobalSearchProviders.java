package apps.sarafrika.elimika.tenancy.search;

import apps.sarafrika.elimika.shared.search.GlobalSearchHit;
import apps.sarafrika.elimika.shared.search.GlobalSearchProvider;
import apps.sarafrika.elimika.shared.search.SearchFilter;
import apps.sarafrika.elimika.shared.search.SearchHit;
import apps.sarafrika.elimika.shared.search.SearchIndexDefinition;
import apps.sarafrika.elimika.shared.search.SearchResults;
import apps.sarafrika.elimika.shared.search.SearchScope;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.tenancy.entity.User;
import apps.sarafrika.elimika.tenancy.repository.UserRepository;
import apps.sarafrika.elimika.tenancy.spi.UserLookupService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * The tenancy module's indexes in global search.
 */
final class TenancyGlobalSearchProviders {

    private TenancyGlobalSearchProviders() {
    }

    /** {@code organisations}: every one for a platform admin; active, verified ones for everyone else, anonymous included. */
    @Component
    static class Organisations implements GlobalSearchProvider {

        private final DomainSecurityService domainSecurityService;

        Organisations(DomainSecurityService domainSecurityService) {
            this.domainSecurityService = domainSecurityService;
        }

        @Override
        public String type() {
            return OrganisationSearchSource.INDEX;
        }

        @Override
        public SearchIndexDefinition definition() {
            return OrganisationSearchSource.DEFINITION;
        }

        @Override
        public Optional<SearchScope> scopeForCurrentCaller() {
            return Optional.of(OrganisationSearchScopes.forCaller(domainSecurityService.isPlatformAdmin()));
        }

        @Override
        public GlobalSearchHit toHit(SearchHit hit) {
            Map<String, Object> document = hit.document();
            return new GlobalSearchHit(type(), hit.uuid(),
                    GlobalSearchHit.text(document, "name"),
                    GlobalSearchHit.text(document, "location"),
                    null,
                    GlobalSearchHit.highlight(hit, "name", "location", "description"));
        }
    }

    /**
     * {@code people}: personal data, so the narrowest provider.
     * <ul>
     *     <li>A platform admin searches everyone, on every searchable attribute.</li>
     *     <li>An organisation manager searches the active members of the organisations they manage, by
     *     name only - never email, username or user number, the same rule as the roster search.</li>
     *     <li>Everyone else, anonymous callers included, never sees the type.</li>
     * </ul>
     */
    @Component
    static class People implements GlobalSearchProvider {

        private final DomainSecurityService domainSecurityService;
        private final UserLookupService userLookupService;
        private final UserRepository userRepository;

        People(DomainSecurityService domainSecurityService, UserLookupService userLookupService,
               UserRepository userRepository) {
            this.domainSecurityService = domainSecurityService;
            this.userLookupService = userLookupService;
            this.userRepository = userRepository;
        }

        @Override
        public String type() {
            return PeopleSearchSource.INDEX;
        }

        @Override
        public SearchIndexDefinition definition() {
            return PeopleSearchSource.DEFINITION;
        }

        @Override
        public Optional<SearchScope> scopeForCurrentCaller() {
            if (domainSecurityService.isPlatformAdmin()) {
                return Optional.of(PeopleSearchScopes.platformWide(true).scope());
            }
            List<UUID> managed = managedOrganisations();
            if (managed.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(SearchScope.of(SearchFilter.and(
                    SearchFilter.in("organisation_uuids", managed),
                    SearchFilter.eq("active", true)), "org-manager:" + managed.size()));
        }

        @Override
        public List<String> searchOnForCurrentCaller() {
            return domainSecurityService.isPlatformAdmin() ? null : PeopleSearchScopes.NAME_ATTRIBUTES;
        }

        @Override
        public GlobalSearchHit toHit(SearchHit hit) {
            Map<String, Object> document = hit.document();
            return new GlobalSearchHit(type(), hit.uuid(),
                    GlobalSearchHit.text(document, "full_name"),
                    GlobalSearchHit.text(document, "domains"),
                    null,
                    // Names only: the engine highlights every attribute holding a query word, and a
                    // manager must not learn that their text matched an email address.
                    GlobalSearchHit.highlight(hit, "full_name"));
        }

        /**
         * The engine narrows, SQL authorizes: a manager's hits are re-checked with the roster's
         * membership predicate, over all their managed organisations in one query, so a membership
         * revoked after the document was written (or while indexing is failing) never surfaces. A
         * platform admin may see everyone, so their hits pass through.
         */
        @Override
        public List<GlobalSearchHit> recheck(List<GlobalSearchHit> hits) {
            if (hits.isEmpty() || domainSecurityService.isPlatformAdmin()) {
                return hits;
            }
            List<UUID> managed = managedOrganisations();
            if (managed.isEmpty()) {
                return List.of();
            }
            List<UUID> uuids = hits.stream().map(GlobalSearchHit::uuid).toList();
            Set<UUID> members = userRepository.findMembersOfAnyOrganisationByUuidIn(uuids, managed).stream()
                    .map(User::getUuid)
                    .collect(Collectors.toSet());
            return SearchResults.inHitOrder(uuids, hits.stream().filter(hit -> members.contains(hit.uuid())).toList(),
                    GlobalSearchHit::uuid);
        }

        private List<UUID> managedOrganisations() {
            UUID callerUuid = domainSecurityService.getCurrentUserUuid();
            if (callerUuid == null) {
                return List.of();
            }
            return userLookupService.getActiveUserOrganizations(callerUuid).stream()
                    .filter(domainSecurityService::managesOrganisation)
                    .distinct()
                    .toList();
        }
    }
}
