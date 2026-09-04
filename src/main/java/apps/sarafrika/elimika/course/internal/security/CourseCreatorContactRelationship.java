package apps.sarafrika.elimika.course.internal.security;

import apps.sarafrika.elimika.course.repository.CourseEnrollmentRepository;
import apps.sarafrika.elimika.course.repository.CourseTrainingApplicationRepository;
import apps.sarafrika.elimika.course.repository.ProgramEnrollmentRepository;
import apps.sarafrika.elimika.course.repository.ProgramTrainingApplicationRepository;
import apps.sarafrika.elimika.coursecreator.spi.CourseCreatorLookupService;
import apps.sarafrika.elimika.shared.spi.contact.ContactRelationshipSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * The course module's answer to "may this caller hold this person's contact details": yes when the
 * caller owns the course or programme that connects them.
 * <p>
 * Two links qualify, and both are the creator's own catalogue seen from the other side. A learner
 * enrolled on one of the creator's courses or programmes is the creator's learner — the enrolments
 * page renders their email so the creator can chase a stalled cohort. An instructor who has applied
 * to train one of those courses or programmes is the creator's applicant — the review screen renders
 * their email because deciding on an application usually means asking a question first.
 * <p>
 * Scoped to the catalogue, never to the domain: holding {@code course_creator} grants nothing here,
 * only owning the course the other person is attached to does. A creator with an empty catalogue
 * therefore sees no one.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2026-09-04
 */
@Service
@RequiredArgsConstructor
@Slf4j
class CourseCreatorContactRelationship implements ContactRelationshipSource {

    private final CourseCreatorLookupService courseCreatorLookupService;
    private final CourseEnrollmentRepository courseEnrollmentRepository;
    private final ProgramEnrollmentRepository programEnrollmentRepository;
    private final CourseTrainingApplicationRepository courseTrainingApplicationRepository;
    private final ProgramTrainingApplicationRepository programTrainingApplicationRepository;

    @Override
    public boolean viewerMayContactSubject(Party viewer, Party subject) {
        if (viewer.userUuid() == null || subject.hasNoProfile()) {
            return false;
        }
        try {
            UUID courseCreatorUuid = courseCreatorLookupService
                    .findCourseCreatorUuidByUserUuid(viewer.userUuid())
                    .orElse(null);
            if (courseCreatorUuid == null) {
                return false;
            }
            return teachesAsLearner(courseCreatorUuid, subject) || reviewsAsApplicant(courseCreatorUuid, subject);
        } catch (Exception e) {
            log.error("Error checking course-creator contact relationship with user {}", subject.userUuid(), e);
            return false;
        }
    }

    private boolean teachesAsLearner(UUID courseCreatorUuid, Party subject) {
        UUID studentUuid = subject.studentUuid();
        return studentUuid != null
                && (courseEnrollmentRepository.existsForStudentAndCourseCreator(studentUuid, courseCreatorUuid)
                || programEnrollmentRepository.existsForStudentAndCourseCreator(studentUuid, courseCreatorUuid));
    }

    private boolean reviewsAsApplicant(UUID courseCreatorUuid, Party subject) {
        UUID instructorUuid = subject.instructorUuid();
        return instructorUuid != null
                && (courseTrainingApplicationRepository
                .existsForInstructorApplicantAndCourseCreator(instructorUuid, courseCreatorUuid)
                || programTrainingApplicationRepository
                .existsForInstructorApplicantAndCourseCreator(instructorUuid, courseCreatorUuid));
    }
}
