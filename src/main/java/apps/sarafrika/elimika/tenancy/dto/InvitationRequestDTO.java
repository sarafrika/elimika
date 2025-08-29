package apps.sarafrika.elimika.tenancy.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Invitation Request Data Transfer Object
 * <p>
 * Request body for creating new organization or branch invitations in the Sarafrika Elimika system.
 * Contains all required and optional information needed to generate and send an email invitation.
 * 
 * <h3>Usage:</h3>
 * <ul>
 *   <li><strong>Organization Invitations:</strong> Used with POST /api/v1/organisations/{organisationUuid}/invitations</li>
 *   <li><strong>Branch Invitations:</strong> Used with POST /api/v1/branches/{branchUuid}/invitations</li>
 * </ul>
 * 
 * <h3>Field Requirements:</h3>
 * <ul>
 *   <li><strong>Required:</strong> recipient_email, recipient_name, domain_name, inviter_uuid</li>
 *   <li><strong>Optional:</strong> notes</li>
 * </ul>
 * 
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2025-08-29
 */
@Schema(
        name = "InvitationRequest",
        description = "Request body for creating new organization or branch invitations with recipient details and role assignment",
        example = """
                {
                    "recipient_email": "john.doe@example.com",
                    "recipient_name": "John Doe",
                    "domain_name": "instructor",
                    "inviter_uuid": "550e8400-e29b-41d4-a716-446655440004",
                    "notes": "Welcome to our training program! We're excited to have you join our team."
                }
                """
)
public record InvitationRequestDTO(

        @Schema(
                description = "**[REQUIRED]** Email address of the invitation recipient. Must be a valid email format and will be used to send the invitation email with accept/decline links.",
                example = "john.doe@example.com",
                format = "email",
                maxLength = 100,
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "Recipient email is required")
        @Email(message = "Recipient email must be a valid email address")
        @Size(max = 100, message = "Recipient email cannot exceed 100 characters")
        @JsonProperty("recipient_email")
        String recipientEmail,

        @Schema(
                description = "**[REQUIRED]** Full name of the invitation recipient. Used in email templates, invitation records, and for display purposes throughout the invitation process.",
                example = "John Doe",
                minLength = 1,
                maxLength = 150,
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "Recipient name is required")
        @Size(max = 150, message = "Recipient name cannot exceed 150 characters")
        @JsonProperty("recipient_name")
        String recipientName,

        @Schema(
                description = "**[REQUIRED]** Role/domain name being offered to the recipient. Determines the permissions and access level the user will have upon accepting the invitation.",
                example = "instructor",
                allowableValues = {"student", "instructor", "admin", "organisation_user"},
                requiredMode = Schema.RequiredMode.REQUIRED,
                implementation = String.class,
                externalDocs = @io.swagger.v3.oas.annotations.ExternalDocumentation(
                        description = "User Domain Roles Guide",
                        url = "/docs/OrganizationDomainsGuide.md"
                )
        )
        @NotBlank(message = "Domain name is required")
        @JsonProperty("domain_name")
        String domainName,

        @Schema(
                description = "**[REQUIRED]** UUID of the user who is sending this invitation. Must be an existing user with appropriate permissions to invite users to the target organization/branch.",
                example = "550e8400-e29b-41d4-a716-446655440004",
                format = "uuid",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "Inviter UUID is required")
        @JsonProperty("inviter_uuid")
        UUID inviterUuid,

        @Schema(
                description = "**[OPTIONAL]** Optional personal message or notes to include with the invitation email. Will be displayed in the invitation email template and can contain welcoming text, instructions, or context.",
                example = "Welcome to our training program! We're excited to have you join our team as an instructor. Please let us know if you have any questions.",
                nullable = true,
                maxLength = 500,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        @Size(max = 500, message = "Notes cannot exceed 500 characters")
        @JsonProperty("notes")
        String notes

) {
        
        /**
         * Domain role definitions for API documentation and validation.
         */
        public static class DomainRoles {
                public static final String STUDENT = "student";
                public static final String INSTRUCTOR = "instructor";  
                public static final String ADMIN = "admin";
                public static final String ORGANISATION_USER = "organisation_user";
                
                /**
                 * Provides human-readable descriptions for each domain role.
                 * 
                 * @param domainName the domain role name
                 * @return description of the role's purpose and permissions
                 */
                public static String getDescription(String domainName) {
                        return switch (domainName) {
                                case STUDENT -> "A learner enrolled in courses with access to learning materials and assessments";
                                case INSTRUCTOR -> "A teacher or facilitator with course creation and management capabilities";
                                case ADMIN -> "An administrator with full control over the organization and its users";
                                case ORGANISATION_USER -> "A general user within the organization with basic permissions";
                                default -> "Unknown role";
                        };
                }
        }
}