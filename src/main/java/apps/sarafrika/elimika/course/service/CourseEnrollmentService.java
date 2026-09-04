package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.CourseEnrollmentDTO;
import apps.sarafrika.elimika.course.util.enums.EnrollmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface CourseEnrollmentService {
    CourseEnrollmentDTO createCourseEnrollment(CourseEnrollmentDTO courseEnrollmentDTO);

    CourseEnrollmentDTO getCourseEnrollmentByUuid(UUID uuid);

    Page<CourseEnrollmentDTO> getAllCourseEnrollments(Pageable pageable);

    CourseEnrollmentDTO updateCourseEnrollment(UUID uuid, CourseEnrollmentDTO courseEnrollmentDTO);

    void deleteCourseEnrollment(UUID uuid);

    Page<CourseEnrollmentDTO> search(Map<String, String> searchParams, Pageable pageable);

    /**
     * Reads a course's enrolments through the lens of whoever is asking.
     * <p>
     * A course's roster names its learners, so it is not something a catalogue browser may download.
     * The count of enrolments, on the other hand, is the engagement figure printed on every course
     * card. This returns the most either party may have:
     * <ul>
     *     <li>the course's staff - its creator, an instructor or organisation approved to deliver it -
     *     and platform admins get every enrolment in full;</li>
     *     <li>a learner enrolled in the course gets their own enrolment, and only theirs;</li>
     *     <li>anybody else gets the same page count, but each row stripped of the learner's identity,
     *     progress, grade and audit trail.</li>
     * </ul>
     *
     * @param courseUuid the course whose enrolments are wanted
     * @param pageable   the page to read
     * @return the caller's view of the course's enrolments, never null
     */
    Page<CourseEnrollmentDTO> getCourseEnrollmentsForCaller(UUID courseUuid, Pageable pageable);

    boolean existsByStudentUuidAndCourseUuid(UUID studentUuid, UUID courseUuid);

    boolean existsByCourseUuidAndStatusIn(UUID uuid, List<EnrollmentStatus> enrollmentStatuses);
}
