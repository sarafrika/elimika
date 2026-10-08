package apps.sarafrika.elimika.perf.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "RumIngestResponse")
public record RumIngestResponse(
        @JsonProperty("accepted")
        @Schema(description = "Samples stored")
        int accepted,

        @JsonProperty("dropped")
        @Schema(description = "Samples ignored because their timestamp was outside the accepted window")
        int dropped
) {
}
