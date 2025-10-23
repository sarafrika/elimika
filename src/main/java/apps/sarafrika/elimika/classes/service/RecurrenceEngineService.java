package apps.sarafrika.elimika.classes.service;

import apps.sarafrika.elimika.classes.dto.ClassDefinitionDTO;
import apps.sarafrika.elimika.classes.dto.RecurrencePatternDTO;
import apps.sarafrika.elimika.timetabling.spi.ScheduledInstanceDTO;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Service for generating recurring class schedules from class definitions and recurrence patterns.
 * <p>
 * This service handles the Google Calendar-like functionality of converting class definitions
 * with recurrence patterns into individual scheduled instances over time.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2024-09-06
 */
public interface RecurrenceEngineService {

    /**
     * Generates scheduled instances for a class definition based on its recurrence pattern.
     *
     * @param classDefinition The class definition containing recurrence pattern
     * @param scheduleStartDate The date to start generating schedules from
     * @param scheduleEndDate The date to stop generating schedules (optional, uses pattern end date if null)
     * @return List of generated scheduled instances
     * @throws IllegalArgumentException if classDefinition is null or has no recurrence pattern
     */
    List<ScheduledInstanceDTO> generateScheduledInstances(
            ClassDefinitionDTO classDefinition,
            LocalDate scheduleStartDate,
            LocalDate scheduleEndDate
    );

    /**
     * Generates scheduled instances for a class definition and recurrence pattern.
     *
     * @param classDefinition The class definition
     * @param recurrencePattern The recurrence pattern to use
     * @param scheduleStartDate The date to start generating schedules from
     * @param scheduleEndDate The date to stop generating schedules (optional, uses pattern end date if null)
     * @return List of generated scheduled instances
     * @throws IllegalArgumentException if any parameter is null
     */
    List<ScheduledInstanceDTO> generateScheduledInstances(
            ClassDefinitionDTO classDefinition,
            RecurrencePatternDTO recurrencePattern,
            LocalDate scheduleStartDate,
            LocalDate scheduleEndDate
    );

    /**
     * Creates and persists scheduled instances for a recurring class.
     * This is the main method for Google Calendar-like recurring schedule creation.
     *
     * @param classDefinitionUuid The UUID of the class definition
     * @param scheduleStartDate The date to start scheduling from
     * @param scheduleEndDate The date to stop scheduling (optional, uses pattern end date if null)
     * @return List of created scheduled instances
     * @throws IllegalArgumentException if classDefinitionUuid is null or class not found
     */
    List<ScheduledInstanceDTO> scheduleRecurringClass(
            UUID classDefinitionUuid,
            LocalDate scheduleStartDate,
            LocalDate scheduleEndDate
    );

    /**
     * Updates the recurring schedule for an existing class definition.
     * Cancels future instances that no longer match the pattern and creates new ones.
     *
     * @param classDefinitionUuid The UUID of the class definition
     * @return List of updated/created scheduled instances
     * @throws IllegalArgumentException if classDefinitionUuid is null or class not found
     */
    List<ScheduledInstanceDTO> updateRecurringSchedule(UUID classDefinitionUuid);

    /**
     * Cancels all future scheduled instances for a class definition.
     *
     * @param classDefinitionUuid The UUID of the class definition
     * @param reason The reason for cancellation
     * @return Number of cancelled instances
     */
    int cancelRecurringSchedule(UUID classDefinitionUuid, String reason);

    /**
     * Validates that a class definition can be used for recurring scheduling.
     *
     * @param classDefinition The class definition to validate
     * @throws IllegalArgumentException if the class definition is invalid for scheduling
     */
    void validateClassDefinitionForScheduling(ClassDefinitionDTO classDefinition);

    /**
     * Validates that a recurrence pattern is valid and can be processed.
     *
     * @param recurrencePattern The recurrence pattern to validate
     * @throws IllegalArgumentException if the recurrence pattern is invalid
     */
    void validateRecurrencePattern(RecurrencePatternDTO recurrencePattern);

    /**
     * Calculates the next occurrence dates for a recurrence pattern within a date range.
     *
     * @param recurrencePattern The recurrence pattern
     * @param startDate The start date for calculation
     * @param endDate The end date for calculation
     * @return List of dates when the pattern occurs
     */
    List<LocalDate> calculateOccurrenceDates(
            RecurrencePatternDTO recurrencePattern,
            LocalDate startDate,
            LocalDate endDate
    );

    /**
     * Checks for scheduling conflicts with instructor availability and existing classes.
     *
     * @param scheduledInstances The list of scheduled instances to check
     * @return List of instances that have conflicts
     */
    List<ScheduledInstanceDTO> checkSchedulingConflicts(List<ScheduledInstanceDTO> scheduledInstances);
}