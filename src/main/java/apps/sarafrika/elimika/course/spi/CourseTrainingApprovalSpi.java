package apps.sarafrika.elimika.course.spi;

import apps.sarafrika.elimika.shared.enums.LocationType;
import apps.sarafrika.elimika.shared.enums.SessionFormat;
import apps.sarafrika.elimika.shared.utils.enums.RateBasis;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/**
 * Exposes course training approval checks to other modules.
 */
public interface CourseTrainingApprovalSpi {

    /**
     * Checks if the specified instructor has an approved application to deliver the given course.
     *
     * @param courseUuid     UUID of the course
     * @param instructorUuid UUID of the instructor
     * @return true if the instructor is approved to deliver the course
     */
    boolean isInstructorApproved(UUID courseUuid, UUID instructorUuid);

    /**
     * Checks if the specified organisation has an approved application to deliver the given course.
     *
     * @param courseUuid       UUID of the course
     * @param organisationUuid UUID of the organisation
     * @return true if the organisation is approved to deliver the course
     */
    boolean isOrganisationApproved(UUID courseUuid, UUID organisationUuid);

    /**
     * Checks if the specified instructor has an approved application to deliver the given training program.
     *
     * @param programUuid    UUID of the training program
     * @param instructorUuid UUID of the instructor
     * @return true if the instructor is approved to deliver the training program
     */
    boolean isInstructorApprovedForProgram(UUID programUuid, UUID instructorUuid);

    /**
     * Checks if the specified organisation has an approved application to deliver the given training program.
     *
     * @param programUuid       UUID of the training program
     * @param organisationUuid UUID of the organisation
     * @return true if the organisation is approved to deliver the training program
     */
    boolean isOrganisationApprovedForProgram(UUID programUuid, UUID organisationUuid);

    /** The instructor's approved course rate for this cell in the stored basis; empty when not approved or not offered. */
    Optional<BigDecimal> resolveInstructorRate(UUID courseUuid,
                                               UUID instructorUuid,
                                               SessionFormat sessionFormat,
                                               LocationType locationType,
                                               RateBasis basis);

    /** As {@link #resolveInstructorRate}, with the card's currency for callers that charge the rate. */
    Optional<ApprovedTrainingRate> resolveInstructorRateWithCurrency(UUID courseUuid,
                                                                     UUID instructorUuid,
                                                                     SessionFormat sessionFormat,
                                                                     LocationType locationType,
                                                                     RateBasis basis);

    /** The organisation's approved course rate for this cell in the stored basis; empty when not approved or not offered. */
    Optional<BigDecimal> resolveOrganisationRate(UUID courseUuid,
                                                 UUID organisationUuid,
                                                 SessionFormat sessionFormat,
                                                 LocationType locationType,
                                                 RateBasis basis);

    /** The instructor's approved program rate for this cell in the stored basis; empty when not approved or not offered. */
    Optional<BigDecimal> resolveInstructorProgramRate(UUID programUuid,
                                                      UUID instructorUuid,
                                                      SessionFormat sessionFormat,
                                                      LocationType locationType,
                                                      RateBasis basis);

    /** The organisation's approved program rate for this cell in the stored basis; empty when not approved or not offered. */
    Optional<BigDecimal> resolveOrganisationProgramRate(UUID programUuid,
                                                        UUID organisationUuid,
                                                        SessionFormat sessionFormat,
                                                        LocationType locationType,
                                                        RateBasis basis);
}
