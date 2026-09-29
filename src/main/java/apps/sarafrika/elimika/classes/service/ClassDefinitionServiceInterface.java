package apps.sarafrika.elimika.classes.service;

import apps.sarafrika.elimika.classes.dto.ClassDefinitionResponseDTO;
import apps.sarafrika.elimika.classes.dto.ClassDefinitionDTO;
import apps.sarafrika.elimika.classes.dto.ClassSchedulingConflictDTO;
import apps.sarafrika.elimika.classes.dto.ClassSessionTemplateDTO;
import apps.sarafrika.elimika.classes.dto.ClassSessionTemplateScheduleResponseDTO;
import apps.sarafrika.elimika.timetabling.spi.ScheduledInstanceDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
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

    ClassDefinitionResponseDTO createClassDefinition(ClassDefinitionDTO classDefinition,
                                                     MultipartFile thumbnail,
                                                     MultipartFile promotionalVideo);

    ClassDefinitionResponseDTO updateClassDefinition(UUID definitionUuid, ClassDefinitionDTO classDefinition);

    ClassDefinitionResponseDTO uploadThumbnail(UUID definitionUuid, MultipartFile thumbnail);

    ClassDefinitionResponseDTO uploadPromotionalVideo(UUID definitionUuid, MultipartFile promotionalVideo);

    ClassSessionTemplateScheduleResponseDTO addSessionTemplate(UUID definitionUuid, ClassSessionTemplateDTO sessionTemplate);

    void deactivateClassDefinition(UUID definitionUuid);

    ClassDefinitionResponseDTO getClassDefinition(UUID definitionUuid);

    List<ClassDefinitionResponseDTO> findClassesForCourse(UUID courseUuid);

    List<ClassDefinitionResponseDTO> findActiveClassesForCourse(UUID courseUuid);

    List<ClassDefinitionResponseDTO> findClassesForProgram(UUID programUuid);

    List<ClassDefinitionResponseDTO> findActiveClassesForProgram(UUID programUuid);

    List<ClassDefinitionResponseDTO> findClassesForInstructor(UUID instructorUuid);

    List<ClassDefinitionResponseDTO> findActiveClassesForInstructor(UUID instructorUuid);

    List<ClassDefinitionResponseDTO> findClassesForOrganisation(UUID organisationUuid);

    List<apps.sarafrika.elimika.classes.dto.OrganisationInstructorPayableDTO> getInstructorPayablesForOrganisation(
            UUID organisationUuid);

    Page<ClassDefinitionResponseDTO> findAllClasses(Pageable pageable);

    List<ClassDefinitionResponseDTO> findAllActiveClasses();

    /**
     * {@link #findAllClasses} narrowed to classes matching {@code q}: ranked by the search index when
     * it is read-enabled (other {@code searchParams} then filter on its filterable attributes), else a
     * title match in the database.
     */
    Page<ClassDefinitionResponseDTO> searchClasses(String q, Map<String, String> searchParams, Pageable pageable);

    /** {@link #findAllActiveClasses} narrowed to classes matching {@code q}, at most {@code SEARCH_LIST_LIMIT}. */
    List<ClassDefinitionResponseDTO> searchActiveClasses(String q);

    /** {@link #findClassesForOrganisation} narrowed to classes matching {@code q}, at most {@code SEARCH_LIST_LIMIT}. */
    List<ClassDefinitionResponseDTO> searchClassesForOrganisation(UUID organisationUuid, String q);

    /** The most classes an unpaged listing returns for a {@code q} search. */
    int SEARCH_LIST_LIMIT = 100;

    Page<ScheduledInstanceDTO> getClassSchedule(UUID classDefinitionUuid, Pageable pageable);

    Page<ClassSchedulingConflictDTO> getSchedulingConflicts(UUID classDefinitionUuid, Pageable pageable);

}
