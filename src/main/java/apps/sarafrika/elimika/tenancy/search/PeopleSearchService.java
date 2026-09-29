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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
 * Serves a user query ({@code q}) from the {@code people} index and hydrates the hits.
 * <p>
 * Free text over people is served only by search: there is no database fallback. When search is off,
 * the index's reads are not enabled, or the engine fails, every method throws
 * {@link SearchUnavailableException} (503). A filter or sort the index cannot express is an
 * {@link IllegalArgumentException} (400).
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

    /**
     * Request keys the user listing accepts under a different name than the index attribute. The UI
     * sends {@code user_domain} together with {@code q}; on the index it is the {@code domains} array.
     */
    private static final Map<String, String> PARAM_ALIASES = Map.of("user_domain", "domains");

    private final SearchGateway searchGateway;
    private final SearchAvailability searchAvailability;
    private final DomainSecurityService domainSecurityService;
    private final UserRepository userRepository;

    /**
     * Every user, for a platform admin. {@code searchParams} are translated against the index's
     * filterable attributes ({@code user_domain} is accepted as {@code domains}); an unknown key is a 400.
     */
    @Transactional(readOnly = true)
    public Page<User> searchAll(String query, Map<String, String> searchParams, Pageable pageable) {
        PeopleScope scope = PeopleSearchScopes.platformWide(domainSecurityService.isPlatformAdmin());
        SearchFilter filter = SearchParamsTranslator.toFilter(aliased(searchParams), PeopleSearchSource.DEFINITION);
        return search(query, filter, scope, pageable, userRepository::findAllByUuidIn);
    }

    /** Users who hold no admin role, for a platform admin choosing whom to promote. */
    @Transactional(readOnly = true)
    public Page<User> searchAdminEligible(String query, Pageable pageable) {
        PeopleScope scope = PeopleSearchScopes.platformWide(domainSecurityService.isPlatformAdmin());
        SearchFilter eligible = SearchFilter.and(
                SearchFilter.eq("is_platform_admin", false),
                SearchFilter.eq("is_org_admin", false));
        return search(query, eligible, scope, pageable, userRepository::findAdminEligibleByUuidIn);
    }

    /** Active members of one organisation, for that organisation's managers (and platform admins). */
    @Transactional(readOnly = true)
    public Page<User> searchOrganisationRoster(UUID organisationUuid, String query, Pageable pageable) {
        PeopleScope scope = PeopleSearchScopes.organisationRoster(organisationUuid,
                domainSecurityService.isPlatformAdmin(),
                domainSecurityService.managesOrganisation(organisationUuid));
        return search(query, null, scope, pageable,
                uuids -> userRepository.findOrganisationMembersByUuidIn(uuids, organisationUuid));
    }

    /** {@code user_domain=student} becomes {@code domains=student}; other keys pass through. */
    private static Map<String, String> aliased(Map<String, String> searchParams) {
        if (searchParams == null || searchParams.isEmpty()) {
            return searchParams;
        }
        Map<String, String> params = new HashMap<>(searchParams.size());
        searchParams.forEach((key, value) -> {
            String target = key == null ? null : PARAM_ALIASES.getOrDefault(key, key);
            if (params.containsKey(target)) {
                throw new IllegalArgumentException("Conflicting filters on " + target);
            }
            params.put(target, target != null && !target.equals(key) && value != null
                    ? value.toLowerCase(Locale.ROOT) : value);
        });
        return params;
    }

    private Page<User> search(String query, SearchFilter filter, PeopleScope scope, Pageable pageable,
                              Function<List<UUID>, List<User>> hydrate) {
        if (!StringUtils.hasText(query)) {
            throw new IllegalArgumentException("q must not be blank");
        }
        if (!searchAvailability.isReadEnabled(PeopleSearchSource.INDEX)) {
            throw new SearchUnavailableException("Search is not enabled for " + PeopleSearchSource.INDEX);
        }
        String text = query.trim();
        SearchRequest request = new SearchRequest(PeopleSearchSource.INDEX, text, filter, scope.scope(),
                SearchPaging.sorts(pageable, PeopleSearchSource.DEFINITION),
                SearchPaging.page(pageable), SearchPaging.size(pageable), List.of(), scope.searchOn());
        log.debug("People search: scope={} query_length={}", scope.scope().label(), text.length());
        SearchPage result = searchGateway.search(request);
        List<UUID> uuids = SearchPaging.hitUuids(result);
        List<User> rows = uuids.isEmpty() ? List.of() : hydrate.apply(uuids);
        return SearchPaging.toPage(result, rows, User::getUuid, pageable);
    }
}
