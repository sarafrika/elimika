package apps.sarafrika.elimika.course.spi;

import apps.sarafrika.elimika.shared.enums.ClassVisibility;
import apps.sarafrika.elimika.shared.enums.SessionFormat;

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
     * Resolves the approved instructor rate for the provided privacy/session format pair.
     *
     * @param courseUuid     Course identifier
     * @param instructorUuid Instructor identifier
     * @param visibility     Desired class visibility
     * @param sessionFormat  Target session format
     * @return Optional containing the matching rate if the instructor is approved and has a rate card entry
     */
    Optional<BigDecimal> resolveInstructorRate(UUID courseUuid,
                                               UUID instructorUuid,
                                               ClassVisibility visibility,
                                               SessionFormat sessionFormat);

    /**
     * Resolves the approved organisation rate for the provided privacy/session format pair.
     *
     * @param courseUuid       Course identifier
     * @param organisationUuid Organisation identifier
     * @param visibility       Desired class visibility
     * @param sessionFormat    Target session format
     * @return Optional containing the matching rate if the organisation is approved and has a rate card entry
     */
    Optional<BigDecimal> resolveOrganisationRate(UUID courseUuid,
                                                 UUID organisationUuid,
                                                 ClassVisibility visibility,
                                                 SessionFormat sessionFormat);
}
