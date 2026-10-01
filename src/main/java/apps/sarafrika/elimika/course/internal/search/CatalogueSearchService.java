package apps.sarafrika.elimika.course.internal.search;

import apps.sarafrika.elimika.course.dto.CatalogueCategoryFacet;
import apps.sarafrika.elimika.course.dto.CatalogueFacets;
import apps.sarafrika.elimika.course.dto.CatalogueItem;
import apps.sarafrika.elimika.course.dto.CatalogueLevelFacet;
import apps.sarafrika.elimika.course.dto.CataloguePriceFacet;
import apps.sarafrika.elimika.course.dto.CatalogueSearchResponse;
import apps.sarafrika.elimika.course.dto.CatalogueShowFacet;
import apps.sarafrika.elimika.shared.search.FederatedSearchResult;
import apps.sarafrika.elimika.shared.search.SearchAvailability;
import apps.sarafrika.elimika.shared.search.SearchFilter;
import apps.sarafrika.elimika.shared.search.SearchGateway;
import apps.sarafrika.elimika.shared.search.SearchHit;
import apps.sarafrika.elimika.shared.search.SearchPage;
import apps.sarafrika.elimika.shared.search.SearchRequest;
import apps.sarafrika.elimika.shared.search.SearchResults;
import apps.sarafrika.elimika.shared.search.SearchScope;
import apps.sarafrika.elimika.shared.search.SearchSort;
import apps.sarafrika.elimika.shared.search.SearchUnavailableException;
import apps.sarafrika.elimika.shared.spi.ClassDefinitionLookupService;
import apps.sarafrika.elimika.shared.storage.util.FileUrlResolver;
import apps.sarafrika.elimika.shared.utils.PageMetadata;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The public catalogue: one search over the {@code courses} and {@code programs} indexes, merged into
 * one ranking, with disjunctive facet counts.
 * <p>
 * Every caller - anonymous or signed in, admin or not - sees the public catalogue only: the
 * {@code is_public} scope of both indexes, re-checked against the database on every page.
 * <ul>
 *     <li><b>Hits.</b> {@code show=all} is a federated search across both indexes (merged by ranking
 *     score, or by the chosen sort, paged with offset/limit); {@code show=courses|programmes} searches
 *     the one index. Both indexes rank with the same rules and share the sort attributes
 *     ({@code created_at}, {@code rating_bayes}, {@code popularity_30d}), so the merge is consistent.</li>
 *     <li><b>Facets.</b> One non-federated multi-search per request: per index, a base query under
 *     every filter (its exhaustive total is the {@code show} count, and it facets the groups with no
 *     selection), plus one query per group that has a selection, under every filter but that group's.
 *     So each group's counts ignore its own selection and honour all the others; {@code show} narrows
 *     the category, level and price counts to the shown types.</li>
 *     <li><b>Level.</b> A course matches its difficulty. A programme matches a level when any member
 *     course has it ({@code difficulty_uuids}); programmes have no difficulty of their own.</li>
 *     <li><b>Hydration.</b> One SQL query per type per page re-checks public visibility and loads the
 *     live lesson and learner counts (and a programme's course count); course class counts come from
 *     the classes module through {@link ClassDefinitionLookupService} in one grouped query. Hits that
 *     fail the re-check are dropped and the total restated ({@link SearchResults#total}).</li>
 * </ul>
 * Search off, an index's reads off, or the engine failing raises {@link SearchUnavailableException}
 * (503 "Search is unavailable"); there is no database fallback.
 */
@Service
public class CatalogueSearchService {

    public static final int DEFAULT_SIZE = 24;
    public static final int MAX_SIZE = 48;

    static final String TYPE_COURSE = "course";
    static final String TYPE_PROGRAMME = "programme";

    private static final SearchScope PUBLIC_COURSES = CatalogueSearchScopes.courses(false, null, Set.of());
    private static final SearchScope PUBLIC_PROGRAMS = CatalogueSearchScopes.programs(false, Set.of());
    /** Matches nothing: a level that names no difficulty row. */
    private static final UUID NO_MATCH = new UUID(0L, 0L);

    private final SearchAvailability availability;
    private final SearchGateway gateway;
    private final NamedParameterJdbcTemplate jdbc;
    private final ClassDefinitionLookupService classDefinitionLookupService;

    public CatalogueSearchService(SearchAvailability availability, SearchGateway gateway,
                                  NamedParameterJdbcTemplate jdbc,
                                  ClassDefinitionLookupService classDefinitionLookupService) {
        this.availability = availability;
        this.gateway = gateway;
        this.jdbc = jdbc;
        this.classDefinitionLookupService = classDefinitionLookupService;
    }

    // ===== Request vocabulary =====

    public enum Show {
        ALL, COURSES, PROGRAMMES;

        boolean includes(Index index) {
            return this == ALL || (index == Index.COURSES ? this == COURSES : this == PROGRAMMES);
        }
    }

    public enum Level {
        BEGINNER, INTERMEDIATE, ADVANCED
    }

    public enum Price {
        FREE, PAID
    }

    public enum Sort {
        RELEVANCE, NEWEST, RATING, POPULAR
    }

    /** The filter groups that are faceted. */
    private enum Group {
        CATEGORY, LEVEL, PRICE
    }

    /** The two indexes and how each names the shared filters. */
    private enum Index {
        COURSES(CourseSearchSource.INDEX, "category_uuids", "difficulty_uuid", PUBLIC_COURSES),
        PROGRAMMES(ProgramSearchSource.INDEX, "category_uuid", "difficulty_uuids", PUBLIC_PROGRAMS);

        final String name;
        final String categoryAttribute;
        final String levelAttribute;
        final SearchScope scope;

        Index(String name, String categoryAttribute, String levelAttribute, SearchScope scope) {
            this.name = name;
            this.categoryAttribute = categoryAttribute;
            this.levelAttribute = levelAttribute;
            this.scope = scope;
        }

        String attribute(Group group) {
            return switch (group) {
                case CATEGORY -> categoryAttribute;
                case LEVEL -> levelAttribute;
                case PRICE -> "is_free";
            };
        }

        static Index of(String indexName) {
            return PROGRAMMES.name.equals(indexName) ? PROGRAMMES : COURSES;
        }
    }

    /** A parsed catalogue request. Lists are empty when the group has no selection. */
    public record Query(String q, Show show, List<UUID> categoryUuids, List<Level> levels, List<Price> prices,
                        UUID creatorUuid, Sort sort, int page, int size) {

        public Query {
            q = StringUtils.hasText(q) ? q.trim() : null;
            show = show == null ? Show.ALL : show;
            categoryUuids = categoryUuids == null ? List.of() : List.copyOf(new LinkedHashSet<>(categoryUuids));
            levels = levels == null ? List.of() : List.copyOf(new LinkedHashSet<>(levels));
            prices = prices == null ? List.of() : List.copyOf(new LinkedHashSet<>(prices));
            sort = sort == null ? (q == null ? Sort.POPULAR : Sort.RELEVANCE) : sort;
            if (page < 0) {
                throw new IllegalArgumentException("page must be 0 or greater");
            }
            if (size < 1 || size > MAX_SIZE) {
                throw new IllegalArgumentException("size must be between 1 and " + MAX_SIZE);
            }
        }

        boolean selects(Group group) {
            return switch (group) {
                case CATEGORY -> !categoryUuids.isEmpty();
                case LEVEL -> !levels.isEmpty();
                // Both free and paid selected is no filter at all.
                case PRICE -> prices.size() == 1;
            };
        }
    }

    // ===== Search =====

    public CatalogueSearchResponse search(Query query) {
        for (Index index : Index.values()) {
            if (!availability.isReadEnabled(index.name)) {
                throw new SearchUnavailableException("Search is not enabled for " + index.name);
            }
        }
        Map<Level, Set<UUID>> levelUuids = levelUuids();
        List<SearchSort> sort = sortFor(query.sort());

        Facets facets = facets(query, levelUuids);

        List<Index> shown = query.show() == Show.ALL ? List.of(Index.COURSES, Index.PROGRAMMES)
                : List.of(query.show() == Show.COURSES ? Index.COURSES : Index.PROGRAMMES);
        List<Hit> hits = new ArrayList<>();
        if (shown.size() == 1) {
            Index index = shown.getFirst();
            SearchPage page = gateway.search(new SearchRequest(index.name, query.q(),
                    filter(index, query, levelUuids, null), index.scope, sort, query.page(), query.size(),
                    List.of(), null));
            page.hits().forEach(hit -> hits.add(new Hit(index, hit.uuid(), hit.document(), hit.formatted())));
        } else {
            List<SearchRequest> requests = shown.stream().map(index -> new SearchRequest(index.name, query.q(),
                    filter(index, query, levelUuids, null), index.scope, sort, 0, 1, List.of(), null)).toList();
            FederatedSearchResult result = gateway.federatedSearch(requests,
                    query.page() * query.size(), query.size());
            for (FederatedSearchResult.Hit hit : result.hits()) {
                hits.add(new Hit(Index.of(hit.index()), hit.uuid(), hit.document(), hit.formatted()));
            }
        }
        // The facet base queries count exhaustively under exactly these filters; the federated
        // estimate is only an estimate.
        long engineTotal = shown.stream().mapToLong(facets.totals::get).sum();

        List<CatalogueItem> items = hydrate(hits, query.q() != null);
        long total = SearchResults.total(engineTotal, hits.size(), items.size());
        PageMetadata metadata = PageMetadata.from(
                new PageImpl<>(items, PageRequest.of(query.page(), query.size()), total));
        return new CatalogueSearchResponse(items, metadata, facets.toDto(query, levelUuids, categoryNames(facets, query)));
    }

    private record Hit(Index index, UUID uuid, Map<String, Object> document, Map<String, Object> formatted) {
    }

    private static List<SearchSort> sortFor(Sort sort) {
        return switch (sort) {
            case RELEVANCE -> List.of();
            case NEWEST -> List.of(SearchSort.desc("created_at"));
            case RATING -> List.of(SearchSort.desc("rating_bayes"), SearchSort.desc("created_at"));
            case POPULAR -> List.of(SearchSort.desc("popularity_30d"), SearchSort.desc("created_at"));
        };
    }

    /** Every filter of the query on {@code index}, except {@code excluded}'s. */
    private static SearchFilter filter(Index index, Query query, Map<Level, Set<UUID>> levelUuids, Group excluded) {
        List<SearchFilter> parts = new ArrayList<>();
        if (query.creatorUuid() != null) {
            parts.add(SearchFilter.eq(CourseSearchSource.COURSE_CREATOR_UUID, query.creatorUuid()));
        }
        if (excluded != Group.CATEGORY && query.selects(Group.CATEGORY)) {
            parts.add(SearchFilter.in(index.categoryAttribute, query.categoryUuids()));
        }
        if (excluded != Group.LEVEL && query.selects(Group.LEVEL)) {
            Set<UUID> uuids = new LinkedHashSet<>();
            query.levels().forEach(level -> uuids.addAll(levelUuids.getOrDefault(level, Set.of())));
            parts.add(SearchFilter.in(index.levelAttribute, uuids.isEmpty() ? List.of(NO_MATCH) : uuids));
        }
        if (excluded != Group.PRICE && query.selects(Group.PRICE)) {
            parts.add(SearchFilter.eq("is_free", query.prices().getFirst() == Price.FREE));
        }
        return SearchFilter.and(parts);
    }

    // ===== Facets =====

    /**
     * Runs the facet queries in one multi-search. Per index: the base query (every filter, facets for
     * the groups without a selection) and one query per selected group (every other filter, that
     * group's facet).
     */
    private Facets facets(Query query, Map<Level, Set<UUID>> levelUuids) {
        List<SearchRequest> requests = new ArrayList<>();
        List<FacetSlot> slots = new ArrayList<>();
        for (Index index : Index.values()) {
            List<String> baseFacets = new ArrayList<>();
            for (Group group : Group.values()) {
                if (!query.selects(group)) {
                    baseFacets.add(index.attribute(group));
                }
            }
            requests.add(new SearchRequest(index.name, query.q(), filter(index, query, levelUuids, null), index.scope,
                    List.of(), 0, 1, baseFacets, null));
            slots.add(new FacetSlot(index, null));
            for (Group group : Group.values()) {
                if (query.selects(group)) {
                    requests.add(new SearchRequest(index.name, query.q(), filter(index, query, levelUuids, group),
                            index.scope, List.of(), 0, 1, List.of(index.attribute(group)), null));
                    slots.add(new FacetSlot(index, group));
                }
            }
        }
        List<SearchPage> pages = gateway.multiSearchPerIndex(requests);

        Facets facets = new Facets();
        for (int i = 0; i < slots.size(); i++) {
            FacetSlot slot = slots.get(i);
            SearchPage page = pages.get(i);
            if (slot.group() == null) {
                facets.totals.put(slot.index(), page.totalHits());
            }
            if (!query.show().includes(slot.index())) {
                continue;
            }
            for (Group group : Group.values()) {
                boolean fromThisQuery = slot.group() == null ? !query.selects(group) : slot.group() == group;
                if (!fromThisQuery) {
                    continue;
                }
                Map<String, Long> distribution = page.facetDistribution()
                        .getOrDefault(slot.index().attribute(group), Map.of());
                Map<String, Long> merged = facets.counts.computeIfAbsent(group, key -> new HashMap<>());
                distribution.forEach((value, count) -> merged.merge(value, count, Long::sum));
            }
        }
        return facets;
    }

    private record FacetSlot(Index index, Group group) {
    }

    /** Raw facet results: per-index totals and, per group, counts by stored value summed over shown indexes. */
    private static final class Facets {
        final Map<Index, Long> totals = new EnumMap<>(Index.class);
        final Map<Group, Map<String, Long>> counts = new EnumMap<>(Group.class);

        Map<String, Long> of(Group group) {
            return counts.getOrDefault(group, Map.of());
        }

        CatalogueFacets toDto(Query query, Map<Level, Set<UUID>> levelUuids, Map<UUID, String> categoryNames) {
            long courses = totals.getOrDefault(Index.COURSES, 0L);
            long programmes = totals.getOrDefault(Index.PROGRAMMES, 0L);

            Map<UUID, Long> byCategory = new LinkedHashMap<>();
            of(Group.CATEGORY).forEach((value, count) -> {
                UUID uuid = parseUuid(value);
                if (uuid != null && count > 0) {
                    byCategory.merge(uuid, count, Long::sum);
                }
            });
            query.categoryUuids().forEach(uuid -> byCategory.putIfAbsent(uuid, 0L));
            List<CatalogueCategoryFacet> categories = byCategory.entrySet().stream()
                    .filter(entry -> categoryNames.containsKey(entry.getKey()))
                    .map(entry -> new CatalogueCategoryFacet(entry.getKey(), categoryNames.get(entry.getKey()),
                            entry.getValue()))
                    .sorted(Comparator.comparingLong(CatalogueCategoryFacet::count).reversed()
                            .thenComparing(CatalogueCategoryFacet::name, String.CASE_INSENSITIVE_ORDER))
                    .toList();

            Map<String, Long> levels = of(Group.LEVEL);
            long[] perLevel = new long[Level.values().length];
            for (Level level : Level.values()) {
                for (UUID uuid : levelUuids.getOrDefault(level, Set.of())) {
                    perLevel[level.ordinal()] += levels.getOrDefault(uuid.toString(), 0L);
                }
            }

            Map<String, Long> prices = of(Group.PRICE);
            return new CatalogueFacets(
                    new CatalogueShowFacet(courses + programmes, courses, programmes),
                    categories,
                    new CatalogueLevelFacet(perLevel[Level.BEGINNER.ordinal()],
                            perLevel[Level.INTERMEDIATE.ordinal()], perLevel[Level.ADVANCED.ordinal()]),
                    new CataloguePriceFacet(prices.getOrDefault("true", 0L), prices.getOrDefault("false", 0L)));
        }
    }

    private Map<UUID, String> categoryNames(Facets facets, Query query) {
        Set<UUID> uuids = new LinkedHashSet<>(query.categoryUuids());
        facets.of(Group.CATEGORY).keySet().forEach(value -> {
            UUID uuid = parseUuid(value);
            if (uuid != null) {
                uuids.add(uuid);
            }
        });
        if (uuids.isEmpty()) {
            return Map.of();
        }
        Map<UUID, String> names = new HashMap<>();
        jdbc.query("SELECT uuid, name FROM course_categories WHERE uuid IN (:uuids)",
                new MapSqlParameterSource("uuids", uuids),
                rs -> {
                    names.put(SearchRows.uuid(rs, "uuid"), rs.getString("name"));
                });
        return names;
    }

    /** Difficulty-level UUIDs per catalogue level, matched by name case-insensitively. */
    private Map<Level, Set<UUID>> levelUuids() {
        Map<Level, Set<UUID>> byLevel = new EnumMap<>(Level.class);
        jdbc.query("SELECT uuid, name FROM course_difficulty_levels", Map.of(), rs -> {
            String name = rs.getString("name");
            if (name == null) {
                return;
            }
            for (Level level : Level.values()) {
                if (level.name().equalsIgnoreCase(name.trim())) {
                    byLevel.computeIfAbsent(level, key -> new LinkedHashSet<>()).add(SearchRows.uuid(rs, "uuid"));
                }
            }
        });
        return byLevel;
    }

    // ===== Hydration =====

    private List<CatalogueItem> hydrate(List<Hit> hits, boolean hasText) {
        List<UUID> courseUuids = hits.stream().filter(hit -> hit.index() == Index.COURSES && hit.uuid() != null)
                .map(Hit::uuid).toList();
        List<UUID> programUuids = hits.stream().filter(hit -> hit.index() == Index.PROGRAMMES && hit.uuid() != null)
                .map(Hit::uuid).toList();
        Map<UUID, long[]> courseCounts = courseCounts(courseUuids);
        Map<UUID, long[]> programCounts = programCounts(programUuids);
        Map<UUID, Long> classCounts = courseCounts.isEmpty()
                ? Map.of() : classDefinitionLookupService.countActivePublicClassesByCourse(courseCounts.keySet());

        List<CatalogueItem> items = new ArrayList<>(hits.size());
        for (Hit hit : hits) {
            if (hit.index() == Index.COURSES) {
                long[] counts = courseCounts.get(hit.uuid());
                if (counts != null) {
                    items.add(courseItem(hit, counts, classCounts.getOrDefault(hit.uuid(), 0L), hasText));
                }
            } else {
                long[] counts = programCounts.get(hit.uuid());
                if (counts != null) {
                    items.add(programmeItem(hit, counts, hasText));
                }
            }
        }
        return items;
    }

    /**
     * The public-visibility re-check for courses (root, published, admin-approved, active - the rule
     * behind the document's {@code is_public}) with the live lesson and learner counts, in one query.
     * Value: {@code [lesson_count, learner_count]}.
     */
    private Map<UUID, long[]> courseCounts(Collection<UUID> uuids) {
        if (uuids.isEmpty()) {
            return Map.of();
        }
        Map<UUID, long[]> counts = new HashMap<>();
        jdbc.query("""
                SELECT c.uuid,
                       (SELECT COUNT(*) FROM lessons l
                        WHERE l.course_uuid = c.uuid AND LOWER(l.status) = 'published') AS lesson_count,
                       (SELECT COUNT(DISTINCT e.student_uuid) FROM course_enrollments e
                        WHERE e.course_uuid = c.uuid AND LOWER(e.status) IN ('active', 'completed')) AS learner_count
                FROM courses c
                WHERE c.uuid IN (:uuids)
                  AND c.parent_course_uuid IS NULL
                  AND LOWER(c.status) = 'published' AND c.admin_approved = true AND c.active = true
                """, new MapSqlParameterSource("uuids", uuids), rs -> {
            counts.put(SearchRows.uuid(rs, "uuid"), new long[]{rs.getLong("lesson_count"), rs.getLong("learner_count")});
        });
        return counts;
    }

    /**
     * The public-visibility re-check for programmes (published, admin-approved, active) with the live
     * course, lesson and learner counts, in one query. Value: {@code [course_count, lesson_count,
     * learner_count]}.
     */
    private Map<UUID, long[]> programCounts(Collection<UUID> uuids) {
        if (uuids.isEmpty()) {
            return Map.of();
        }
        Map<UUID, long[]> counts = new HashMap<>();
        jdbc.query("""
                SELECT p.uuid,
                       (SELECT COUNT(*) FROM program_courses pc WHERE pc.program_uuid = p.uuid) AS course_count,
                       (SELECT COUNT(*) FROM program_courses pc
                        JOIN lessons l ON l.course_uuid = pc.course_uuid AND LOWER(l.status) = 'published'
                        WHERE pc.program_uuid = p.uuid) AS lesson_count,
                       (SELECT COUNT(DISTINCT pe.student_uuid) FROM program_enrollments pe
                        WHERE pe.program_uuid = p.uuid AND LOWER(pe.status) IN ('active', 'completed')) AS learner_count
                FROM training_programs p
                WHERE p.uuid IN (:uuids)
                  AND LOWER(p.status) = 'published' AND p.admin_approved = true AND p.is_active = true
                """, new MapSqlParameterSource("uuids", uuids), rs -> {
            counts.put(SearchRows.uuid(rs, "uuid"), new long[]{
                    rs.getLong("course_count"), rs.getLong("lesson_count"), rs.getLong("learner_count")});
        });
        return counts;
    }

    private static CatalogueItem courseItem(Hit hit, long[] counts, long classCount, boolean hasText) {
        Map<String, Object> document = hit.document();
        Integer ageLowerLimit = integer(document.get("age_lower_limit"));
        return new CatalogueItem(
                TYPE_COURSE,
                hit.uuid(),
                text(document, "name"),
                plainText(text(document, "description")),
                FileUrlResolver.publicUrl(text(document, "thumbnail_url")),
                texts(document.get("category_names")),
                uuids(document.get("category_uuids")),
                parseUuid(text(document, "course_creator_uuid")),
                text(document, "creator_name"),
                text(document, "difficulty_name"),
                decimal(document.get("rating_avg")) == null ? null : decimal(document.get("rating_avg")).doubleValue(),
                longValue(document.get("review_count")),
                counts[0],
                null,
                counts[1],
                classCount,
                ageLowerLimit != null && ageLowerLimit >= 18 ? ageLowerLimit + "+" : null,
                decimal(document.get("price")),
                Boolean.TRUE.equals(document.get("is_free")),
                hasText ? highlight(hit.formatted(), "name") : null);
    }

    private static CatalogueItem programmeItem(Hit hit, long[] counts, boolean hasText) {
        Map<String, Object> document = hit.document();
        String levelMin = text(document, "level_min");
        String levelMax = text(document, "level_max");
        String level = levelMin == null ? levelMax
                : levelMax == null || levelMin.equals(levelMax) ? levelMin : levelMin + " → " + levelMax;
        List<String> categoryNames = texts(document.get("category_names"));
        List<UUID> categoryUuids = uuids(document.get("category_uuids"));
        if (categoryUuids.isEmpty() && text(document, "category_uuid") != null) {
            // A schema-1 document, until the rebuild replaces it.
            categoryUuids = List.of(parseUuid(text(document, "category_uuid")));
            categoryNames = text(document, "category_name") == null ? List.of() : List.of(text(document, "category_name"));
        }
        return new CatalogueItem(
                TYPE_PROGRAMME,
                hit.uuid(),
                text(document, "title"),
                plainText(text(document, "description")),
                FileUrlResolver.publicUrl(text(document, "thumbnail_url")),
                categoryNames,
                categoryUuids,
                parseUuid(text(document, "course_creator_uuid")),
                text(document, "creator_name"),
                level,
                decimal(document.get("rating_avg")) == null ? null : decimal(document.get("rating_avg")).doubleValue(),
                longValue(document.get("review_count")),
                counts[1],
                counts[0],
                counts[2],
                null,
                null,
                decimal(document.get("price")),
                Boolean.TRUE.equals(document.get("is_free")),
                hasText ? highlight(hit.formatted(), "title") : null);
    }

    /**
     * The engine's highlighted title, HTML-escaped so the only markup left is its {@code <em>} tags;
     * {@code null} when nothing in the title matched.
     */
    static String highlight(Map<String, Object> formatted, String field) {
        String value = formatted == null ? null : text(formatted, field);
        if (value == null || !value.contains("<em>")) {
            return null;
        }
        return HtmlUtils.htmlEscape(value)
                .replace("&lt;em&gt;", "<em>")
                .replace("&lt;/em&gt;", "</em>");
    }

    // ===== Document readers =====

    /** Descriptions are stored as rich text; cards need a plain-text summary. */
    static String plainText(String html) {
        if (html == null) {
            return null;
        }
        String withoutBlocks = html.replaceAll("(?i)</?(p|div|h[1-6]|li|ul|ol|br|blockquote)[^>]*>", " ");
        String withoutTags = withoutBlocks.replaceAll("<[^>]*>", "");
        String text = HtmlUtils.htmlUnescape(withoutTags).replace('\u00A0', ' ').replaceAll("\\s+", " ").trim();
        return text.isEmpty() ? null : text;
    }

    private static String text(Map<String, Object> document, String key) {
        Object value = document.get(key);
        return value == null ? null : value.toString();
    }

    private static List<String> texts(Object value) {
        if (!(value instanceof Collection<?> values)) {
            return List.of();
        }
        return values.stream().filter(java.util.Objects::nonNull).map(Object::toString).toList();
    }

    private static List<UUID> uuids(Object value) {
        return texts(value).stream().map(CatalogueSearchService::parseUuid).filter(java.util.Objects::nonNull).toList();
    }

    private static UUID parseUuid(String value) {
        if (value == null) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static BigDecimal decimal(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static Integer integer(Object value) {
        BigDecimal number = decimal(value);
        return number == null ? null : number.intValue();
    }

    private static long longValue(Object value) {
        BigDecimal number = decimal(value);
        return number == null ? 0L : number.longValue();
    }

    // ===== Parameter parsing =====

    /** Parses a repeatable, comma-separable parameter into enum values, case-insensitively (400 otherwise). */
    public static <E extends Enum<E>> List<E> parseEnums(String name, List<String> raw, Class<E> type) {
        List<E> values = new ArrayList<>();
        for (String token : tokens(raw)) {
            values.add(parseEnum(name, token, type));
        }
        return values;
    }

    public static <E extends Enum<E>> E parseEnum(String name, String raw, Class<E> type) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String value = raw.trim().toUpperCase(Locale.ROOT);
        for (E constant : type.getEnumConstants()) {
            if (constant.name().equals(value)) {
                return constant;
            }
        }
        throw new IllegalArgumentException("Unknown " + name + " '" + raw.trim() + "'; expected one of "
                + java.util.Arrays.stream(type.getEnumConstants())
                .map(constant -> constant.name().toLowerCase(Locale.ROOT)).toList());
    }

    public static List<UUID> parseUuids(String name, List<String> raw) {
        List<UUID> values = new ArrayList<>();
        for (String token : tokens(raw)) {
            values.add(parseUuidParam(name, token));
        }
        return values;
    }

    public static UUID parseUuidParam(String name, String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return UUID.fromString(raw.trim());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(name + " must be a UUID");
        }
    }

    private static List<String> tokens(List<String> raw) {
        if (raw == null) {
            return List.of();
        }
        List<String> tokens = new ArrayList<>();
        for (String value : raw) {
            if (value == null) {
                continue;
            }
            for (String token : value.split(",")) {
                if (StringUtils.hasText(token)) {
                    tokens.add(token.trim());
                }
            }
        }
        return tokens;
    }
}
