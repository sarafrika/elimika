package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.course.model.CourseTrainingApplication;
import apps.sarafrika.elimika.course.model.ProgramTrainingApplication;
import apps.sarafrika.elimika.course.repository.CourseTrainingApplicationRepository;
import apps.sarafrika.elimika.course.repository.ProgramTrainingApplicationRepository;
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
    private final ProgramTrainingApplicationRepository programTrainingApplicationRepository;

    @Override
    public boolean isInstructorApproved(UUID courseUuid, UUID instructorUuid) {
        return isApplicantApproved(courseUuid, instructorUuid, CourseTrainingApplicantType.INSTRUCTOR);
    }

    @Override
    public boolean isOrganisationApproved(UUID courseUuid, UUID organisationUuid) {
        return isApplicantApproved(courseUuid, organisationUuid, CourseTrainingApplicantType.ORGANISATION);
    }

    @Override
    public boolean isInstructorApprovedForProgram(UUID programUuid, UUID instructorUuid) {
        return isProgramApplicantApproved(programUuid, instructorUuid, CourseTrainingApplicantType.INSTRUCTOR);
    }

    @Override
    public boolean isOrganisationApprovedForProgram(UUID programUuid, UUID organisationUuid) {
        return isProgramApplicantApproved(programUuid, organisationUuid, CourseTrainingApplicantType.ORGANISATION);
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

    @Override
    public Optional<BigDecimal> resolveInstructorProgramRate(UUID programUuid,
                                                             UUID instructorUuid,
                                                             SessionFormat sessionFormat,
                                                             LocationType locationType) {
        return resolveProgramRate(programUuid, instructorUuid, CourseTrainingApplicantType.INSTRUCTOR, sessionFormat, locationType);
    }

    @Override
    public Optional<BigDecimal> resolveOrganisationProgramRate(UUID programUuid,
                                                               UUID organisationUuid,
                                                               SessionFormat sessionFormat,
                                                               LocationType locationType) {
        return resolveProgramRate(programUuid, organisationUuid, CourseTrainingApplicantType.ORGANISATION, sessionFormat, locationType);
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

    private boolean isProgramApplicantApproved(UUID programUuid,
                                               UUID applicantUuid,
                                               CourseTrainingApplicantType applicantType) {
        if (programUuid == null || applicantUuid == null) {
            return false;
        }
        return programTrainingApplicationRepository.existsByProgramUuidAndApplicantTypeAndApplicantUuidAndStatus(
                programUuid,
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

    private Optional<BigDecimal> resolveProgramRate(UUID programUuid,
                                                    UUID applicantUuid,
                                                    CourseTrainingApplicantType applicantType,
                                                    SessionFormat sessionFormat,
                                                    LocationType locationType) {
        if (programUuid == null || applicantUuid == null || sessionFormat == null) {
            return Optional.empty();
        }

        return programTrainingApplicationRepository
                .findByProgramUuidAndApplicantTypeAndApplicantUuidAndStatus(
                        programUuid,
                        applicantType,
                        applicantUuid,
                        CourseTrainingApplicationStatus.APPROVED
                )
                .map(application -> extractProgramRate(application, sessionFormat, locationType));
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

    private BigDecimal extractProgramRate(ProgramTrainingApplication application,
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
