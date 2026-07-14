package apps.sarafrika.elimika.resourcing.spi;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Outcome of validating a set of resource booking requests.
 */
@Schema(name = "ResourceValidationReport", description = "Per-occurrence conflict report for a set of requested resource bookings")
public record ResourceValidationReport(

        @Schema(description = "True when every requested window can be satisfied")
        @JsonProperty("clean")
        boolean clean,

        @Schema(description = "Every conflicting window with details of what it collides with")
        @JsonProperty("conflicts")
        List<ResourceConflictDetail> conflicts
) {

    public static ResourceValidationReport empty() {
        return new ResourceValidationReport(true, List.of());
    }

    public static ResourceValidationReport withConflicts(List<ResourceConflictDetail> conflicts) {
        return new ResourceValidationReport(conflicts == null || conflicts.isEmpty(), conflicts == null ? List.of() : conflicts);
    }
}
