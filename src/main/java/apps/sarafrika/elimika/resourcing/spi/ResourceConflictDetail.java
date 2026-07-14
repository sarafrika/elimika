package apps.sarafrika.elimika.resourcing.spi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * One requested occurrence window that cannot be satisfied on a resource, and why.
 */
@Schema(name = "ResourceConflictDetail", description = "A requested resource booking window that conflicts, and what it collides with")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ResourceConflictDetail(

        @Schema(description = "Resource the conflict occurred on")
        @JsonProperty("resource_uuid")
        UUID resourceUuid,

        @Schema(description = "Human readable resource name")
        @JsonProperty("resource_name")
        String resourceName,

        @Schema(description = "Requested window start (UTC)")
        @JsonProperty("requested_start")
        LocalDateTime requestedStart,

        @Schema(description = "Requested window end (UTC)")
        @JsonProperty("requested_end")
        LocalDateTime requestedEnd,

        @Schema(description = "Conflict classification")
        @JsonProperty("conflict_type")
        ResourceConflictType conflictType,

        @Schema(description = "UUID of the booking collided with, when applicable")
        @JsonProperty("conflicting_booking_uuid")
        UUID conflictingBookingUuid,

        @Schema(description = "UUID of the marketplace job holding the colliding reservation, when applicable")
        @JsonProperty("conflicting_job_uuid")
        UUID conflictingJobUuid,

        @Schema(description = "Human readable description of the conflict")
        @JsonProperty("description")
        String description
) {
}
