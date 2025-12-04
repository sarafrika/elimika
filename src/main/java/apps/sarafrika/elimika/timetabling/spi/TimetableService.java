package apps.sarafrika.elimika.timetabling.spi;

import apps.sarafrika.elimika.timetabling.spi.EnrollmentDTO;
import apps.sarafrika.elimika.timetabling.spi.EnrollmentRequestDTO;
import apps.sarafrika.elimika.timetabling.spi.StudentScheduleDTO;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Service Provider Interface for Timetable operations.
 * <p>
 * This interface defines the public API that other modules can use to interact
 * with the Timetabling module. It provides operations for scheduling classes,
 * managing enrollments, and querying schedules.
 * <p>
 * The SPI follows Spring Modulith patterns for inter-module communication
 * and maintains clean boundaries between modules.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2024-09-05
 */
public interface TimetableService {

    // ===== Scheduling Operations =====

    /**
     * Schedules a new class instance on the calendar.
     * This method performs conflict detection to ensure the instructor is available
     * and validates that the class definition exists.
     *
     * @param request The schedule request containing class definition, instructor, and time details
     * @return The created scheduled instance with generated UUID and audit fields
     * @throws IllegalArgumentException if request is null or contains invalid data
     * @throws RuntimeException if class definition is not found or instructor conflicts exist
     */
    ScheduledInstanceDTO scheduleClass(ScheduleRequestDTO request);

    /**
     * Cancels a scheduled instance with the provided reason.
     * This will also cancel all active enrollments for the instance.
     *
     * @param instanceUuid The UUID of the scheduled instance to cancel
     * @param reason The reason for cancellation
     * @throws IllegalArgumentException if instanceUuid is null or reason is empty
     * @throws RuntimeException if scheduled instance is not found or cannot be cancelled
     */
    void cancelScheduledInstance(UUID instanceUuid, String reason);

    /**
     * Updates the status of a scheduled instance (e.g., from SCHEDULED to ONGOING).
     * This is typically used for automated status transitions.
     *
     * @param instanceUuid The UUID of the scheduled instance to update
     * @param newStatus The new status to set
     * @throws IllegalArgumentException if instanceUuid is null or status is invalid
     * @throws RuntimeException if scheduled instance is not found
     */
    void updateScheduledInstanceStatus(UUID instanceUuid, String newStatus);

    // ===== Enrollment Operations =====

    /**
     * Enrolls a student into a class definition.
     * This method performs capacity checks and conflict detection for each scheduled instance and
     * creates enrollments across all sessions tied to the class.
     *
     * @param request The enrollment request containing student and class definition details
     * @return The created enrollments with generated UUID and audit fields
     * @throws IllegalArgumentException if request is null or contains invalid data
     * @throws RuntimeException if any scheduled instance is full or student has conflicts
     */
    List<EnrollmentDTO> enrollStudent(EnrollmentRequestDTO request);

    /**
     * Cancels a student enrollment.
     *
     * @param enrollmentUuid The UUID of the enrollment to cancel
     * @param reason The reason for cancellation
     * @throws IllegalArgumentException if enrollmentUuid is null or reason is empty
     * @throws RuntimeException if enrollment is not found or cannot be cancelled
     */
    void cancelEnrollment(UUID enrollmentUuid, String reason);

    /**
     * Marks attendance for a student enrollment.
     *
     * @param enrollmentUuid The UUID of the enrollment
     * @param attended Whether the student attended (true) or was absent (false)
     * @throws IllegalArgumentException if enrollmentUuid is null
     * @throws RuntimeException if enrollment is not found or attendance already marked
     */
    void markAttendance(UUID enrollmentUuid, boolean attended);

    // ===== Query Operations =====

    /**
     * Retrieves the schedule for a specific instructor within a date range.
     *
     * @param instructorUuid The UUID of the instructor
     * @param start The start date of the range (inclusive)
     * @param end The end date of the range (inclusive)
     * @return List of scheduled instances for the instructor (empty list if none found)
     * @throws IllegalArgumentException if any parameter is null or start is after end
     */
    List<ScheduledInstanceDTO> getScheduleForInstructor(UUID instructorUuid, LocalDate start, LocalDate end);

