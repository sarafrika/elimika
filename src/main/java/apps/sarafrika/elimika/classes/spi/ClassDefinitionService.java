package apps.sarafrika.elimika.classes.spi;

import apps.sarafrika.elimika.classes.dto.ClassDefinitionDTO;
import apps.sarafrika.elimika.classes.dto.RecurrencePatternDTO;

import java.util.List;
import java.util.UUID;

/**
 * Service Provider Interface for Class Definition operations.
 * <p>
 * This interface defines the public API that other modules can use to interact
 * with the Classes module. It provides operations for managing class definitions
 * and their associated recurrence patterns.
 * <p>
 * The SPI follows Spring Modulith patterns for inter-module communication
 * and maintains clean boundaries between modules.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2024-09-05
 */
public interface ClassDefinitionService {

    /**
     * Creates a new class definition.
     *
     * @param classDefinition The class definition data to create
     * @return The created class definition with generated UUID and audit fields
     * @throws IllegalArgumentException if required fields are missing or invalid
     */
    ClassDefinitionDTO createClassDefinition(ClassDefinitionDTO classDefinition);

    /**
     * Updates an existing class definition.
     *
     * @param definitionUuid The UUID of the class definition to update
     * @param classDefinition The updated class definition data
     * @return The updated class definition
     * @throws IllegalArgumentException if the UUID is null or invalid
     * @throws RuntimeException if class definition is not found
     */
    ClassDefinitionDTO updateClassDefinition(UUID definitionUuid, ClassDefinitionDTO classDefinition);

    /**
     * Deactivates a class definition by setting its active status to false.
     * This is a soft delete operation that preserves the record for audit purposes.
     *
     * @param definitionUuid The UUID of the class definition to deactivate
     * @throws IllegalArgumentException if the UUID is null or invalid
     * @throws RuntimeException if class definition is not found
     */
    void deactivateClassDefinition(UUID definitionUuid);

    /**
     * Retrieves a class definition by its UUID.
     *
     * @param definitionUuid The UUID of the class definition to retrieve
     * @return The class definition if found
     * @throws IllegalArgumentException if the UUID is null or invalid
     * @throws RuntimeException if class definition is not found
     */
    ClassDefinitionDTO getClassDefinition(UUID definitionUuid);

    /**
     * Retrieves all class definitions for a specific course.
     *
     * @param courseUuid The UUID of the course
     * @return List of class definitions for the course (empty list if none found)
     * @throws IllegalArgumentException if the UUID is null or invalid
     */
    List<ClassDefinitionDTO> findClassesForCourse(UUID courseUuid);

    /**
     * Retrieves all active class definitions for a specific course.
     *
     * @param courseUuid The UUID of the course
     * @return List of active class definitions for the course (empty list if none found)
     * @throws IllegalArgumentException if the UUID is null or invalid
     */
    List<ClassDefinitionDTO> findActiveClassesForCourse(UUID courseUuid);

    /**
     * Retrieves all class definitions for a specific instructor.
     *
     * @param instructorUuid The UUID of the instructor
     * @return List of class definitions for the instructor (empty list if none found)
     * @throws IllegalArgumentException if the UUID is null or invalid
     */
    List<ClassDefinitionDTO> findClassesForInstructor(UUID instructorUuid);

    /**
     * Retrieves all active class definitions for a specific instructor.
     *
     * @param instructorUuid The UUID of the instructor
     * @return List of active class definitions for the instructor (empty list if none found)
     * @throws IllegalArgumentException if the UUID is null or invalid
     */
    List<ClassDefinitionDTO> findActiveClassesForInstructor(UUID instructorUuid);

    /**
     * Retrieves all class definitions for a specific organization.
     *
     * @param organisationUuid The UUID of the organization
     * @return List of class definitions for the organization (empty list if none found)
     * @throws IllegalArgumentException if the UUID is null or invalid
     */
    List<ClassDefinitionDTO> findClassesForOrganisation(UUID organisationUuid);

    /**
     * Retrieves all active class definitions.
     *
     * @return List of all active class definitions (empty list if none found)
     */
    List<ClassDefinitionDTO> findAllActiveClasses();

    /**
     * Creates a new recurrence pattern.
     *
     * @param recurrencePattern The recurrence pattern data to create
     * @return The created recurrence pattern with generated UUID and audit fields
     * @throws IllegalArgumentException if required fields are missing or invalid
     */
    RecurrencePatternDTO createRecurrencePattern(RecurrencePatternDTO recurrencePattern);

    /**
     * Updates an existing recurrence pattern.
     *
     * @param patternUuid The UUID of the recurrence pattern to update
     * @param recurrencePattern The updated recurrence pattern data
     * @return The updated recurrence pattern
     * @throws IllegalArgumentException if the UUID is null or invalid
     * @throws RuntimeException if recurrence pattern is not found
     */
    RecurrencePatternDTO updateRecurrencePattern(UUID patternUuid, RecurrencePatternDTO recurrencePattern);

    /**
     * Retrieves a recurrence pattern by its UUID.
     *
     * @param patternUuid The UUID of the recurrence pattern to retrieve
     * @return The recurrence pattern if found
     * @throws IllegalArgumentException if the UUID is null or invalid
     * @throws RuntimeException if recurrence pattern is not found
     */
    RecurrencePatternDTO getRecurrencePattern(UUID patternUuid);

    /**
     * Deletes a recurrence pattern. This will also remove the pattern reference
     * from any class definitions that use it.
     *
     * @param patternUuid The UUID of the recurrence pattern to delete
     * @throws IllegalArgumentException if the UUID is null or invalid
     * @throws RuntimeException if recurrence pattern is not found or still in use
     */
    void deleteRecurrencePattern(UUID patternUuid);
}