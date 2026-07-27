package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.OrganisationCourseContentDTO;

import java.util.UUID;

/**
 * Assembles course content for an organisation, gating lesson bodies behind an approved
 * training application so unapproved schools only ever see the decision-making summary.
 */
public interface OrganisationCourseContentService {

    /**
     * @param courseUuid       the course being viewed
     * @param organisationUuid the organisation viewing it
     * @return outline + rating always; full lesson content only when the organisation is approved
     */
    OrganisationCourseContentDTO getContentForOrganisation(UUID courseUuid, UUID organisationUuid);
}
