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
