package apps.sarafrika.elimika.shared.spi.enrollment;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * Enrollment Lookup Service Provider Interface
 * <p>
 * Provides read-only access to enrollment information for other modules.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2025-10-23
 */
public interface EnrollmentLookupService {

    record ClassEnrollmentStatusSnapshot(
            UUID enrollmentUuid,
            String status,
            LocalDateTime lastUpdatedAt
    ) { }

    /**
     * Checks if an enrollment exists.
     *
     * @param enrollmentUuid The UUID of the enrollment
     * @return true if the enrollment exists, false otherwise
     */
    boolean enrollmentExists(UUID enrollmentUuid);

    /**
     * Gets the student UUID for an enrollment.
     *
     * @param enrollmentUuid The UUID of the enrollment
     * @return Optional containing the student UUID, or empty if enrollment not found
     */
    Optional<UUID> getEnrollmentStudentUuid(UUID enrollmentUuid);

    /**
     * Gets the scheduled instance UUID for an enrollment.
     *
     * @param enrollmentUuid The UUID of the enrollment
     * @return Optional containing the scheduled instance UUID, or empty if enrollment not found
     */
    Optional<UUID> getEnrollmentScheduledInstanceUuid(UUID enrollmentUuid);

    /**
     * Checks if a student is enrolled in a specific scheduled instance.
     *
     * @param studentUuid The UUID of the student
     * @param scheduledInstanceUuid The UUID of the scheduled instance
     * @return true if the student is enrolled, false otherwise
     */
    boolean isStudentEnrolledInInstance(UUID studentUuid, UUID scheduledInstanceUuid);

    /**
     * Returns the most recent class enrollment for a student in a given course.
     *
     * @param studentUuid The UUID of the student
     * @param courseUuid The UUID of the course
     * @return Optional containing the most recent class enrollment status snapshot
     */
    Optional<ClassEnrollmentStatusSnapshot> findMostRecentEnrollmentForCourse(UUID studentUuid, UUID courseUuid);

    /**
     * Returns the most recent class enrollment for a student in a given class definition.
     *
     * @param studentUuid The UUID of the student
     * @param classDefinitionUuid The UUID of the class definition
     * @return Optional containing the most recent class enrollment status snapshot
     */
    Optional<ClassEnrollmentStatusSnapshot> findMostRecentEnrollmentForClassDefinition(UUID studentUuid,
                                                                                       UUID classDefinitionUuid);

    /**
     * Returns the most recent active class enrollment for a student in a given course.
     * Active enrollments are those still in-progress for the course.
     *
     * @param studentUuid The UUID of the student
     * @param courseUuid The UUID of the course
     * @return Optional containing the most recent active class enrollment status snapshot
     */
    Optional<ClassEnrollmentStatusSnapshot> findMostRecentActiveEnrollmentForCourse(UUID studentUuid, UUID courseUuid);

    /**
     * How many distinct learners hold a live enrolment on any of these classes.
     * <p>
     * Distinct, because a learner is enrolled once per scheduled session: counting rows would report
     * a term of weekly sessions as thirteen learners. Cancelled and waitlisted enrolments do not
     * count — neither describes somebody who was taught.
     *
     * @param classDefinitionUuids the classes to count across; empty yields zero
     * @return the number of distinct learners
     */
    long countDistinctLearnersForClassDefinitions(Collection<UUID> classDefinitionUuids);

    /**
     * How many seats these classes have filled, counting each learner once per class they are on.
     * <p>
     * Differs from {@link #countDistinctLearnersForClassDefinitions(Collection)} in that a learner
     * attending two of the classes occupies two seats but is one learner. Only ever published as a
     * ratio against capacity.
     *
     * @param classDefinitionUuids the classes to count across; empty yields zero
     * @return the number of filled seats
     */
    long countFilledSeatsForClassDefinitions(Collection<UUID> classDefinitionUuids);
}
