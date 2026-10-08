package apps.sarafrika.elimika.perf.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "RumSummaryRow", description = "Percentiles for one route template and metric")
public record RumSummaryRow(
        @JsonProperty("route_template")
        String routeTemplate,

        @JsonProperty("metric")
        String metric,

        @JsonProperty("samples")
        long samples,

        @JsonProperty("p50_ms")
        double p50Ms,

        @JsonProperty("p95_ms")
        double p95Ms,

        @JsonProperty("p99_ms")
        double p99Ms
) {
}
