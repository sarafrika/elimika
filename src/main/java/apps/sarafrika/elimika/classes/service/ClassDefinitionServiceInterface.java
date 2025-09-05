package apps.sarafrika.elimika.classes.service;

import apps.sarafrika.elimika.classes.dto.ClassDefinitionDTO;
import apps.sarafrika.elimika.classes.dto.RecurrencePatternDTO;

import java.util.List;
import java.util.UUID;

/**
 * Internal service interface for Class Definition operations.
 * <p>
 * This interface defines the internal service contract within the Classes module.
 * The implementation of this interface will also implement the public SPI.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2024-09-05
 */
public interface ClassDefinitionServiceInterface {

    ClassDefinitionDTO createClassDefinition(ClassDefinitionDTO classDefinition);

    ClassDefinitionDTO updateClassDefinition(UUID definitionUuid, ClassDefinitionDTO classDefinition);

    void deactivateClassDefinition(UUID definitionUuid);

    ClassDefinitionDTO getClassDefinition(UUID definitionUuid);

    List<ClassDefinitionDTO> findClassesForCourse(UUID courseUuid);

    List<ClassDefinitionDTO> findActiveClassesForCourse(UUID courseUuid);

    List<ClassDefinitionDTO> findClassesForInstructor(UUID instructorUuid);

    List<ClassDefinitionDTO> findActiveClassesForInstructor(UUID instructorUuid);

    List<ClassDefinitionDTO> findClassesForOrganisation(UUID organisationUuid);

    List<ClassDefinitionDTO> findAllActiveClasses();

    RecurrencePatternDTO createRecurrencePattern(RecurrencePatternDTO recurrencePattern);

    RecurrencePatternDTO updateRecurrencePattern(UUID patternUuid, RecurrencePatternDTO recurrencePattern);

    RecurrencePatternDTO getRecurrencePattern(UUID patternUuid);

    void deleteRecurrencePattern(UUID patternUuid);
}