package apps.sarafrika.elimika.course.internal.training;

import apps.sarafrika.elimika.course.internal.security.CourseFootingCap;
import apps.sarafrika.elimika.course.spi.CourseSecuritySpi;
import apps.sarafrika.elimika.course.util.enums.CourseContentAccess;
import apps.sarafrika.elimika.course.util.enums.CourseTrainingApplicantType;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** The two parties to a training application: the applicant (instructor or org manager) and the course or program owner. */
@Component
@RequiredArgsConstructor
public class TrainingApplicationAccess {

    private final DomainSecurityService domainSecurityService;
    private final CourseFootingCap courseFootingCap;
    private final CourseSecuritySpi courseSecurity;

    /** Each applicant kind is only reachable from its own dashboard footing. */
    public boolean isApplicant(CourseTrainingApplicantType applicantType, UUID applicantUuid) {
        if (applicantType == null || applicantUuid == null) {
            return false;
        }
        return switch (applicantType) {
            case INSTRUCTOR -> courseFootingCap.permits(CourseContentAccess.INSTRUCTOR)
                    && domainSecurityService.isInstructorWithUuid(applicantUuid);
            case ORGANISATION -> courseFootingCap.permits(CourseContentAccess.ORGANISATION)
                    && domainSecurityService.managesOrganisation(applicantUuid);
        };
    }

    public boolean ownsCourse(UUID courseUuid) {
        return courseUuid != null
                && courseFootingCap.permits(CourseContentAccess.CREATOR)
                && courseSecurity.isCourseOwner(courseUuid);
    }

    public boolean ownsProgram(UUID programUuid) {
        return programUuid != null && courseSecurity.isProgramOwner(programUuid);
    }
}
