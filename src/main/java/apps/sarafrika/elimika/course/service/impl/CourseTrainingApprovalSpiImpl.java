package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.course.model.CourseTrainingApplication;
import apps.sarafrika.elimika.course.repository.CourseTrainingApplicationRepository;
import apps.sarafrika.elimika.course.spi.CourseTrainingApprovalSpi;
import apps.sarafrika.elimika.course.util.enums.CourseTrainingApplicantType;
import apps.sarafrika.elimika.course.util.enums.CourseTrainingApplicationStatus;
import apps.sarafrika.elimika.shared.enums.LocationType;
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
                                                      SessionFormat sessionFormat,
                                                      LocationType locationType) {
        return resolveRate(courseUuid, instructorUuid, CourseTrainingApplicantType.INSTRUCTOR, sessionFormat, locationType);
    }

    @Override
    public Optional<BigDecimal> resolveOrganisationRate(UUID courseUuid,
                                                        UUID organisationUuid,
                                                        SessionFormat sessionFormat,
                                                        LocationType locationType) {
        return resolveRate(courseUuid, organisationUuid, CourseTrainingApplicantType.ORGANISATION, sessionFormat, locationType);
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
                                             SessionFormat sessionFormat,
                                             LocationType locationType) {
        if (courseUuid == null || applicantUuid == null || sessionFormat == null) {
            return Optional.empty();
        }

        return applicationRepository
                .findByCourseUuidAndApplicantTypeAndApplicantUuidAndStatus(
                        courseUuid,
                        applicantType,
                        applicantUuid,
                        CourseTrainingApplicationStatus.APPROVED
                )
                .map(application -> extractRate(application, sessionFormat, locationType));
    }

    private BigDecimal extractRate(CourseTrainingApplication application,
                                   SessionFormat sessionFormat,
                                   LocationType locationType) {
        LocationType effectiveLocation = locationType != null ? locationType : LocationType.ONLINE;
        boolean online = LocationType.ONLINE.equals(effectiveLocation);
        boolean inPerson = LocationType.IN_PERSON.equals(effectiveLocation) || LocationType.HYBRID.equals(effectiveLocation);

        if (!online && !inPerson) {
            online = true;
        }

        return switch (sessionFormat) {
            case INDIVIDUAL -> online ? application.getPrivateOnlineRate() : application.getPrivateInpersonRate();
            case GROUP -> online ? application.getGroupOnlineRate() : application.getGroupInpersonRate();
        };
    }
}
