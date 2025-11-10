package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.course.model.CourseTrainingApplication;
import apps.sarafrika.elimika.course.repository.CourseTrainingApplicationRepository;
import apps.sarafrika.elimika.course.spi.CourseTrainingApprovalSpi;
import apps.sarafrika.elimika.course.util.enums.CourseTrainingApplicantType;
import apps.sarafrika.elimika.course.util.enums.CourseTrainingApplicationStatus;
import apps.sarafrika.elimika.shared.enums.ClassVisibility;
import apps.sarafrika.elimika.shared.enums.SessionFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CourseTrainingApprovalSpiImpl implements CourseTrainingApprovalSpi {

    private final CourseTrainingApplicationRepository applicationRepository;

    @Override
    public boolean isInstructorApproved(UUID courseUuid, UUID instructorUuid) {
        return isApplicantApproved(courseUuid, instructorUuid, CourseTrainingApplicantType.INSTRUCTOR);
    }

    @Override
    public boolean isOrganisationApproved(UUID courseUuid, UUID organisationUuid) {
        return isApplicantApproved(courseUuid, organisationUuid, CourseTrainingApplicantType.ORGANISATION);
    }

    @Override
    public Optional<BigDecimal> resolveInstructorRate(UUID courseUuid,
                                                      UUID instructorUuid,
                                                      ClassVisibility visibility,
                                                      SessionFormat sessionFormat) {
        return resolveRate(courseUuid, instructorUuid, CourseTrainingApplicantType.INSTRUCTOR, visibility, sessionFormat);
    }

    @Override
    public Optional<BigDecimal> resolveOrganisationRate(UUID courseUuid,
                                                        UUID organisationUuid,
                                                        ClassVisibility visibility,
                                                        SessionFormat sessionFormat) {
        return resolveRate(courseUuid, organisationUuid, CourseTrainingApplicantType.ORGANISATION, visibility, sessionFormat);
    }

    private boolean isApplicantApproved(UUID courseUuid,
                                        UUID applicantUuid,
                                        CourseTrainingApplicantType applicantType) {
        if (courseUuid == null || applicantUuid == null) {
            return false;
        }
        return applicationRepository.existsByCourseUuidAndApplicantTypeAndApplicantUuidAndStatus(
                courseUuid,
                applicantType,
                applicantUuid,
                CourseTrainingApplicationStatus.APPROVED
        );
    }

    private Optional<BigDecimal> resolveRate(UUID courseUuid,
                                             UUID applicantUuid,
                                             CourseTrainingApplicantType applicantType,
                                             ClassVisibility visibility,
                                             SessionFormat sessionFormat) {
        if (courseUuid == null || applicantUuid == null || visibility == null || sessionFormat == null) {
            return Optional.empty();
        }

        return applicationRepository
                .findByCourseUuidAndApplicantTypeAndApplicantUuidAndStatus(
                        courseUuid,
                        applicantType,
                        applicantUuid,
                        CourseTrainingApplicationStatus.APPROVED
                )
                .map(application -> extractRate(application, visibility, sessionFormat));
    }

    private BigDecimal extractRate(CourseTrainingApplication application,
                                   ClassVisibility visibility,
                                   SessionFormat sessionFormat) {
        return switch (visibility) {
            case PRIVATE -> sessionFormat == SessionFormat.INDIVIDUAL
                    ? application.getPrivateIndividualRate()
                    : application.getPrivateGroupRate();
            case PUBLIC -> sessionFormat == SessionFormat.INDIVIDUAL
                    ? application.getPublicIndividualRate()
                    : application.getPublicGroupRate();
        };
    }
}
