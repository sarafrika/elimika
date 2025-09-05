package apps.sarafrika.elimika.classes.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Domain event DTO fired when a class definition is deactivated.
 * <p>
 * This event notifies other modules (like Timetabling) about the deactivation
 * of a class definition so they can handle any scheduled instances.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2024-09-05
 */
public record ClassDefinitionDeactivatedEventDTO(
        @NotNull UUID definitionUuid,
        @NotNull String title
) {}