package apps.sarafrika.elimika.search.dto;

import apps.sarafrika.elimika.shared.search.GlobalSearchHit;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * The global search box's answer.
 *
 * @param hits   up to {@code limit} hits per type, grouped by type in the order the types were
 *               requested, each group in relevance order
 * @param totals per searched type, how many documents the caller could page through for this query;
 *               types the caller may not see (or that are not enabled) are absent
 */
public record GlobalSearchResponse(
        @JsonProperty("hits") List<GlobalSearchHit> hits,
        @JsonProperty("totals") Map<String, Long> totals
) {
}
