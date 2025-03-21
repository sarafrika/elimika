package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.CourseLearningObjectiveDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * Service interface for managing course learning objectives.
 */
public interface CourseLearningObjectiveService {

    /**
     * Creates a new course learning objective.
     *
     * @param courseLearningObjectiveDTO The DTO containing course learning objective details.
     * @return The created CourseLearningObjectiveDTO.
     */
    @Transactional
    CourseLearningObjectiveDTO createCourseLearningObjective(CourseLearningObjectiveDTO courseLearningObjectiveDTO);

    /**
     * Retrieves a course learning objective by its UUID.
     *
     * @param uuid The UUID of the course learning objective.
     * @return The CourseLearningObjectiveDTO representing the learning objective.
     */
    @Transactional(readOnly = true)
    CourseLearningObjectiveDTO getCourseLearningObjectiveByUuid(UUID uuid);

    /**
     * Retrieves all course learning objectives with pagination.
     *
     * @param pageable Pagination and sorting information.
     * @return A paginated list of CourseLearningObjectiveDTOs.
     */
    @Transactional(readOnly = true)
    Page<CourseLearningObjectiveDTO> getAllCourseLearningObjectives(Pageable pageable);

    /**
     * Updates an existing course learning objective.
     *
     * @param uuid The UUID of the course learning objective to update.
     * @param courseLearningObjectiveDTO The DTO containing updated course learning objective details.
     * @return The updated CourseLearningObjectiveDTO.
     */
    @Transactional
    CourseLearningObjectiveDTO updateCourseLearningObjective(UUID uuid, CourseLearningObjectiveDTO courseLearningObjectiveDTO);

    /**
     * Deletes a course learning objective by UUID.
     *
     * @param uuid The UUID of the course learning objective to delete.
     */
    @Transactional
    void deleteCourseLearningObjective(UUID uuid);

    /**
     * Searches for course learning objectives based on given parameters.
     *
     * @param searchParams A map containing search filters.
     * @param pageable Pagination and sorting information.
     * @return A paginated list of CourseLearningObjectiveDTOs matching the search criteria.
     */
    @Transactional(readOnly = true)
    Page<CourseLearningObjectiveDTO> searchCourseLearningObjectives(Map<String, String> searchParams, Pageable pageable);
}