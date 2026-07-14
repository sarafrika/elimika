package apps.sarafrika.elimika.resourcing.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(
        name = "ResourceCalendarEntry",
        description = "One entry in a resource's merged calendar view: an expanded open-hours window, a blackout, a recruitment hold or a confirmed booking"
)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ResourceCalendarEntryDTO(

        @Schema(description = "Entry kind", example = "HOLD", allowableValues = {"OPEN_HOURS", "BLACKOUT", "HOLD", "CONFIRMED"})
        @JsonProperty("entry_type")
        String entryType,

        @Schema(description = "Entry window start (UTC)")
        @JsonProperty("start_time")
        LocalDateTime startTime,

        @Schema(description = "Entry window end (UTC)")
        @JsonProperty("end_time")
        LocalDateTime endTime,

        @Schema(description = "Availability rule the entry was expanded from, when applicable", nullable = true)
        @JsonProperty("rule_uuid")
        UUID ruleUuid,

        @Schema(description = "Booking behind the entry, when applicable", nullable = true)
        @JsonProperty("booking_uuid")
        UUID bookingUuid,

        @Schema(description = "Marketplace job holding the slot, when applicable", nullable = true)
        @JsonProperty("job_uuid")
        UUID jobUuid,

        @Schema(description = "Class definition occupying the slot, when applicable", nullable = true)
        @JsonProperty("class_definition_uuid")
        UUID classDefinitionUuid,

        @Schema(description = "Units reserved, for equipment pool bookings", nullable = true)
        @JsonProperty("quantity")
        Integer quantity,

        @Schema(description = "Rule notes or booking release reason, when applicable", nullable = true)
        @JsonProperty("notes")
        String notes
) {
}
