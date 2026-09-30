package apps.sarafrika.elimika.course.internal.recommend;

import apps.sarafrika.elimika.course.internal.recommend.CourseCandidateStore.Audience;
import apps.sarafrika.elimika.course.internal.search.CourseSearchSource;
import apps.sarafrika.elimika.shared.search.SearchAvailability;
import apps.sarafrika.elimika.shared.search.SearchFilter;
import apps.sarafrika.elimika.shared.search.SearchGateway;
import apps.sarafrika.elimika.shared.search.SearchHit;
import apps.sarafrika.elimika.shared.search.SearchPage;
import apps.sarafrika.elimika.shared.search.SearchRequest;
import apps.sarafrika.elimika.shared.search.SearchScope;
import apps.sarafrika.elimika.shared.search.SearchSort;
import apps.sarafrika.elimika.shared.search.SearchUnavailableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Candidate retrieval: one multi-search over the public {@code courses} index (one request per candidate
 * set, all sharing the audience scope and the exclusions), then one SQL load of the union. When search is
 * off or fails, the same set filters run as one relational query instead; that fallback is allowed here
 * because none of it is text search ("more like this" is simply skipped).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CourseCandidateRetriever {

    static final int SET_SIZE = 30;
    static final int POPULAR_SIZE = 20;
    static final int MORE_LIKE_THIS_SIZE = 20;
    static final int FALLBACK_LIMIT = 150;
    /** Longest id list sent in one filter; search filters are strings, so keep them short. */
    static final int MAX_FILTER_IDS = 100;

    private static final String IS_PUBLIC = "is_public";
    private static final String UUID_ATTRIBUTE = "uuid";
    private static final String CATEGORY_UUIDS = "category_uuids";
    private static final String SKILL_UUIDS = "skill_uuids";
    private static final String PREREQUISITE_UUIDS = "prerequisite_uuids";
    private static final String AGE_LOWER = "age_lower_limit";
    private static final String AGE_UPPER = "age_upper_limit";

    private final SearchGateway searchGateway;
    private final SearchAvailability searchAvailability;
    private final CourseCandidateStore store;

    /**
     * What to retrieve.
     *
     * @param categoryUuids   set a: courses in these categories, best rated first
     * @param neighbourUuids  set b: co-enrolment neighbours
     * @param affiliatedUuids set c: courses offered by affiliated organisations or instructors
     * @param moreLikeThis    set d: text of a course to find similar ones to, or {@code null}
     * @param skillUuids      set e: courses teaching these skills
     * @param followOnOf      courses that list any of these as a prerequisite (the learner's next steps)
     */
    public record CandidateQuery(Collection<UUID> categoryUuids, Collection<UUID> neighbourUuids,
                                 Collection<UUID> affiliatedUuids, String moreLikeThis, Collection<UUID> skillUuids,
                                 Collection<UUID> followOnOf) {
    }

    /** Hydrated candidates plus the search relevance of "more like this" hits (empty on the fallback). */
    public record Retrieval(List<CandidateCourse> candidates, Map<UUID, Double> textSimilarity, boolean fromSearch) {
    }

    public Retrieval retrieve(CandidateQuery query, Audience audience, Collection<UUID> excluded) {
        if (searchAvailability.isReadEnabled(CourseSearchSource.INDEX)) {
            try {
                return fromSearch(query, audience, excluded);
            } catch (SearchUnavailableException ex) {
                log.info("Course recommendations falling back to SQL: {}", ex.getMessage());
            }
        }
        Set<UUID> courseUuids = new LinkedHashSet<>();
        courseUuids.addAll(nullSafe(query.neighbourUuids()));
        courseUuids.addAll(nullSafe(query.affiliatedUuids()));
        List<CandidateCourse> candidates = store.findRelationalCandidates(nullSafe(query.categoryUuids()), courseUuids,
                nullSafe(query.skillUuids()), nullSafe(query.followOnOf()), audience, excluded, FALLBACK_LIMIT);
        return new Retrieval(candidates, Map.of(), false);
    }

    private Retrieval fromSearch(CandidateQuery query, Audience audience, Collection<UUID> excluded) {
        SearchScope scope = SearchScope.of(SearchFilter.and(SearchFilter.eq(IS_PUBLIC, true), audienceFilter(audience)),
                "course-recommendations");
        SearchFilter exclusion = excluded == null || excluded.isEmpty() ? null
                : SearchFilter.notIn(UUID_ATTRIBUTE, limited(excluded));

        List<SearchRequest> requests = new ArrayList<>();
        int moreLikeThisIndex = -1;
        if (!nullSafe(query.categoryUuids()).isEmpty()) {
            requests.add(request(null, SearchFilter.in(CATEGORY_UUIDS, limited(query.categoryUuids())), exclusion, scope,
                    List.of(SearchSort.desc("rating_bayes")), SET_SIZE));
        }
        if (!nullSafe(query.neighbourUuids()).isEmpty()) {
            requests.add(request(null, SearchFilter.in(UUID_ATTRIBUTE, limited(query.neighbourUuids())), exclusion, scope,
                    List.of(), SET_SIZE));
        }
        if (!nullSafe(query.affiliatedUuids()).isEmpty()) {
            requests.add(request(null, SearchFilter.in(UUID_ATTRIBUTE, limited(query.affiliatedUuids())), exclusion, scope,
                    List.of(), SET_SIZE));
        }
        if (!nullSafe(query.skillUuids()).isEmpty()) {
            requests.add(request(null, SearchFilter.in(SKILL_UUIDS, limited(query.skillUuids())), exclusion, scope,
                    List.of(), SET_SIZE));
        }
        if (!nullSafe(query.followOnOf()).isEmpty()) {
            requests.add(request(null, SearchFilter.in(PREREQUISITE_UUIDS, limited(query.followOnOf())), exclusion, scope,
                    List.of(), SET_SIZE));
        }
        if (query.moreLikeThis() != null && !query.moreLikeThis().isBlank()) {
            moreLikeThisIndex = requests.size();
            requests.add(request(query.moreLikeThis(), null, exclusion, scope, List.of(), MORE_LIKE_THIS_SIZE)
                    .withMatchingStrategy(SearchRequest.MatchingStrategy.LAST));
        }
        // Always: the genuinely popular courses, for cold start, popularity and the exploration slot.
        requests.add(request(null, null, exclusion, scope,
                List.of(SearchSort.desc("popularity_30d"), SearchSort.desc("rating_bayes")), POPULAR_SIZE));

        List<SearchPage> pages = searchGateway.multiSearchPerIndex(requests);
        Set<UUID> uuids = new LinkedHashSet<>();
        Map<UUID, Double> textSimilarity = new HashMap<>();
        for (int i = 0; i < pages.size(); i++) {
            for (SearchHit hit : pages.get(i).hits()) {
                uuids.add(hit.uuid());
                if (i == moreLikeThisIndex && hit.rankingScore() != null) {
                    textSimilarity.merge(hit.uuid(), hit.rankingScore(), Math::max);
                }
            }
        }
        return new Retrieval(store.loadPublic(uuids, audience, excluded), textSimilarity, true);
    }

    private static SearchRequest request(String text, SearchFilter setFilter, SearchFilter exclusion, SearchScope scope,
                                         List<SearchSort> sort, int size) {
        return new SearchRequest(CourseSearchSource.INDEX, text, SearchFilter.and(setFilter, exclusion), scope, sort,
                0, size, List.of(), null);
    }

    /**
     * The audience as a filter. {@code NOT x > age} also keeps documents without the attribute, so courses
     * with no limit stay in.
     */
    static SearchFilter audienceFilter(Audience audience) {
        if (audience == null || !audience.restricted()) {
            return null;
        }
        if (audience.age() == null) {
            return SearchFilter.and(SearchFilter.not(SearchFilter.gte(AGE_LOWER, 0)),
                    SearchFilter.not(SearchFilter.gte(AGE_UPPER, 0)));
        }
        return SearchFilter.and(SearchFilter.not(SearchFilter.gt(AGE_LOWER, audience.age())),
                SearchFilter.not(SearchFilter.lt(AGE_UPPER, audience.age())));
    }

    private static List<UUID> limited(Collection<UUID> uuids) {
        return uuids.stream().distinct().limit(MAX_FILTER_IDS).toList();
    }

    private static <T> Collection<T> nullSafe(Collection<T> values) {
        return values == null ? List.of() : values;
    }
}
