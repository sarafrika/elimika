package apps.sarafrika.elimika.instructor.search;

import apps.sarafrika.elimika.instructor.model.Instructor;
import apps.sarafrika.elimika.instructor.repository.InstructorRepository;
import apps.sarafrika.elimika.shared.search.NearMe;
import apps.sarafrika.elimika.shared.search.SearchAvailability;
import apps.sarafrika.elimika.shared.search.SearchFilter;
import apps.sarafrika.elimika.shared.search.SearchGateway;
import apps.sarafrika.elimika.shared.search.SearchPage;
import apps.sarafrika.elimika.shared.search.SearchParamsTranslator;
import apps.sarafrika.elimika.shared.search.SearchRequest;
import apps.sarafrika.elimika.shared.search.SearchResults;
import apps.sarafrika.elimika.shared.search.SearchSort;
import apps.sarafrika.elimika.shared.search.SearchUnavailableException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Serves instructor list reads that carry a {@code q} from the {@code instructors} index, returning
 * the matching entities in rank order so the caller maps them exactly as it maps database rows.
 * There is no database fallback for free text, nor for near-me ({@code near=lat,lng}).
 */
@Component
@RequiredArgsConstructor
public class InstructorSearchReader {

    private final SearchAvailability searchAvailability;
    private final SearchGateway searchGateway;
    private final InstructorRepository instructorRepository;
    private final InstructorVisibility instructorVisibility;

    /**
     * Searches the index and hydrates the hits with one batch query.
     *
     * @param searchParams the remaining request parameters, in the {@code searchParams} vocabulary;
     *                     {@code q} must already be removed
     * @return the page of matching instructors in rank order
     * @throws IllegalArgumentException   for a filter or sort the index does not allow (a 400)
     * @throws SearchUnavailableException when search or the index's reads are off, or the engine fails (a 503)
     */
    public Page<Instructor> search(String q, Map<String, String> searchParams, Pageable pageable,
                                   InstructorVisibility.Caller caller) {
        if (!StringUtils.hasText(q)) {
            throw new IllegalArgumentException("q must not be blank");
        }
        return run(q.trim(), searchParams, pageable, caller, null).page();
    }

    /** A page of near-me results with each row's distance band - never metres or coordinates. */
    public record NearMeResult(Page<Instructor> page, Map<UUID, String> distanceBands) {
    }

    /**
     * Near-me: the instructors within {@code near}'s radius, nearest first without {@code q} and by
     * relevance with it. Only documents with a {@code _geo} point can match, which the source sets
     * only for verified instructors who opted in; the visibility scope still applies on top.
     *
     * @throws SearchUnavailableException when search or the index's reads are off, or the engine fails (a 503)
     */
    public NearMeResult searchNear(String q, NearMe near, Map<String, String> searchParams, Pageable pageable,
                                   InstructorVisibility.Caller caller) {
        return run(StringUtils.hasText(q) ? q.trim() : null, searchParams, pageable, caller,
                Objects.requireNonNull(near, "near"));
    }

    private NearMeResult run(String text, Map<String, String> searchParams, Pageable pageable,
                             InstructorVisibility.Caller caller, NearMe near) {
        SearchFilter filter = SearchParamsTranslator.toFilter(searchParams, InstructorSearchSource.DEFINITION);
        Sort sort = pageable.isPaged() ? pageable.getSort() : Sort.unsorted();
        List<SearchSort> sorts = SearchParamsTranslator.toSort(sortExpression(sort), InstructorSearchSource.DEFINITION);
        if (near != null) {
            filter = SearchFilter.and(filter, near.filter());
            sorts = near.sorts(text != null, sorts);
        }
        int page = pageable.isPaged() ? pageable.getPageNumber() : 0;
        int size = pageable.isPaged() ? Math.clamp(pageable.getPageSize(), 1, SearchRequest.MAX_SIZE) : 20;

        if (!searchAvailability.isReadEnabled(InstructorSearchSource.INDEX)) {
            throw new SearchUnavailableException("Search is not enabled for " + InstructorSearchSource.INDEX);
        }
        SearchPage result = searchGateway.search(new SearchRequest(InstructorSearchSource.INDEX, text, filter,
                instructorVisibility.searchScope(caller, searchParams), sorts, page, size, List.of(), null));

        List<UUID> uuids = SearchResults.hitUuids(result);
        List<Instructor> rows = uuids.isEmpty() ? List.of() : hydrate(uuids, caller, searchParams);
        // A hit whose row was deleted, or that the database no longer lets the caller see (verification
        // withdrawn since it was indexed), is dropped rather than shown, and the total restated.
        List<Instructor> ordered = SearchResults.inHitOrder(uuids, rows, Instructor::getUuid);
        if (near != null) {
            // Likewise a near-me hit whose opt-in or verification lapsed after it was indexed.
            ordered = ordered.stream().filter(InstructorSearchSource::isLocatable).toList();
        }
        Page<Instructor> hydrated = new PageImpl<>(ordered, PageRequest.of(page, size, sort),
                SearchResults.total(result.totalHits(), result.hits().size(), ordered.size()));
        if (near == null) {
            return new NearMeResult(hydrated, Map.of());
        }
        // From the same rounded point the index holds: the engine never hands _geo (or, without it, a
        // distance) back.
        Map<UUID, String> bands = new HashMap<>();
        ordered.forEach(instructor -> {
            String band = near.distanceBand(InstructorSearchSource.nearMePoint(instructor));
            if (band != null) {
                bands.put(instructor.getUuid(), band);
            }
        });
        return new NearMeResult(hydrated, bands);
    }

    private List<Instructor> hydrate(List<UUID> uuids, InstructorVisibility.Caller caller, Map<String, String> params) {
        Specification<Instructor> byUuid = (root, query, cb) -> root.get("uuid").in(uuids);
        Specification<Instructor> visible = instructorVisibility.databaseScope(caller, params);
        return instructorRepository.findAll(visible == null ? byUuid : byUuid.and(visible));
    }

    private static String sortExpression(Sort sort) {
        if (sort == null || sort.isUnsorted()) {
            return null;
        }
        return sort.stream()
                .map(order -> order.getProperty() + "," + order.getDirection().name().toLowerCase(Locale.ROOT))
                .collect(Collectors.joining(","));
    }
}
