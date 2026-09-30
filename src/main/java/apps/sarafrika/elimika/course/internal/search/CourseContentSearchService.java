package apps.sarafrika.elimika.course.internal.search;

import apps.sarafrika.elimika.shared.search.GlobalSearchHit;
import apps.sarafrika.elimika.shared.search.SearchAvailability;
import apps.sarafrika.elimika.shared.search.SearchFilter;
import apps.sarafrika.elimika.shared.search.SearchGateway;
import apps.sarafrika.elimika.shared.search.SearchHit;
import apps.sarafrika.elimika.shared.search.SearchPage;
import apps.sarafrika.elimika.shared.search.SearchRequest;
import apps.sarafrika.elimika.shared.search.SearchResults;
import apps.sarafrika.elimika.shared.search.SearchScope;
import apps.sarafrika.elimika.shared.search.SearchUnavailableException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Searches one course's lessons, content items, quizzes and assignments.
 * <p>
 * The endpoint has already decided the caller may read the course. Here the caller's entitlement
 * ({@link CourseContentEntitlement}) is the scope, ANDed with the course and the requested types, and
 * every hit is re-checked in SQL before it is returned. Free text is served only by the index: search
 * off, the {@code course_content} read flag off, or the engine failing is a
 * {@link SearchUnavailableException} (503 "Search is unavailable").
 */
@Service
public class CourseContentSearchService {

    public static final int MAX_QUERY_LENGTH = 200;
    public static final int DEFAULT_PAGE_SIZE = 20;

    private static final int SNIPPET_BEFORE = 60;
    private static final int SNIPPET_LENGTH = 200;
    private static final String MARK = "<em>";
    private static final String END_MARK = "</em>";

    private final SearchAvailability availability;
    private final SearchGateway gateway;
    private final CourseContentEntitlement entitlement;

    public CourseContentSearchService(SearchAvailability availability, SearchGateway gateway,
                                      CourseContentEntitlement entitlement) {
        this.availability = availability;
        this.gateway = gateway;
        this.entitlement = entitlement;
    }

    /**
     * @throws IllegalArgumentException   for a missing or long {@code q}, an unknown type or a bad page (400)
     * @throws SearchUnavailableException when search or the index's reads are off, or the engine fails (503)
     */
    public Page<CourseContentSearchHitDTO> search(UUID courseUuid, String q, String types, Integer page, Integer size) {
        String text = q == null ? "" : q.trim();
        if (text.isEmpty()) {
            throw new IllegalArgumentException("q is required");
        }
        if (text.length() > MAX_QUERY_LENGTH) {
            throw new IllegalArgumentException("q must be at most " + MAX_QUERY_LENGTH + " characters");
        }
        int pageNumber = page == null ? 0 : page;
        int pageSize = size == null ? DEFAULT_PAGE_SIZE : size;
        if (pageNumber < 0) {
            throw new IllegalArgumentException("page must be 0 or greater");
        }
        if (pageSize < 1 || pageSize > SearchRequest.MAX_SIZE) {
            throw new IllegalArgumentException("size must be between 1 and " + SearchRequest.MAX_SIZE);
        }
        Set<String> wanted = parseTypes(types);
        if (!availability.isReadEnabled(CourseContentSearchSource.INDEX)) {
            throw new SearchUnavailableException("Search is not enabled for " + CourseContentSearchSource.INDEX);
        }
        PageRequest pageable = PageRequest.of(pageNumber, pageSize);
        Optional<SearchScope> scope = entitlement.scope();
        if (scope.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, 0);
        }
        SearchFilter filter = SearchFilter.and(
                SearchFilter.eq(CourseContentSearchSource.COURSE_UUID, courseUuid),
                wanted.isEmpty() ? null : SearchFilter.in(CourseContentSearchSource.TYPE, wanted));
        SearchPage result = gateway.search(new SearchRequest(CourseContentSearchSource.INDEX, text, filter, scope.get(),
                List.of(), pageNumber, pageSize, List.of(), null));

        List<CourseContentSearchHitDTO> hits = result.hits().stream()
                .filter(hit -> hit.uuid() != null)
                .map(CourseContentSearchService::toDto)
                .toList();
        List<CourseContentSearchHitDTO> shown = entitlement.retainVisible(hits, CourseContentSearchHitDTO::uuid);
        return new PageImpl<>(shown, pageable, SearchResults.total(result.totalHits(), hits.size(), shown.size()));
    }

    private static CourseContentSearchHitDTO toDto(SearchHit hit) {
        Map<String, Object> document = hit.document();
        return new CourseContentSearchHitDTO(
                GlobalSearchHit.text(document, CourseContentSearchSource.TYPE),
                hit.uuid(),
                uuid(GlobalSearchHit.text(document, "lesson_uuid")),
                integer(document == null ? null : document.get("lesson_number")),
                GlobalSearchHit.text(document, "lesson_title"),
                GlobalSearchHit.text(document, "title"),
                highlight(hit));
    }

    /** Comma-separated item types; blank means all of them. */
    static Set<String> parseTypes(String types) {
        Set<String> parsed = new LinkedHashSet<>();
        if (types == null || types.isBlank()) {
            return parsed;
        }
        for (String raw : types.split(",")) {
            String type = raw.trim().toLowerCase(Locale.ROOT);
            if (type.isEmpty()) {
                continue;
            }
            if (!CourseContentSearchSource.TYPES.contains(type)) {
                throw new IllegalArgumentException("Unknown content type: " + type.replaceAll("[^a-z0-9_]", "")
                        + ". Known types: " + String.join(", ", CourseContentSearchSource.TYPES));
            }
            parsed.add(type);
        }
        return parsed;
    }

    /**
     * A short excerpt around the first match in the title or body. The body can run to
     * a couple of thousand characters, so it is cut to a window around the first marker, on word
     * boundaries, with the markers kept balanced.
     */
    static String highlight(SearchHit hit) {
        String formatted = GlobalSearchHit.highlight(hit, "title", "body");
        if (formatted == null || formatted.length() <= SNIPPET_LENGTH) {
            return formatted;
        }
        int mark = formatted.indexOf(MARK);
        int start = Math.max(0, mark - SNIPPET_BEFORE);
        if (start > 0) {
            int space = formatted.lastIndexOf(' ', start);
            start = space < 0 ? 0 : space + 1;
        }
        int end = Math.min(formatted.length(), mark + SNIPPET_LENGTH);
        if (end < formatted.length()) {
            int space = formatted.lastIndexOf(' ', end);
            if (space > mark) {
                end = space;
            }
        }
        String snippet = formatted.substring(start, end);
        int lastOpen = snippet.lastIndexOf('<');
        if (lastOpen > snippet.lastIndexOf('>')) {
            snippet = snippet.substring(0, lastOpen);
        }
        if (count(snippet, MARK) > count(snippet, END_MARK)) {
            snippet = snippet + END_MARK;
        }
        return (start > 0 ? "…" : "") + snippet + (end < formatted.length() ? "…" : "");
    }

    private static int count(String text, String token) {
        int count = 0;
        for (int at = text.indexOf(token); at >= 0; at = text.indexOf(token, at + token.length())) {
            count++;
        }
        return count;
    }

    private static UUID uuid(String value) {
        if (value == null) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static Integer integer(Object value) {
        return value instanceof Number number ? number.intValue() : null;
    }
}
