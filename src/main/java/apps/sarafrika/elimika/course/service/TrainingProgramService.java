package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.CourseDTO;
import apps.sarafrika.elimika.course.dto.TrainingProgramDTO;
import apps.sarafrika.elimika.course.util.enums.ModerationAction;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;
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
     * {@link #getTrainingProgramByUuid(UUID)} for the current caller: a program that is not live
     * (draft, in review, or not admin-approved) resolves only for a platform admin, its author, an
     * enrolled learner or someone approved to deliver it, and is reported as not found to anyone
     * else. Published-and-approved and archived programs are readable by all.
     *
     * @throws apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException if the program does
     *         not exist or is not visible to the caller
     */
    TrainingProgramDTO getVisibleTrainingProgramByUuid(UUID uuid);

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

    TrainingProgramDTO uploadThumbnail(UUID programUuid, MultipartFile thumbnail);

    TrainingProgramDTO uploadBanner(UUID programUuid, MultipartFile banner);

    TrainingProgramDTO uploadIntroVideo(UUID programUuid, MultipartFile introVideo);

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

    /**
     * {@link #search(Map, Pageable)} narrowed to the programs the current caller may discover.
     * <p>
     * Platform admins see everything. Everyone else sees live programs - admin-approved, active and
     * not archived - plus the programs they author themselves, whatever their state. Internal
     * callers that need every program keep using {@link #search(Map, Pageable)}.
     *
     * @param searchParams search parameters with operators
     * @param pageable pagination parameters
     * @return paginated search results visible to the caller
     */
    Page<TrainingProgramDTO> searchForCaller(Map<String, String> searchParams, Pageable pageable);

    /**
     * The admin approval queue: programs never approved that are in review or published. With
     * {@code q} the queue is searched through the programs index with an admin (unrestricted) scope
     * and the hits re-checked against the queue's SQL filter; search off or failing raises
     * {@link apps.sarafrika.elimika.shared.search.SearchUnavailableException} (503). Without
     * {@code q} it is the relational listing.
     *
     * @param q        optional free text
     * @param pageable pagination parameters
     */
    Page<TrainingProgramDTO> searchPendingApproval(String q, Pageable pageable);

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
     * Returns a training program to draft (status DRAFT, not published). It stays active only while
     * learners are actively enrolled, mirroring course unpublishing.
     *
     * @param programUuid the program UUID to unpublish
     * @return the unpublished program DTO
     */
    TrainingProgramDTO unpublishProgram(UUID programUuid);

    /**
     * Archives a training program (status ARCHIVED, not published, inactive).
     *
     * @param programUuid the program UUID to archive
     * @return the archived program DTO
     */
    TrainingProgramDTO archiveProgram(UUID programUuid);

    TrainingProgramDTO approveProgram(UUID programUuid, String reason);

    TrainingProgramDTO unapproveProgram(UUID programUuid, String reason, ModerationAction action);

    boolean isProgramApproved(UUID programUuid);

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
     * Retrieves a page of free training programs (price is null or 0) that the current caller may
     * discover, on the same visibility rule as {@link #searchForCaller(Map, Pageable)}.
     * <p>
     * Price is deliberately not a generic search filter, so "free" is answered by a dedicated
     * specification rather than a {@code price} search parameter.
     *
     * @param pageable pagination parameters
     * @return paginated free programs visible to the caller
     */
    Page<TrainingProgramDTO> getFreeProgramsForCaller(Pageable pageable);

    /**
     * Retrieves training programs by category.
     *
     * @param categoryUuid the category UUID
     * @return list of programs in the category
     */
    List<TrainingProgramDTO> getProgramsByCategory(UUID categoryUuid);

    /**
     * Retrieves training programs by course creator.
     *
     * @param courseCreatorUuid the course creator UUID
     * @return list of programs by the course creator
     */
    List<TrainingProgramDTO> getProgramsByCourseCreator(UUID courseCreatorUuid);

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
