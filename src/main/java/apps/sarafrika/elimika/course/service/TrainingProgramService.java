package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.CourseDTO;
import apps.sarafrika.elimika.course.dto.TrainingProgramDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service interface for comprehensive training program management.
 * Provides methods for CRUD operations, search functionality, and business logic.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2024-06-30
 */
public interface TrainingProgramService {

    // ===== BASIC CRUD OPERATIONS =====

    /**
     * Creates a new training program with default DRAFT status and inactive state.
     *
     * @param trainingProgramDTO the program data to create
     * @return the created program with system-generated fields
     */
    TrainingProgramDTO createTrainingProgram(TrainingProgramDTO trainingProgramDTO);

    /**
     * Retrieves a training program by its UUID.
     *
     * @param uuid the program UUID
     * @return the program DTO with computed properties
     * @throws apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException if program not found
     */
    TrainingProgramDTO getTrainingProgramByUuid(UUID uuid);

    /**
     * Retrieves all training programs with pagination support.
     *
     * @param pageable pagination parameters
     * @return paginated list of training programs
     */
    Page<TrainingProgramDTO> getAllTrainingPrograms(Pageable pageable);

    /**
     * Updates an existing training program with selective field updates.
     *
     * @param uuid the program UUID to update
     * @param trainingProgramDTO the updated program data
     * @return the updated program DTO
     * @throws apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException if program not found
     */
    TrainingProgramDTO updateTrainingProgram(UUID uuid, TrainingProgramDTO trainingProgramDTO);

    /**
     * Permanently deletes a training program and its associated data.
     *
     * @param uuid the program UUID to delete
     * @throws apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException if program not found
     */
    void deleteTrainingProgram(UUID uuid);

    /**
     * Performs advanced search on training programs with flexible criteria.
     *
     * @param searchParams search parameters with operators
     * @param pageable pagination parameters
     * @return paginated search results
     */
    Page<TrainingProgramDTO> search(Map<String, String> searchParams, Pageable pageable);

    // ===== PROGRAM PUBLISHING =====

    /**
     * Publishes a training program, making it available for enrollment.
     * Sets status to PUBLISHED and active to true.
     *
     * @param programUuid the program UUID to publish
     * @return the published program DTO
     * @throws apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException if program not found
     */
    TrainingProgramDTO publishProgram(UUID programUuid);

    /**
     * Checks if a program is ready for publishing.
     * Validates that program has title, description, and at least one course.
     *
     * @param programUuid the program UUID to check
     * @return true if program is ready for publishing
     */
    boolean isProgramReadyForPublishing(UUID programUuid);

    // ===== PROGRAM COURSES =====

    /**
     * Retrieves all courses in a program ordered by sequence.
     *
     * @param programUuid the program UUID
     * @return list of courses in sequence order
     */
    List<CourseDTO> getAllProgramCourses(UUID programUuid);

    /**
     * Retrieves only the required courses for a program.
     *
     * @param programUuid the program UUID
     * @return list of required courses
     */
    List<CourseDTO> getRequiredCourses(UUID programUuid);

    /**
     * Retrieves only the optional courses for a program.
     *
     * @param programUuid the program UUID
     * @return list of optional courses
     */
    List<CourseDTO> getOptionalCourses(UUID programUuid);

    /**
     * Gets the total number of courses in a program.
     *
     * @param programUuid the program UUID
     * @return total course count
     */
    int getTotalProgramCourses(UUID programUuid);

    /**
     * Gets the total number of required courses in a program.
     *
     * @param programUuid the program UUID
     * @return required course count
     */
    int getTotalRequiredCourses(UUID programUuid);

    // ===== PROGRAM ANALYTICS =====

    /**
     * Calculates the completion rate for a specific program.
     * Returns percentage of enrolled students who completed the program.
     *
     * @param programUuid the program UUID
     * @return completion rate as percentage (0.0 to 100.0)
     */
    double getProgramCompletionRate(UUID programUuid);

    /**
     * Checks if a specific student has completed a program.
     *
     * @param studentUuid the student UUID
     * @param programUuid the program UUID
     * @return true if student completed the program
     */
    boolean isProgramComplete(UUID studentUuid, UUID programUuid);

    // ===== PROGRAM DISCOVERY METHODS =====

    /**
     * Retrieves all active training programs.
     *
     * @return list of active programs
     */
    List<TrainingProgramDTO> getActivePrograms();

    /**
     * Retrieves all published training programs.
     *
     * @return list of published programs
     */
    List<TrainingProgramDTO> getPublishedPrograms();

    /**
     * Retrieves all free training programs (price is null or 0).
     *
     * @return list of free programs
     */
    List<TrainingProgramDTO> getFreePrograms();

    /**
     * Retrieves training programs by category.
     *
     * @param categoryUuid the category UUID
     * @return list of programs in the category
     */
    List<TrainingProgramDTO> getProgramsByCategory(UUID categoryUuid);

    /**
     * Retrieves training programs by instructor.
     *
     * @param instructorUuid the instructor UUID
     * @return list of programs by the instructor
     */
    List<TrainingProgramDTO> getProgramsByInstructor(UUID instructorUuid);

    /**
     * Retrieves programs by type classification.
     *
     * @param programType the program type (e.g., "Extended Program", "Intensive Program")
     * @return list of programs matching the type
     */
    List<TrainingProgramDTO> getProgramsByType(String programType);

    /**
     * Retrieves extended programs (100+ hours duration).
     *
     * @return list of extended programs
     */
    List<TrainingProgramDTO> getExtendedPrograms();

    /**
     * Retrieves intensive programs (50-99 hours duration).
     *
     * @return list of intensive programs
     */
    List<TrainingProgramDTO> getIntensivePrograms();
}