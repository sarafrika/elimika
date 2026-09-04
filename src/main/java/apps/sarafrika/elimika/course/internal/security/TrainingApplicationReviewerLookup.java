package apps.sarafrika.elimika.course.internal.security;

import apps.sarafrika.elimika.course.repository.CourseTrainingApplicationRepository;
import apps.sarafrika.elimika.course.repository.ProgramTrainingApplicationRepository;
import apps.sarafrika.elimika.course.util.enums.CourseTrainingApplicantType;
import apps.sarafrika.elimika.coursecreator.spi.CourseCreatorLookupService;
import apps.sarafrika.elimika.shared.spi.instructor.InstructorCredentialReviewerLookup;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * The course module's answer to "may this caller read that instructor's credentials?": yes, while
 * the instructor has an outstanding or decided application to train one of the caller's own courses
 * or training programmes.
 * <p>
 * The relationship is checked against the creator's own catalogue, so holding the
 * {@code course_creator} domain buys nothing by itself — an applicant has to have chosen you.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TrainingApplicationReviewerLookup implements InstructorCredentialReviewerLookup {

    private final CourseTrainingApplicationRepository courseTrainingApplicationRepository;
    private final ProgramTrainingApplicationRepository programTrainingApplicationRepository;
    private final CourseCreatorLookupService courseCreatorLookupService;

    @Override
    public boolean isReviewingApplicationFrom(UUID instructorUuid, UUID reviewerUserUuid) {
        if (instructorUuid == null || reviewerUserUuid == null) {
            return false;
        }
        try {
            UUID courseCreatorUuid = courseCreatorLookupService
                    .findCourseCreatorUuidByUserUuid(reviewerUserUuid)
                    .orElse(null);
            if (courseCreatorUuid == null) {
                return false;
            }

            return courseTrainingApplicationRepository.existsForCourseCreator(
                    CourseTrainingApplicantType.INSTRUCTOR, instructorUuid, courseCreatorUuid)
                    || programTrainingApplicationRepository.existsForCourseCreator(
                    CourseTrainingApplicantType.INSTRUCTOR, instructorUuid, courseCreatorUuid);
        } catch (Exception e) {
            log.error("Error checking training-application review rights over instructor {}", instructorUuid, e);
            return false;
        }
    }
}
