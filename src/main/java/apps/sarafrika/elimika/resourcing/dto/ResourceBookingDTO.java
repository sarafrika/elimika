package apps.sarafrika.elimika.resourcing.dto;

import apps.sarafrika.elimika.resourcing.spi.ResourceBookingSourceType;
import apps.sarafrika.elimika.resourcing.spi.ResourceBookingStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(
        name = "ResourceBooking",
        description = "Time-slot reservation of an organisation resource"
)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ResourceBookingDTO(

        @Schema(description = "**[READ-ONLY]** Unique identifier of the booking", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "uuid", access = JsonProperty.Access.READ_ONLY)
        UUID uuid,

        @Schema(description = "Resource booked")
        @JsonProperty("resource_uuid")
        UUID resourceUuid,

        @Schema(description = "Organisation owning the resource")
        @JsonProperty("organisation_uuid")
        UUID organisationUuid,

        @Schema(description = "Booking lifecycle state", example = "HOLD")
        @JsonProperty("status")
        ResourceBookingStatus status,

        @Schema(description = "Units reserved (1 for venues)", example = "1")
        @JsonProperty("quantity")
        Integer quantity,

        @Schema(description = "Reservation window start (UTC)")
        @JsonProperty("start_time")
        LocalDateTime startTime,

        @Schema(description = "Reservation window end (UTC)")
        @JsonProperty("end_time")
        LocalDateTime endTime,

        @Schema(description = "What created the booking", example = "MARKETPLACE_JOB")
        @JsonProperty("source_type")
        ResourceBookingSourceType sourceType,

        @Schema(description = "Marketplace job holding the reservation, when applicable", nullable = true)
        @JsonProperty("job_uuid")
        UUID jobUuid,

        @Schema(description = "Class definition backing the reservation, when applicable", nullable = true)
        @JsonProperty("class_definition_uuid")
        UUID classDefinitionUuid,

        @Schema(description = "Scheduled instance backing the reservation, when applicable", nullable = true)
        @JsonProperty("scheduled_instance_uuid")
        UUID scheduledInstanceUuid,

        @Schema(description = "When the booking was released, when applicable", nullable = true)
        @JsonProperty("released_at")
        LocalDateTime releasedAt,

        @Schema(description = "Why the booking was released, when applicable", nullable = true)
        @JsonProperty("release_reason")
        String releaseReason
) {
}
