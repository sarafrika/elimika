package apps.sarafrika.elimika.common.event.organisation;

import java.util.UUID;

/**
 * Event record for organisation user registration.
 * 
 * This event is published when a user needs to be registered as an organisation_user.
 * Organisation users can be either organisation administrators (with full org access)
 * or branch-level administrators (with branch-specific access).
 * 
 * The actual scope and permissions are determined by the UserOrganisationDomainMapping
 * configuration, particularly the branch assignment.
 *
 * @param fullName the full name of the organisation user
 * @param userUuid the UUID of the user being registered
 * @param organisationUuid the UUID of the organization they belong to
 * @param isOrganisationAdmin true if this is an org-wide admin, false for branch-level user
 */
public record RegisterOrganisationUser(
        String fullName,
        UUID userUuid,
        UUID organisationUuid,
        boolean isOrganisationAdmin
) {
}