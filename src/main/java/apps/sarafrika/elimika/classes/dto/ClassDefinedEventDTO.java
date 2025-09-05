package apps.sarafrika.elimika.classes.dto;

import apps.sarafrika.elimika.classes.util.enums.LocationType;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Domain event DTO fired when a new class definition is created.
 * <p>
 * This event notifies other modules (like Timetabling) about the creation
 * of a new class definition that may need scheduling.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2024-09-05
 */
public record ClassDefinedEventDTO(
        @NotNull UUID definitionUuid,
        @NotNull String title,
        @NotNull Integer durationMinutes,
        @NotNull UUID defaultInstructorUuid,
        UUID courseUuid,
        UUID organisationUuid,
        @NotNull LocationType locationType,
        @NotNull Integer maxParticipants,
        @NotNull Boolean allowWaitlist,
        UUID recurrencePatternUuid
) {}