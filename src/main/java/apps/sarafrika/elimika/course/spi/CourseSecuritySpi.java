package apps.sarafrika.elimika.course.spi;

import java.util.Set;
import java.util.UUID;

/**
 * Service Provider Interface for course-related security operations.
 * This interface provides authorization checks for course ownership.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2025-10-20
 */
public interface CourseSecuritySpi {

    /**
     * Checks if the currently authenticated user is the owner of the specified course.
     *
     * @param courseUuid UUID of the course to check
     * @return true if the current user owns the course, false otherwise
     */
    boolean isCourseOwner(UUID courseUuid);

    /**
     * Checks if the currently authenticated user created the specified training program.
     * <p>
     * The program-side sibling of {@link #isCourseOwner(UUID)}. A program records its author in
     * {@code course_creator_uuid}, but that column holds the author's course-creator profile when
     * the program was built from the course-creator dashboard and their instructor profile when it
     * was built from the instructor dashboard, so either profile counts as ownership.
     *
     * @param programUuid UUID of the training program to check
     * @return true if the current user created the program, false otherwise
     */
    boolean isProgramOwner(UUID programUuid);

    /**
     * Checks whether the current user may add, change or remove the given course-to-program
     * association.
     * <p>
     * The association's own {@code program_uuid} decides, not the path: the service acts on the row
     * identified by {@code programCourseUuid} and never reads the path variable, so authorising the
     * path alone would let a caller name a program they own and a row belonging to somebody else.
     * When no such row exists the path's program decides instead, leaving the service free to answer
     * 404 for an owner who quoted a stale identifier.
     * <p>
     * {@code payloadProgramUuid} is the program the request body names, which for an update is the
     * program the row would be <em>moved</em> to; it must be owned as well, or {@code null} when the
     * request names none.
     *
     * @param programUuid UUID of the program on the request path
     * @param programCourseUuid UUID of the program-course association being written, or null on create
     * @param payloadProgramUuid UUID of the program named in the request body, or null when absent
     * @return true if the current user may perform the write
     */
    boolean canWriteProgramCourse(UUID programUuid, UUID programCourseUuid, UUID payloadProgramUuid);

    /**
     * Checks whether the current user may add, change or remove the given program requirement.
     * <p>
     * The requirement-side sibling of
     * {@link #canWriteProgramCourse(UUID, UUID, UUID)}, and resolved the same way: the requirement's
     * own program decides, falling back to the path's program when the requirement does not exist.
     *
     * @param programUuid UUID of the program on the request path
     * @param requirementUuid UUID of the requirement being written, or null on create
     * @param payloadProgramUuid UUID of the program named in the request body, or null when absent
     * @return true if the current user may perform the write
     */
    boolean canWriteProgramRequirement(UUID programUuid, UUID requirementUuid, UUID payloadProgramUuid);

    /**
     * Checks whether the current user may read the course's lesson content.
     * <p>
     * True for the course owner or a member of an organisation approved to train
     * the course. Platform admins are granted separately at the endpoint. Enrolled
     * learners read content through their own class flow, not this path.
     *
     * @param courseUuid UUID of the course to check
     * @return true if the current user may read the course content, false otherwise
     */
    boolean canReadCourseContent(UUID courseUuid);

    /**
     * Checks whether the current user is a learner enrolled in this course.
     * <p>
     * True only when the caller holds a course enrolment for this course whose status still
     * {@link apps.sarafrika.elimika.course.util.enums.EnrollmentStatus#allowsAccess() allows access}
     * — active or completed. A dropped or suspended enrolment does not grant entry.
     *
     * @param courseUuid UUID of the course to check
     * @return true if the current user is an enrolled learner on the course
     */
    boolean isEnrolledLearner(UUID courseUuid);

    /**
     * Returns every course the current caller may enter as a learner, as a set supporting O(1)
     * membership tests.
     * <p>
     * Prefer this over repeated {@link #isEnrolledLearner(UUID)} calls when testing several courses
     * — it is the same single load either way, but the intent is clearer at the call site.
     *
     * @return the caller's accessible course UUIDs, never null
     */
    Set<UUID> enrolledCourseUuids();

    /**
     * Checks whether the current user may read the course's material as either staff or learner.
     * <p>
     * This is the learner-facing sibling of {@link #canReadCourseContent(UUID)}: it grants
     * everything that predicate grants, plus enrolled learners. It exists separately so that
     * {@code canReadCourseContent} keeps meaning "staff who own or train this course", and so
     * every deliberately learner-visible route can be found by searching for this method.
     * Platform admins are granted separately at the endpoint.
     *
     * @param courseUuid UUID of the course to check
     * @return true if the current user may read the course as staff or as an enrolled learner
     */
    boolean canReadCourseAsLearner(UUID courseUuid);

    /**
     * Checks whether the current user may read and write the course's gradebook.
     * <p>
     * True for the course owner, an instructor approved to train the course, or a member
     * of an organisation approved to train it. Holding the instructor or course_creator
     * domain is <em>not</em> sufficient on its own - marking somebody's work requires an
     * actual relationship to that course. Platform admins are granted at the endpoint.
     *
     * @param courseUuid UUID of the course to check
     * @return true if the current user may manage the course's gradebook
     */
    boolean canManageCourseGradebook(UUID courseUuid);
}
