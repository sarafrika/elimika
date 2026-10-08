package apps.sarafrika.elimika.perf.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

@Schema(name = "RumEventRequest", description = "One real-user performance sample")
public record RumEventRequest(
        @JsonProperty("route_template")
        @Schema(description = "Route pattern with ids replaced, e.g. /dashboard/courses/[id]", example = "/dashboard/overview")
        @NotBlank @Size(max = 255)
        String routeTemplate,

        @JsonProperty("domain")
        @Schema(description = "Dashboard domain of the viewer", example = "student")
        @Size(max = 64)
        String domain,

        @JsonProperty("metric")
        @Schema(description = "Metric name, e.g. LCP, INP, TTFB, time_to_data", example = "LCP")
        @NotBlank @Size(max = 64) @Pattern(regexp = "^[A-Za-z0-9_.:-]+$")
        String metric,

        @JsonProperty("value_ms")
        @Schema(description = "Measured duration in milliseconds", example = "1830.5")
        @NotNull @PositiveOrZero @DecimalMax("3600000")
        Double valueMs,

        @JsonProperty("section")
        @Schema(description = "Page section the sample belongs to, if any", example = "enrolments")
        @Size(max = 128)
        String section,

        @JsonProperty("network_type")
        @Schema(description = "Effective connection type", example = "4g")
        @Size(max = 32)
        String networkType,

        @JsonProperty("device_class")
        @Schema(description = "Coarse device class", example = "mobile")
        @Size(max = 32)
        String deviceClass,

        @JsonProperty("app_version")
        @Schema(description = "Web app build version", example = "1.42.0")
        @Size(max = 64)
        String appVersion,

        @JsonProperty("occurred_at")
        @Schema(description = "When the sample was taken (ISO-8601 with offset)")
        @NotNull
        OffsetDateTime occurredAt
) {
}
