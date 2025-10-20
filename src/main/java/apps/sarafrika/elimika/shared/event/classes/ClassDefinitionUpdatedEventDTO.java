package apps.sarafrika.elimika.shared.event.classes;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Domain event DTO fired when a class definition is updated.
 * <p>
 * This event notifies other modules (like Timetabling) about changes
 * to a class definition that may require schedule updates.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2024-09-05
 */
public record ClassDefinitionUpdatedEventDTO(
        @NotNull UUID definitionUuid,
        @NotNull String title
) {}