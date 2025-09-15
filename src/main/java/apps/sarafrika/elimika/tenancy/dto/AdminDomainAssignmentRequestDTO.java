package apps.sarafrika.elimika.tenancy.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Request DTO for assigning admin domain to a user
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2024-12-01
 */
@Schema(
    name = "AdminDomainAssignmentRequest",
    description = "Request object for assigning admin domain privileges to a user"
)
public record AdminDomainAssignmentRequestDTO(

    @Schema(
        description = "Domain name to assign to the user",
        example = "admin",
        allowableValues = {"admin", "organisation_user"}
    )
    @NotBlank(message = "Domain name is required")
    String domainName,

    @Schema(
        description = "Type of assignment - global or organization-specific",
        example = "global",
        allowableValues = {"global", "organization"}
    )
    @NotBlank(message = "Assignment type is required")
    String assignmentType,

    @Schema(
        description = "Reason for assigning admin privileges",
        example = "Promoted to system administrator role"
    )
    @Size(max = 500, message = "Reason cannot exceed 500 characters")
    String reason,

    @Schema(
        description = "Effective date for the admin assignment",
        example = "2024-12-01"
    )
    LocalDate effectiveDate
) {

    public AdminDomainAssignmentRequestDTO {
        if (effectiveDate == null) {
            effectiveDate = LocalDate.now();
        }
    }
}