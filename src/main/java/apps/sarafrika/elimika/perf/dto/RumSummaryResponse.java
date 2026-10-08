package apps.sarafrika.elimika.perf.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;

@Schema(name = "RumSummaryResponse")
public record RumSummaryResponse(
        @JsonProperty("from")
        OffsetDateTime from,

        @JsonProperty("to")
        OffsetDateTime to,

        @JsonProperty("rows")
        List<RumSummaryRow> rows
) {
}
