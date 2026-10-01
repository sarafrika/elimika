package apps.sarafrika.elimika.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/** Matches per result type, under every active filter except {@code show}. */
@Schema(name = "CatalogueShowFacet", description = "Matches per result type, ignoring the show selection.")
public record CatalogueShowFacet(
        @JsonProperty("all") long all,
        @JsonProperty("courses") long courses,
        @JsonProperty("programmes") long programmes
) {
}
