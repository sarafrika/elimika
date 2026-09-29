package apps.sarafrika.elimika.tenancy.search;

import apps.sarafrika.elimika.shared.search.SearchAvailability;
import apps.sarafrika.elimika.shared.search.SearchFilter;
import apps.sarafrika.elimika.shared.search.SearchGateway;
import apps.sarafrika.elimika.shared.search.SearchPage;
import apps.sarafrika.elimika.shared.search.SearchParamsTranslator;
import apps.sarafrika.elimika.shared.search.SearchRequest;
import apps.sarafrika.elimika.shared.search.SearchScope;
import apps.sarafrika.elimika.shared.search.SearchUnavailableException;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.tenancy.entity.Organisation;
import apps.sarafrika.elimika.tenancy.repository.OrganisationRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Serves an organisation query ({@code q}) from the {@code organisations} index and hydrates the hits
 * in hit order. There is no database fallback: with search off, the index's reads not enabled, or the
 * engine failing, every method throws {@link SearchUnavailableException} (503).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrganisationSearchService {

    private final SearchGateway searchGateway;
    private final SearchAvailability searchAvailability;
    private final DomainSecurityService domainSecurityService;
    private final OrganisationRepository organisationRepository;

    /** Organisations the caller may see: every one for a platform admin, active and verified otherwise. */
    @Transactional(readOnly = true)
    public Page<Organisation> search(String query, Map<String, String> searchParams, Pageable pageable) {
        boolean platformAdmin = domainSecurityService.isPlatformAdmin();
        SearchFilter filter = SearchParamsTranslator.toFilter(searchParams, OrganisationSearchSource.DEFINITION);
        return search(query, filter, OrganisationSearchScopes.forCaller(platformAdmin), pageable,
                organisation -> OrganisationSearchScopes.visible(organisation, platformAdmin));
    }

    /** Organisations awaiting admin verification, for platform admins. */
    @Transactional(readOnly = true)
    public Page<Organisation> searchPending(String query, Pageable pageable) {
        SearchScope scope = OrganisationSearchScopes.pendingQueue(domainSecurityService.isPlatformAdmin());
        return search(query, SearchFilter.eq("admin_verified", false), scope, pageable,
                organisation -> !organisation.isDeleted() && !Boolean.TRUE.equals(organisation.getAdminVerified()));
    }

    private Page<Organisation> search(String query, SearchFilter filter, SearchScope scope, Pageable pageable,
                                      Predicate<Organisation> recheck) {
        if (!StringUtils.hasText(query)) {
            throw new IllegalArgumentException("q must not be blank");
        }
        if (!searchAvailability.isReadEnabled(OrganisationSearchSource.INDEX)) {
            throw new SearchUnavailableException("Search is not enabled for " + OrganisationSearchSource.INDEX);
        }
        String text = query.trim();
        SearchRequest request = new SearchRequest(OrganisationSearchSource.INDEX, text, filter, scope,
                SearchPaging.sorts(pageable, OrganisationSearchSource.DEFINITION),
                SearchPaging.page(pageable), SearchPaging.size(pageable), List.of(), null);
        log.debug("Organisation search: scope={} query_length={}", scope.label(), text.length());
        SearchPage result = searchGateway.search(request);
        List<UUID> uuids = SearchPaging.hitUuids(result);
        List<Organisation> rows = uuids.isEmpty() ? List.of()
                : organisationRepository.findByUuidIn(uuids).stream().filter(recheck).toList();
        return SearchPaging.toPage(result, rows, Organisation::getUuid, pageable);
    }
}
