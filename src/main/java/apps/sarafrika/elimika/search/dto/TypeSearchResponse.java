package apps.sarafrika.elimika.search.dto;

import apps.sarafrika.elimika.shared.search.GlobalSearchHit;
import apps.sarafrika.elimika.shared.utils.PageMetadata;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * One page of one type from global search ("see all results").
 *
 * @param content  the hits on this page, in relevance order unless sorted
 * @param metadata the usual page metadata
 * @param facets   per requested facet attribute, the count of matching documents for each value
 */
public record TypeSearchResponse(
        @JsonProperty("content") List<GlobalSearchHit> content,
        @JsonProperty("metadata") PageMetadata metadata,
        @JsonProperty("facets") Map<String, Map<String, Long>> facets
) {
}
