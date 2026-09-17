package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.course.factory.TrainingRateCardFactory;
import apps.sarafrika.elimika.course.model.TrainingRateCardHolder;
import apps.sarafrika.elimika.course.repository.CourseTrainingApplicationRepository;
import apps.sarafrika.elimika.course.repository.ProgramTrainingApplicationRepository;
import apps.sarafrika.elimika.course.spi.ApprovedTrainingRate;
import apps.sarafrika.elimika.course.spi.CourseTrainingApprovalSpi;
import apps.sarafrika.elimika.course.util.enums.CourseTrainingApplicantType;
import apps.sarafrika.elimika.course.util.enums.CourseTrainingApplicationStatus;
import apps.sarafrika.elimika.shared.enums.LocationType;
import apps.sarafrika.elimika.shared.enums.SessionFormat;
import apps.sarafrika.elimika.shared.utils.enums.RateBasis;
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

    @Override
    public Optional<BigDecimal> resolveInstructorRate(UUID courseUuid, UUID instructorUuid,
                                                      SessionFormat sessionFormat, LocationType locationType,
                                                      RateBasis basis) {
        return resolveInstructorRateWithCurrency(courseUuid, instructorUuid, sessionFormat, locationType, basis)
                .map(ApprovedTrainingRate::rate);
    }

    @Override
    public Optional<ApprovedTrainingRate> resolveInstructorRateWithCurrency(UUID courseUuid, UUID instructorUuid,
                                                                            SessionFormat sessionFormat,
                                                                            LocationType locationType,
                                                                            RateBasis basis) {
        return resolveRate(courseUuid, instructorUuid, CourseTrainingApplicantType.INSTRUCTOR, sessionFormat, locationType, basis);
    }

    @Override
    public Optional<BigDecimal> resolveOrganisationRate(UUID courseUuid, UUID organisationUuid,
                                                        SessionFormat sessionFormat, LocationType locationType,
                                                        RateBasis basis) {
        return resolveRate(courseUuid, organisationUuid, CourseTrainingApplicantType.ORGANISATION, sessionFormat, locationType, basis)
                .map(ApprovedTrainingRate::rate);
    }

    @Override
    public Optional<BigDecimal> resolveInstructorProgramRate(UUID programUuid, UUID instructorUuid,
                                                             SessionFormat sessionFormat, LocationType locationType,
                                                             RateBasis basis) {
        return resolveProgramRate(programUuid, instructorUuid, CourseTrainingApplicantType.INSTRUCTOR, sessionFormat, locationType, basis)
                .map(ApprovedTrainingRate::rate);
    }

    @Override
    public Optional<BigDecimal> resolveOrganisationProgramRate(UUID programUuid, UUID organisationUuid,
                                                               SessionFormat sessionFormat, LocationType locationType,
                                                               RateBasis basis) {
        return resolveProgramRate(programUuid, organisationUuid, CourseTrainingApplicantType.ORGANISATION, sessionFormat, locationType, basis)
                .map(ApprovedTrainingRate::rate);
    }

    private Optional<ApprovedTrainingRate> resolveRate(UUID courseUuid,
                                                       UUID applicantUuid,
                                                       CourseTrainingApplicantType applicantType,
                                                       SessionFormat sessionFormat,
                                                       LocationType locationType,
                                                       RateBasis basis) {
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
                .flatMap(application -> extractRate(application, sessionFormat, locationType, basis));
    }

    private Optional<ApprovedTrainingRate> resolveProgramRate(UUID programUuid,
                                                              UUID applicantUuid,
                                                              CourseTrainingApplicantType applicantType,
                                                              SessionFormat sessionFormat,
                                                              LocationType locationType,
                                                              RateBasis basis) {
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
                .flatMap(application -> extractRate(application, sessionFormat, locationType, basis));
    }

    /** A null or non-positive cell is not offered, so it resolves to nothing rather than to a price. */
    private static Optional<ApprovedTrainingRate> extractRate(TrainingRateCardHolder application,
                                                              SessionFormat sessionFormat,
                                                              LocationType locationType,
                                                              RateBasis basis) {
        return Optional.ofNullable(TrainingRateCardFactory.toDTO(application).resolveRate(sessionFormat, locationType, basis))
                .filter(rate -> rate.signum() > 0)
                .map(rate -> new ApprovedTrainingRate(rate, application.getRateCurrency()));
    }
}
