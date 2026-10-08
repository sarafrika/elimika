package apps.sarafrika.elimika.course.internal.search;

import apps.sarafrika.elimika.course.dto.ApplyCatalogueApplication;
import apps.sarafrika.elimika.course.dto.ApplyCatalogueFacets;
import apps.sarafrika.elimika.course.dto.ApplyCatalogueFitFacet;
import apps.sarafrika.elimika.course.dto.ApplyCatalogueItem;
import apps.sarafrika.elimika.course.dto.ApplyCatalogueResponse;
import apps.sarafrika.elimika.course.dto.CatalogueCategoryFacet;
import apps.sarafrika.elimika.course.dto.CatalogueShowFacet;
import apps.sarafrika.elimika.course.internal.search.CatalogueSearchService.Show;
import apps.sarafrika.elimika.course.internal.search.CatalogueSearchService.Sort;
import apps.sarafrika.elimika.instructor.spi.InstructorLookupService;
import apps.sarafrika.elimika.profile.spi.ProfessionalProfileService;
import apps.sarafrika.elimika.profile.spi.UserSkillDTO;
import apps.sarafrika.elimika.shared.search.FederatedSearchResult;
import apps.sarafrika.elimika.shared.search.SearchAvailability;
import apps.sarafrika.elimika.shared.search.SearchFilter;
import apps.sarafrika.elimika.shared.search.SearchGateway;
import apps.sarafrika.elimika.shared.search.SearchPage;
import apps.sarafrika.elimika.shared.search.SearchRequest;
import apps.sarafrika.elimika.shared.search.SearchResults;
import apps.sarafrika.elimika.shared.search.SearchScope;
import apps.sarafrika.elimika.shared.search.SearchSort;
import apps.sarafrika.elimika.shared.search.SearchUnavailableException;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.shared.storage.util.FileUrlResolver;
import apps.sarafrika.elimika.shared.utils.PageMetadata;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static apps.sarafrika.elimika.course.internal.search.CatalogueSearchService.integer;
import static apps.sarafrika.elimika.course.internal.search.CatalogueSearchService.longValue;
import static apps.sarafrika.elimika.course.internal.search.CatalogueSearchService.parseUuid;
import static apps.sarafrika.elimika.course.internal.search.CatalogueSearchService.text;
import static apps.sarafrika.elimika.course.internal.search.CatalogueSearchService.texts;
import static apps.sarafrika.elimika.course.internal.search.CatalogueSearchService.uuids;

/**
 * The instructor "apply to train" catalogue: the public catalogue's courses and programmes in one
 * ranking, filtered by how they fit the caller - not yet applied to, matching their wallet skills, or
 * already applied to.
 * <ul>
 *     <li><b>Scope.</b> Every request, facet queries included, carries the public scope of its index
 *     ({@link CatalogueSearchScopes}); the caller's applications and skills only ever narrow it.</li>
 *     <li><b>Caller facts.</b> Applied UUIDs come from the caller's own instructor applications and skill
 *     UUIDs from their own wallet (free-text skills without a taxonomy entry are ignored). Nothing about
 *     the caller is taken from the request.</li>
 *     <li><b>Fit.</b> Meilisearch filters on {@code uuid} and {@code skill_uuids}: applied is
 *     {@code uuid IN applied}, open is {@code uuid NOT IN applied}, skills is open plus
 *     {@code skill_uuids IN wallet}. Fit counts are extra queries in the per-index facet multi-search,
 *     under every other filter.</li>
 *     <li><b>Hydration.</b> One SQL query per type re-checks public visibility and loads the minimum
 *     training fee (never indexed); dropped hits restate the total ({@link SearchResults#total}).</li>
 * </ul>
 * Search off or failing raises {@link SearchUnavailableException}; there is no database fallback.
 */
@Service
public class ApplyCatalogueSearchService {

    /** Matches nothing: stands in for an empty IN list. */
    static final UUID NO_MATCH = new UUID(0L, 0L);
    static final String UUID_ATTRIBUTE = "uuid";
    static final String SKILL_UUIDS = "skill_uuids";

    private static final SearchScope PUBLIC_COURSES = CatalogueSearchScopes.courses(false, null, Set.of());
    private static final SearchScope PUBLIC_PROGRAMS = CatalogueSearchScopes.programs(false, Set.of());

    private final SearchAvailability availability;
    private final SearchGateway gateway;
    private final ApplyCatalogueRecords records;
    private final DomainSecurityService domainSecurityService;
    private final InstructorLookupService instructorLookupService;
    private final ProfessionalProfileService professionalProfileService;

