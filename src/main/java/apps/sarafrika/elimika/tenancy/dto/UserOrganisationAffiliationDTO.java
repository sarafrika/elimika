package apps.sarafrika.elimika.tenancy.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO representing a user's affiliation with an organization.
 * Contains the user's role/domain within the specific organization,
 * branch assignment (if any), and temporal information about the affiliation.
 *
 * @author Elimika Team
 * @since 1.1
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserOrganisationAffiliationDTO {
    
    /**
     * UUID of the organization the user is affiliated with
     */
    private UUID organisationUuid;
    
    /**
     * Name of the organization
     */
    private String organisationName;
    
    /**
     * User's domain/role within this specific organization
     * (student, instructor, admin, organisation_user)
     */
    private String domainInOrganisation;
    
    /**
     * UUID of the training branch within the organization (optional)
     * Null if user is not assigned to a specific branch
     */
    private UUID branchUuid;
    
    /**
     * Name of the training branch (if assigned)
     */
    private String branchName;
    
    /**
     * Date when the user's affiliation with this organization started
     */
    private LocalDate startDate;
    
    /**
     * Date when the user's affiliation ends (null means ongoing)
     */
    private LocalDate endDate;
    
    /**
     * Whether this affiliation is currently active
     */
    private boolean active;
    
    /**
     * Timestamp when this affiliation was created in the system
     */
    private LocalDateTime affiliatedDate;
}