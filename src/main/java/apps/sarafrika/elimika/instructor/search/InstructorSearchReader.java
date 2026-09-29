package apps.sarafrika.elimika.instructor.search;

import apps.sarafrika.elimika.instructor.model.Instructor;
import apps.sarafrika.elimika.instructor.repository.InstructorRepository;
import apps.sarafrika.elimika.shared.search.SearchAvailability;
import apps.sarafrika.elimika.shared.search.SearchFilter;
import apps.sarafrika.elimika.shared.search.SearchGateway;
import apps.sarafrika.elimika.shared.search.SearchHit;
import apps.sarafrika.elimika.shared.search.SearchPage;
import apps.sarafrika.elimika.shared.search.SearchParamsTranslator;
import apps.sarafrika.elimika.shared.search.SearchRequest;
import apps.sarafrika.elimika.shared.search.SearchSort;
import apps.sarafrika.elimika.shared.search.SearchUnavailableException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Serves instructor list reads that carry a {@code q} from the {@code instructors} index, returning
 * the matching entities in rank order so the caller maps them exactly as it maps database rows.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InstructorSearchReader {

    private final SearchAvailability searchAvailability;
    private final SearchGateway searchGateway;
    private final InstructorRepository instructorRepository;

    /** Whether a read with this {@code q} should be routed to search. */
    public boolean handles(String q) {
        return StringUtils.hasText(q) && searchAvailability.isReadEnabled(InstructorSearchSource.INDEX);
    }

    /**
     * Searches the index and hydrates the hits with one batch query.
     *
     * @param searchParams the remaining request parameters, in the {@code searchParams} vocabulary;
     *                     {@code q} must already be removed
     * @return the page, or empty when the engine cannot answer and the caller should use the database
     * @throws IllegalArgumentException for a filter or sort the index does not allow (a 400)
     */
    public Optional<Page<Instructor>> search(String q, Map<String, String> searchParams, Pageable pageable,
                                             boolean platformAdmin) {
        SearchFilter filter = SearchParamsTranslator.toFilter(searchParams, InstructorSearchSource.DEFINITION);
        Sort sort = pageable.isPaged() ? pageable.getSort() : Sort.unsorted();
        List<SearchSort> sorts = SearchParamsTranslator.toSort(sortExpression(sort), InstructorSearchSource.DEFINITION);
        int page = pageable.isPaged() ? pageable.getPageNumber() : 0;
        int size = pageable.isPaged() ? Math.clamp(pageable.getPageSize(), 1, SearchRequest.MAX_SIZE) : 20;

        SearchPage result;
        try {
            result = searchGateway.search(new SearchRequest(InstructorSearchSource.INDEX, q.trim(), filter,
                    InstructorSearchScopes.forCaller(platformAdmin), sorts, page, size, List.of(), null));
        } catch (SearchUnavailableException ex) {
            log.warn("Instructor search unavailable, falling back to the database: {}", ex.getMessage());
            return Optional.empty();
        }

        List<UUID> uuids = result.hits().stream().map(SearchHit::uuid).filter(Objects::nonNull).toList();
        Map<UUID, Instructor> byUuid = uuids.isEmpty() ? Map.of()
                : instructorRepository.findByUuidIn(uuids).stream()
                        .collect(Collectors.toMap(Instructor::getUuid, Function.identity(), (a, b) -> a));
        // A hit whose row was deleted since it was indexed is dropped rather than returned empty.
        List<Instructor> ordered = uuids.stream().map(byUuid::get).filter(Objects::nonNull).toList();
        return Optional.of(new PageImpl<>(ordered, PageRequest.of(page, size, sort), result.totalHits()));
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