    public ApplyCatalogueSearchService(SearchAvailability availability, SearchGateway gateway,
                                       ApplyCatalogueRecords records, DomainSecurityService domainSecurityService,
                                       InstructorLookupService instructorLookupService,
                                       ProfessionalProfileService professionalProfileService) {
        this.availability = availability;
        this.gateway = gateway;
        this.records = records;
        this.domainSecurityService = domainSecurityService;
        this.instructorLookupService = instructorLookupService;
        this.professionalProfileService = professionalProfileService;
    }

    // ===== Request vocabulary =====

    public enum Fit {
        OPEN, SKILLS, APPLIED
    }

    /** The two indexes and how each names the category filter. */
    enum Index {
        COURSES(CourseSearchSource.INDEX, "category_uuids", PUBLIC_COURSES),
        PROGRAMMES(ProgramSearchSource.INDEX, "category_uuid", PUBLIC_PROGRAMS);

        final String name;
        final String categoryAttribute;
        final SearchScope scope;

        Index(String name, String categoryAttribute, SearchScope scope) {
            this.name = name;
            this.categoryAttribute = categoryAttribute;
            this.scope = scope;
        }

        static Index of(String indexName) {
            return PROGRAMMES.name.equals(indexName) ? PROGRAMMES : COURSES;
        }
    }

    /** A parsed request. {@code categoryUuids} is empty when nothing is selected. */
    public record Query(String q, Show show, List<UUID> categoryUuids, Fit fit, Sort sort, int page, int size) {

        public Query {
            q = StringUtils.hasText(q) ? q.trim() : null;
            show = show == null ? Show.ALL : show;
            categoryUuids = categoryUuids == null ? List.of() : List.copyOf(new LinkedHashSet<>(categoryUuids));
            fit = fit == null ? Fit.OPEN : fit;
            sort = sort == null ? (q == null ? Sort.POPULAR : Sort.RELEVANCE) : sort;
            if (page < 0) {
                throw new IllegalArgumentException("page must be 0 or greater");
            }
            if (size < 1 || size > CatalogueSearchService.MAX_SIZE) {
                throw new IllegalArgumentException("size must be between 1 and " + CatalogueSearchService.MAX_SIZE);
            }
        }
    }

    /** What the server knows about the caller: their wallet skills and their own applications. */
    record Caller(Set<UUID> skillUuids, Map<UUID, ApplyCatalogueApplication> courseApplications,
                  Map<UUID, ApplyCatalogueApplication> programApplications) {

        Set<UUID> applied(Index index) {
            return (index == Index.COURSES ? courseApplications : programApplications).keySet();
        }

        ApplyCatalogueApplication application(Index index, UUID uuid) {
            return (index == Index.COURSES ? courseApplications : programApplications).get(uuid);
        }
    }

    // ===== Search =====

    public ApplyCatalogueResponse search(Query query) {
        for (Index index : Index.values()) {
            if (!availability.isReadEnabled(index.name)) {
                throw new SearchUnavailableException("Search is not enabled for " + index.name);
            }
        }
        Caller caller = currentCaller();
        List<SearchSort> sort = CatalogueSearchService.sortFor(query.sort());

        Facets facets = facets(query, caller);

        List<Index> shown = shown(query.show());
        List<Hit> hits = new ArrayList<>();
        if (shown.size() == 1) {
            Index index = shown.getFirst();
            SearchPage page = gateway.search(new SearchRequest(index.name, query.q(),
                    filter(index, query, caller, query.fit(), true), index.scope, sort, query.page(), query.size(),
                    List.of(), null));
            page.hits().forEach(hit -> hits.add(new Hit(index, hit.uuid(), hit.document())));
        } else {
            List<SearchRequest> requests = shown.stream().map(index -> new SearchRequest(index.name, query.q(),
                    filter(index, query, caller, query.fit(), true), index.scope, sort, 0, 1, List.of(), null))
                    .toList();
            FederatedSearchResult result = gateway.federatedSearch(requests, query.page() * query.size(), query.size());
            for (FederatedSearchResult.Hit hit : result.hits()) {
                hits.add(new Hit(Index.of(hit.index()), hit.uuid(), hit.document()));
            }
        }
        long engineTotal = shown.stream().mapToLong(index -> facets.totals.getOrDefault(index, 0L)).sum();

        List<ApplyCatalogueItem> items = hydrate(hits, caller);
        long total = SearchResults.total(engineTotal, hits.size(), items.size());
        PageMetadata metadata = PageMetadata.from(
                new PageImpl<>(items, PageRequest.of(query.page(), query.size()), total));
        return new ApplyCatalogueResponse(items, metadata, facets.toDto(query, categoryNames(facets, query)));
    }

    private static List<Index> shown(Show show) {
        return switch (show) {
            case ALL -> List.of(Index.COURSES, Index.PROGRAMMES);
            case COURSES -> List.of(Index.COURSES);
            case PROGRAMMES -> List.of(Index.PROGRAMMES);
        };
    }

