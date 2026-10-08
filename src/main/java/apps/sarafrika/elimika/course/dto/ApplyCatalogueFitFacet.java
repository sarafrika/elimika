package apps.sarafrika.elimika.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/** How many results each fit tab would show, under every other active filter. */
@Schema(name = "ApplyCatalogueFitFacet", description = "Result counts per fit value.")
public record ApplyCatalogueFitFacet(
        @Schema(description = "Not yet applied to") @JsonProperty("open") long open,
        @Schema(description = "Not yet applied to and sharing a skill with the caller's wallet")
        @JsonProperty("skills") long skills,
        @Schema(description = "Already applied to, whatever the status") @JsonProperty("applied") long applied
) {
}
