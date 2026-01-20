package apps.sarafrika.elimika.classes.service;

import apps.sarafrika.elimika.classes.dto.ClassDefinitionResponseDTO;
import apps.sarafrika.elimika.classes.dto.ClassDefinitionDTO;
import apps.sarafrika.elimika.classes.dto.ClassSchedulingConflictDTO;
import apps.sarafrika.elimika.timetabling.spi.ScheduledInstanceDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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

    ClassDefinitionResponseDTO createClassDefinition(ClassDefinitionDTO classDefinition);

    ClassDefinitionResponseDTO updateClassDefinition(UUID definitionUuid, ClassDefinitionDTO classDefinition);

    void deactivateClassDefinition(UUID definitionUuid);

    ClassDefinitionResponseDTO getClassDefinition(UUID definitionUuid);

    List<ClassDefinitionResponseDTO> findClassesForCourse(UUID courseUuid);

    List<ClassDefinitionResponseDTO> findActiveClassesForCourse(UUID courseUuid);

    List<ClassDefinitionResponseDTO> findClassesForInstructor(UUID instructorUuid);

    List<ClassDefinitionResponseDTO> findActiveClassesForInstructor(UUID instructorUuid);

    List<ClassDefinitionResponseDTO> findClassesForOrganisation(UUID organisationUuid);

    List<ClassDefinitionResponseDTO> findAllActiveClasses();

    Page<ScheduledInstanceDTO> getClassSchedule(UUID classDefinitionUuid, Pageable pageable);

    Page<ClassSchedulingConflictDTO> getSchedulingConflicts(UUID classDefinitionUuid, Pageable pageable);

}
