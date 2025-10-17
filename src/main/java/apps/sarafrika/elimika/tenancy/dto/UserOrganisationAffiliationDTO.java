package apps.sarafrika.elimika.tenancy.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO representing a user's affiliation with an organization.
 * Contains the user's role/domain within the specific organization,
 * branch assignment (if any), and temporal information about the affiliation.
 *
 * @param organisationUuid UUID of the organization the user is affiliated with
 * @param organisationName Name of the organization
 * @param domainInOrganisation User's domain/role within this specific organization (student, instructor, admin, organisation_user, course_creator)
 * @param branchUuid UUID of the training branch within the organization (optional, null if not assigned)
 * @param branchName Name of the training branch (if assigned)
 * @param startDate Date when the user's affiliation with this organization started
 * @param endDate Date when the user's affiliation ends (null means ongoing)
 * @param active Whether this affiliation is currently active
 * @param affiliatedDate Timestamp when this affiliation was created in the system
 * @author Elimika Team
 * @since 1.1
 */
public record UserOrganisationAffiliationDTO(
        @JsonProperty("organisation_uuid")
        UUID organisationUuid,

        @JsonProperty("organisation_name")
        String organisationName,

        @JsonProperty("domain_in_organisation")
        String domainInOrganisation,

        @JsonProperty("branch_uuid")
        UUID branchUuid,

        @JsonProperty("branch_name")
        String branchName,

        @JsonProperty("start_date")
        LocalDate startDate,

        @JsonProperty("end_date")
        LocalDate endDate,

        @JsonProperty("active")
        boolean active,

        @JsonProperty("affiliated_date")
        LocalDateTime affiliatedDate
) {
}
