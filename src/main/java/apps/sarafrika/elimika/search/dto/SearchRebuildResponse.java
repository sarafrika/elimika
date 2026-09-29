package apps.sarafrika.elimika.search.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * What an admin rebuild or sync request queued.
 *
 * @param queued  the indexes (or documents) accepted for background work
 * @param skipped the indexes already queued or running
 */
public record SearchRebuildResponse(
        @JsonProperty("queued") List<String> queued,
        @JsonProperty("skipped") List<String> skipped
) {
}
