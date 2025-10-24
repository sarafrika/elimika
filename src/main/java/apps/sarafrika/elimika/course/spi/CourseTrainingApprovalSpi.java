package apps.sarafrika.elimika.course.spi;

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
}
