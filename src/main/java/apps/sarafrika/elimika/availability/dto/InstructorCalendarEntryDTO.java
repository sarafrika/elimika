package apps.sarafrika.elimika.availability.dto;

import apps.sarafrika.elimika.shared.enums.AvailabilityType;
import apps.sarafrika.elimika.shared.spi.timetabling.InstructorScheduleStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Calendar entry that merges availability slots, blocked time, and scheduled instances
 * into a single feed for instructor calendars.
 */
@Schema(name = "InstructorCalendarEntry", description = "Unified calendar entry combining availability slots and scheduled instances")
public record InstructorCalendarEntryDTO(

        @Schema(description = "Unique identifier for the entry (slot UUID or scheduled instance UUID where applicable)")
        @JsonProperty("uuid")
        UUID uuid,

        @Schema(description = "Entry type: AVAILABILITY, BLOCKED, or SCHEDULED_INSTANCE", example = "SCHEDULED_INSTANCE")
        @JsonProperty("entry_type")
        CalendarEntryType entryType,

        @Schema(description = "Start date-time for the entry", example = "2024-09-15T09:00:00Z")
        @JsonProperty("start_time")
        LocalDateTime startTime,

        @Schema(description = "End date-time for the entry", example = "2024-09-15T10:30:00Z")
        @JsonProperty("end_time")
        LocalDateTime endTime,

        @Schema(description = "Availability type when the entry is derived from availability patterns", example = "WEEKLY")
        @JsonProperty("availability_type")
        AvailabilityType availabilityType,

        @Schema(description = "Flag indicating availability; false represents blocked time or scheduled instances occupying the slot")
        @JsonProperty("is_available")
        Boolean isAvailable,

        @Schema(description = "Scheduled instance status when applicable", example = "SCHEDULED")
        @JsonProperty("status")
        InstructorScheduleStatus status,

        @Schema(description = "Optional title (for scheduled instances)", example = "Intro to Java")
        @JsonProperty("title")
        String title,

        @Schema(description = "Class definition UUID for scheduled instances")
        @JsonProperty("class_definition_uuid")
        UUID classDefinitionUuid,

        @Schema(description = "Location type for scheduled instances", example = "ONLINE")
        @JsonProperty("location_type")
        String locationType,

        @Schema(description = "Optional source/reason for blocked entries", example = "BLOCKED_TIME_SLOT")
        @JsonProperty("source")
        String source,

        @Schema(description = "Organisation the instructor is delivering this session for, when the class behind it "
                + "is organisation-owned. Null for the instructor's own classes and for availability entries.")
        @JsonProperty("organisation_uuid")
        UUID organisationUuid,

        @Schema(description = "Display name of the owning organisation", example = "Sarafrika Technical College")
        @JsonProperty("organisation_name")
        String organisationName
) {

    /**
     * The same entry reduced to when it is and whether it is free.
     * <p>
     * The calendar is a booking surface, so anyone deciding when to approach an instructor needs to
     * see which windows are already taken. None of them needs to see <em>what</em> is in the window:
     * the class title, the class it belongs to, where it is delivered, why a session was cancelled
     * and which organisation the instructor is delivering it for are the instructor's business and
     * that of the parties who run the class. The identifier and the window survive so a calendar can
     * still lay the entry out; everything describing it goes.
     */
    public InstructorCalendarEntryDTO redacted() {
        return new InstructorCalendarEntryDTO(
                uuid,
                entryType,
                startTime,
                endTime,
                availabilityType,
                isAvailable,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }

    public enum CalendarEntryType {
        AVAILABILITY,
        BLOCKED,
        SCHEDULED_INSTANCE
    }
}