    /** Resolves the caller's skills and applications from their own records; never from the request. */
    Caller currentCaller() {
        UUID userUuid = domainSecurityService.getCurrentUserUuid();
        if (userUuid == null) {
            throw new AccessDeniedException("An authenticated instructor is required");
        }
        UUID instructorUuid = instructorLookupService.findInstructorUuidByUserUuid(userUuid).orElse(null);
        Set<UUID> skills = new LinkedHashSet<>();
        for (UserSkillDTO skill : professionalProfileService.skills().list(userUuid)) {
            if (skill != null && skill.skillUuid() != null) {
                skills.add(skill.skillUuid());
            }
        }
        return new Caller(Set.copyOf(skills), records.courseApplications(instructorUuid),
                records.programApplications(instructorUuid));
    }

    private record Hit(Index index, UUID uuid, Map<String, Object> document) {
    }

    /** The query's filters on {@code index} under {@code fit}, with or without the category selection. */
    static SearchFilter filter(Index index, Query query, Caller caller, Fit fit, boolean withCategory) {
        List<SearchFilter> parts = new ArrayList<>();
        if (withCategory && !query.categoryUuids().isEmpty()) {
            parts.add(SearchFilter.in(index.categoryAttribute, query.categoryUuids()));
        }
        parts.add(fitFilter(index, caller, fit));
        return SearchFilter.and(parts);
    }

    static SearchFilter fitFilter(Index index, Caller caller, Fit fit) {
        Set<UUID> applied = caller.applied(index);
        SearchFilter notApplied = applied.isEmpty() ? null : SearchFilter.notIn(UUID_ATTRIBUTE, applied);
        return switch (fit) {
            case APPLIED -> SearchFilter.in(UUID_ATTRIBUTE, applied.isEmpty() ? List.of(NO_MATCH) : applied);
            case OPEN -> notApplied;
            case SKILLS -> SearchFilter.and(SearchFilter.in(SKILL_UUIDS,
                    caller.skillUuids().isEmpty() ? List.of(NO_MATCH) : caller.skillUuids()), notApplied);
        };
    }

    // ===== Facets =====

    /** One slot per facet query: the base query, the category query, or one other fit value. */
    record FacetSlot(Index index, Kind kind, Fit fit) {
    }

    enum Kind {
        BASE, CATEGORY, FIT
    }

    /**
     * The facet requests in one multi-search. Per index: the base query (every filter; its total is the
     * {@code show} count and the selected fit's count), the category query when a category is selected
     * (every filter but the category), and one count query per other fit value.
     */
    static List<SearchRequest> facetRequests(Query query, Caller caller, List<FacetSlot> slots) {
        List<SearchRequest> requests = new ArrayList<>();
        for (Index index : Index.values()) {
            boolean categorySelected = !query.categoryUuids().isEmpty();
            requests.add(new SearchRequest(index.name, query.q(), filter(index, query, caller, query.fit(), true),
                    index.scope, List.of(), 0, 1,
                    categorySelected ? List.of() : List.of(index.categoryAttribute), null));
            slots.add(new FacetSlot(index, Kind.BASE, query.fit()));
            if (categorySelected) {
                requests.add(new SearchRequest(index.name, query.q(), filter(index, query, caller, query.fit(), false),
                        index.scope, List.of(), 0, 1, List.of(index.categoryAttribute), null));
                slots.add(new FacetSlot(index, Kind.CATEGORY, query.fit()));
            }
            for (Fit fit : Fit.values()) {
                if (fit != query.fit()) {
                    requests.add(new SearchRequest(index.name, query.q(), filter(index, query, caller, fit, true),
                            index.scope, List.of(), 0, 1, List.of(), null));
                    slots.add(new FacetSlot(index, Kind.FIT, fit));
                }
            }
        }
        return requests;
    }

    private Facets facets(Query query, Caller caller) {
        List<FacetSlot> slots = new ArrayList<>();
        List<SearchPage> pages = gateway.multiSearchPerIndex(facetRequests(query, caller, slots));
        Facets facets = new Facets();
        for (int i = 0; i < slots.size(); i++) {
            FacetSlot slot = slots.get(i);
            SearchPage page = pages.get(i);
            if (slot.kind() == Kind.BASE) {
                facets.totals.put(slot.index(), page.totalHits());
            }
            if (!shown(query.show()).contains(slot.index())) {
                continue;
            }
            if (slot.kind() != Kind.CATEGORY) {
                facets.fits.merge(slot.fit(), page.totalHits(), Long::sum);
            }
            boolean categoryFromThis = query.categoryUuids().isEmpty() ? slot.kind() == Kind.BASE
                    : slot.kind() == Kind.CATEGORY;
            if (categoryFromThis) {
                page.facetDistribution().getOrDefault(slot.index().categoryAttribute, Map.of())
                        .forEach((value, count) -> facets.categories.merge(value, count, Long::sum));
            }
        }
        return facets;
    }

