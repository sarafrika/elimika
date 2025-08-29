package apps.sarafrika.elimika.tenancy.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Invitation Preview Data Transfer Object
 * <p>
 * Provides public-safe invitation details for users clicking invitation links.
 * Contains only the information needed to display invitation details without requiring authentication.
 * Sensitive information like tokens and internal IDs are excluded for security.
 * 
 * <h3>Usage:</h3>
 * <ul>
 *   <li><strong>Public Endpoint:</strong> GET /api/v1/invitations/preview?token={token}</li>
 *   <li><strong>Security:</strong> No authentication required, token validates access</li>
 *   <li><strong>Purpose:</strong> Allow users to see invitation details before login/registration</li>
 * </ul>
 * 
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2025-08-29
 */
@Schema(
        name = "InvitationPreview", 
        description = "Public-safe invitation details shown to users before authentication",
        example = """
                {
                    "recipient_name": "John Doe",
                    "organisation_name": "Acme Training Institute",
                    "branch_name": "Downtown Branch",
                    "role_name": "Instructor",
                    "role_description": "A teacher or facilitator with course creation and management capabilities",
                    "inviter_name": "Jane Smith",
                    "expires_at": "2025-09-05T14:30:00",
                    "notes": "Welcome to our training program! We're excited to have you join our team.",
                    "is_expired": false,
                    "requires_registration": true
                }
                """
)
public record InvitationPreviewDTO(

        @Schema(
                description = "Full name of the person being invited",
                example = "John Doe",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @JsonProperty("recipient_name")
        String recipientName,

        @Schema(
                description = "Name of the organization extending the invitation",
                example = "Acme Training Institute", 
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @JsonProperty("organisation_name")
        String organisationName,

        @Schema(
                description = "Name of the specific training branch (if applicable)",
                example = "Downtown Branch",
                nullable = true,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        @JsonProperty("branch_name")
        String branchName,

        @Schema(
                description = "Display name of the role/domain being offered",
                example = "Instructor",
                allowableValues = {"Student", "Instructor", "Administrator", "Organization Member"},
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @JsonProperty("role_name")
        String roleName,

        @Schema(
                description = "Detailed description of the role's responsibilities and permissions",
                example = "A teacher or facilitator with course creation and management capabilities",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @JsonProperty("role_description") 
        String roleDescription,

        @Schema(
                description = "Full name of the person who sent the invitation",
                example = "Jane Smith",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @JsonProperty("inviter_name")
        String inviterName,

        @Schema(
                description = "Date and time when the invitation expires in ISO 8601 format",
                example = "2025-09-05T14:30:00",
                format = "date-time",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @JsonProperty("expires_at")
        LocalDateTime expiresAt,

        @Schema(
                description = "Optional personal message or notes included with the invitation",
                example = "Welcome to our training program! We're excited to have you join our team.",
                nullable = true,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        @JsonProperty("notes")
        String notes,

        @Schema(
                description = "Indicates whether the invitation has expired and can no longer be accepted",
                example = "false",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @JsonProperty("is_expired")
        boolean isExpired,

        @Schema(
                description = "Indicates whether the recipient needs to register an account before accepting. True for student/instructor roles, false for admin/organisation_user roles.",
                example = "true",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @JsonProperty("requires_registration")
        boolean requiresRegistration

) {
        
        /**
         * Creates a preview DTO from a full invitation DTO with role descriptions.
         * 
         * @param invitation the full invitation DTO
         * @param requiresRegistration whether the user needs to register first
         * @return invitation preview with public-safe information
         */
        public static InvitationPreviewDTO fromInvitationDTO(InvitationDTO invitation, boolean requiresRegistration) {
                return new InvitationPreviewDTO(
                        invitation.recipientName(),
                        invitation.organisationName(),
                        invitation.branchName(), 
                        formatRoleName(invitation.domainName()),
                        getRoleDescription(invitation.domainName()),
                        invitation.inviterName(),
                        invitation.expiresAt(),
                        invitation.notes(),
                        !invitation.status().equals(apps.sarafrika.elimika.tenancy.entity.Invitation.InvitationStatus.PENDING) || 
                        invitation.expiresAt().isBefore(LocalDateTime.now()),
                        requiresRegistration
                );
        }

        /**
         * Formats domain name for display purposes.
         */
        private static String formatRoleName(String domainName) {
                if (domainName == null) return "Member";
                
                return switch (domainName.toLowerCase()) {
                        case "student" -> "Student";
                        case "instructor" -> "Instructor"; 
                        case "admin" -> "Administrator";
                        case "organisation_user" -> "Organization Member";
                        default -> "Member";
                };
        }

        /**
         * Provides detailed role descriptions for user understanding.
         */
        private static String getRoleDescription(String domainName) {
                if (domainName == null) return "A general member of the organization";
                
                return switch (domainName.toLowerCase()) {
                        case "student" -> "A learner enrolled in courses with access to learning materials and assessments";
                        case "instructor" -> "A teacher or facilitator with course creation and management capabilities";
                        case "admin" -> "An administrator with full control over the organization and its users";
                        case "organisation_user" -> "A general user within the organization with basic permissions";
                        default -> "A general member of the organization";
                };
        }
}