    /**
     * Retrieves the schedule for a specific student within a date range.
     * This includes enrollment information and attendance status.
     *
     * @param studentUuid The UUID of the student
     * @param start The start date of the range (inclusive)
     * @param end The end date of the range (inclusive)
     * @return List of student schedule entries (empty list if none found)
     * @throws IllegalArgumentException if any parameter is null or start is after end
     */
    List<StudentScheduleDTO> getScheduleForStudent(UUID studentUuid, LocalDate start, LocalDate end);

    /**
     * Retrieves a specific scheduled instance by its UUID.
     *
     * @param instanceUuid The UUID of the scheduled instance
     * @return The scheduled instance
     * @throws IllegalArgumentException if instanceUuid is null
     * @throws RuntimeException if scheduled instance is not found
     */
    ScheduledInstanceDTO getScheduledInstance(UUID instanceUuid);

    /**
     * Retrieves a specific enrollment by its UUID.
     *
     * @param enrollmentUuid The UUID of the enrollment
     * @return The enrollment
     * @throws IllegalArgumentException if enrollmentUuid is null
     * @throws RuntimeException if enrollment is not found
     */
    EnrollmentDTO getEnrollment(UUID enrollmentUuid);

    /**
     * Retrieves all enrollments for a specific scheduled instance.
     *
     * @param instanceUuid The UUID of the scheduled instance
     * @return List of enrollments for the instance (empty list if none found)
     * @throws IllegalArgumentException if instanceUuid is null
     */
    List<EnrollmentDTO> getEnrollmentsForInstance(UUID instanceUuid);

    /**
     * Retrieves all enrollments for a class definition across its scheduled instances.
     *
     * @param classDefinitionUuid The UUID of the class definition
     * @return List of enrollments for the class (empty list if none found)
     * @throws IllegalArgumentException if classDefinitionUuid is null
     */
    List<EnrollmentDTO> getEnrollmentsForClass(UUID classDefinitionUuid);

    /**
     * Checks if an instructor has scheduling conflicts with the proposed time slot.
     *
     * @param instructorUuid The UUID of the instructor
     * @param request The schedule request to validate
     * @return true if there are conflicts, false otherwise
     * @throws IllegalArgumentException if any parameter is null
     */
    boolean hasInstructorConflict(UUID instructorUuid, ScheduleRequestDTO request);

    /**
     * Checks if a student has enrollment conflicts with the proposed time slot.
     *
     * @param studentUuid The UUID of the student
     * @param request The schedule request to validate against
     * @return true if there are conflicts, false otherwise
     * @throws IllegalArgumentException if any parameter is null
     */
    boolean hasStudentConflict(UUID studentUuid, ScheduleRequestDTO request);

    /**
     * Gets the current enrollment count for a scheduled instance.
     *
     * @param instanceUuid The UUID of the scheduled instance
     * @return Current active enrollment count
     * @throws IllegalArgumentException if instanceUuid is null
     */
    long getEnrollmentCount(UUID instanceUuid);

    /**
     * Checks if a scheduled instance has available capacity for new enrollments.
     *
     * @param instanceUuid The UUID of the scheduled instance
     * @return true if capacity is available, false otherwise
     * @throws IllegalArgumentException if instanceUuid is null
     */
    boolean hasCapacityForEnrollment(UUID instanceUuid);

    /**
     * Checks if any scheduled instance for a class definition still has capacity.
     *
     * @param classDefinitionUuid The class definition identifier
     * @return true if at least one scheduled instance can accept enrollments
     */
    boolean hasCapacityForClassDefinition(UUID classDefinitionUuid);

    /**
     * Adds a student to the waitlist for all scheduled instances of a class definition
     * when capacity has been reached.
     *
     * @param request enrollment request containing student and class identifiers
     * @return list of waitlist records created
     */
    List<EnrollmentDTO> joinWaitlist(EnrollmentRequestDTO request);
}
