package apps.sarafrika.elimika.tenancy.search;

import apps.sarafrika.elimika.shared.search.SearchAvailability;
import apps.sarafrika.elimika.shared.search.SearchFilter;
import apps.sarafrika.elimika.shared.search.SearchGateway;
import apps.sarafrika.elimika.shared.search.SearchPage;
import apps.sarafrika.elimika.shared.search.SearchParamsTranslator;
import apps.sarafrika.elimika.shared.search.SearchRequest;
import apps.sarafrika.elimika.shared.search.SearchUnavailableException;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.tenancy.entity.User;
import apps.sarafrika.elimika.tenancy.repository.UserRepository;
import apps.sarafrika.elimika.tenancy.search.PeopleSearchScopes.PeopleScope;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Routes a user query ({@code q}) to the {@code people} index and hydrates the hits.
 * <p>
 * Every method answers {@link Optional#empty()} when the caller should serve the request from the
 * database instead: no query text, reads not enabled for the index, or the engine unavailable.
 * <p>
 * Hits are hydrated with one query that re-applies the route's authorization predicate, so an index
 * that lags the database (a membership revoked seconds ago) can never widen what the caller sees.
 * <p>
 * Query text is personal data here - people search for names and email addresses - so it is never
 * logged; only the scope label and the query length are.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PeopleSearchService {

    private final SearchGateway searchGateway;
    private final SearchAvailability searchAvailability;
    private final DomainSecurityService domainSecurityService;
    private final UserRepository userRepository;

    /**
     * Every user, for a platform admin. {@code searchParams} are translated against the index's
     * filterable attributes; an unknown key is a 400.
     */
    @Transactional(readOnly = true)
    public Optional<Page<User>> searchAll(String query, Map<String, String> searchParams, Pageable pageable) {
        if (!routable(query)) {
            return Optional.empty();
        }
        PeopleScope scope = PeopleSearchScopes.platformWide(domainSecurityService.isPlatformAdmin());
        SearchFilter filter = SearchParamsTranslator.toFilter(searchParams, PeopleSearchSource.DEFINITION);
        return search(query, filter, scope, pageable, userRepository::findAllByUuidIn);
    }

    /** Users who hold no admin role, for a platform admin choosing whom to promote. */
    @Transactional(readOnly = true)
    public Optional<Page<User>> searchAdminEligible(String query, Pageable pageable) {
        if (!routable(query)) {
            return Optional.empty();
        }
        PeopleScope scope = PeopleSearchScopes.platformWide(domainSecurityService.isPlatformAdmin());
        SearchFilter eligible = SearchFilter.and(
                SearchFilter.eq("is_platform_admin", false),
                SearchFilter.eq("is_org_admin", false));
        return search(query, eligible, scope, pageable, userRepository::findAdminEligibleByUuidIn);
    }

    /** Active members of one organisation, for that organisation's managers (and platform admins). */
    @Transactional(readOnly = true)
    public Optional<Page<User>> searchOrganisationRoster(UUID organisationUuid, String query, Pageable pageable) {
        if (!routable(query)) {
            return Optional.empty();
        }
        PeopleScope scope = PeopleSearchScopes.organisationRoster(organisationUuid,
                domainSecurityService.isPlatformAdmin(),
                domainSecurityService.managesOrganisation(organisationUuid));
        return search(query, null, scope, pageable,
                uuids -> userRepository.findOrganisationMembersByUuidIn(uuids, organisationUuid));
    }

    /**
     * Whether the database fallback of a roster search may match on email: only for a platform
     * admin, mirroring the index route where an organisation manager searches names only.
     */
    public boolean rosterMatchesEmail() {
        return domainSecurityService.isPlatformAdmin();
    }

    private boolean routable(String query) {
        return StringUtils.hasText(query) && searchAvailability.isReadEnabled(PeopleSearchSource.INDEX);
    }

    private Optional<Page<User>> search(String query, SearchFilter filter, PeopleScope scope, Pageable pageable,
                                        Function<List<UUID>, List<User>> hydrate) {
        String text = query.trim();
        SearchRequest request = new SearchRequest(PeopleSearchSource.INDEX, text, filter, scope.scope(),
                SearchPaging.sorts(pageable, PeopleSearchSource.DEFINITION),
                SearchPaging.page(pageable), SearchPaging.size(pageable), List.of(), scope.searchOn());
        log.debug("People search: scope={} query_length={}", scope.scope().label(), text.length());
        SearchPage result;
        try {
            result = searchGateway.search(request);
        } catch (SearchUnavailableException ex) {
            log.warn("People search unavailable (scope={}), falling back to the database: {}",
                    scope.scope().label(), ex.getMessage());
            return Optional.empty();
        }
        List<UUID> uuids = SearchPaging.hitUuids(result);
        List<User> rows = uuids.isEmpty() ? List.of() : hydrate.apply(uuids);
        return Optional.of(SearchPaging.toPage(result, rows, User::getUuid, pageable));
    }
}