    private static final class Facets {
        final Map<Index, Long> totals = new EnumMap<>(Index.class);
        final Map<Fit, Long> fits = new EnumMap<>(Fit.class);
        final Map<String, Long> categories = new HashMap<>();

        ApplyCatalogueFacets toDto(Query query, Map<UUID, String> categoryNames) {
            long courses = totals.getOrDefault(Index.COURSES, 0L);
            long programmes = totals.getOrDefault(Index.PROGRAMMES, 0L);
            Map<UUID, Long> byCategory = new LinkedHashMap<>();
            categories.forEach((value, count) -> {
                UUID uuid = parseUuid(value);
                if (uuid != null && count > 0) {
                    byCategory.merge(uuid, count, Long::sum);
                }
            });
            query.categoryUuids().forEach(uuid -> byCategory.putIfAbsent(uuid, 0L));
            List<CatalogueCategoryFacet> category = byCategory.entrySet().stream()
                    .filter(entry -> categoryNames.containsKey(entry.getKey()))
                    .map(entry -> new CatalogueCategoryFacet(entry.getKey(), categoryNames.get(entry.getKey()),
                            entry.getValue()))
                    .sorted(Comparator.comparingLong(CatalogueCategoryFacet::count).reversed()
                            .thenComparing(CatalogueCategoryFacet::name, String.CASE_INSENSITIVE_ORDER))
                    .toList();
            return new ApplyCatalogueFacets(
                    new CatalogueShowFacet(courses + programmes, courses, programmes),
                    category,
                    new ApplyCatalogueFitFacet(fits.getOrDefault(Fit.OPEN, 0L), fits.getOrDefault(Fit.SKILLS, 0L),
                            fits.getOrDefault(Fit.APPLIED, 0L)));
        }
    }

    private Map<UUID, String> categoryNames(Facets facets, Query query) {
        Set<UUID> uuids = new LinkedHashSet<>(query.categoryUuids());
        facets.categories.keySet().stream().map(CatalogueSearchService::parseUuid).filter(Objects::nonNull)
                .forEach(uuids::add);
        return records.categoryNames(uuids);
    }

    // ===== Hydration =====

    private List<ApplyCatalogueItem> hydrate(List<Hit> hits, Caller caller) {
        Map<UUID, BigDecimal> courseFees = records.publicCourseFees(uuidsOf(hits, Index.COURSES));
        Map<UUID, BigDecimal> programFees = records.publicProgramFees(uuidsOf(hits, Index.PROGRAMMES));
        List<ApplyCatalogueItem> items = new ArrayList<>(hits.size());
        for (Hit hit : hits) {
            BigDecimal fee = (hit.index() == Index.COURSES ? courseFees : programFees).get(hit.uuid());
            if (fee != null) {
                items.add(item(hit, caller, fee));
            }
        }
        return items;
    }

    private static List<UUID> uuidsOf(List<Hit> hits, Index index) {
        return hits.stream().filter(hit -> hit.index() == index && hit.uuid() != null).map(Hit::uuid).toList();
    }

    private static ApplyCatalogueItem item(Hit hit, Caller caller, BigDecimal fee) {
        Map<String, Object> document = hit.document();
        boolean course = hit.index() == Index.COURSES;
        List<String> categoryNames = texts(document.get("category_names"));
        List<UUID> categoryUuids = uuids(document.get("category_uuids"));
        if (!course && categoryUuids.isEmpty() && parseUuid(text(document, "category_uuid")) != null) {
            categoryUuids = List.of(parseUuid(text(document, "category_uuid")));
            categoryNames = text(document, "category_name") == null ? List.of() : List.of(text(document, "category_name"));
        }
        return new ApplyCatalogueItem(
                course ? CatalogueSearchService.TYPE_COURSE : CatalogueSearchService.TYPE_PROGRAMME,
                hit.uuid(),
                text(document, course ? "name" : "title"),
                text(document, course ? "course_code" : "program_code"),
                categoryUuids,
                categoryNames,
                text(document, "creator_name"),
                FileUrlResolver.publicUrl(text(document, "thumbnail_url")),
                integer(document.get("age_lower_limit")),
                integer(document.get("age_upper_limit")),
                longValue(document.get("lesson_count")),
                longValue(document.get("requirement_count")),
                course ? null : (int) longValue(document.get("course_count")),
                matchesSkills(uuids(document.get(SKILL_UUIDS)), caller.skillUuids()),
                caller.application(hit.index(), hit.uuid()),
                fee);
    }

    private static boolean matchesSkills(Collection<UUID> documentSkills, Set<UUID> callerSkills) {
        return documentSkills.stream().anyMatch(callerSkills::contains);
    }
}
