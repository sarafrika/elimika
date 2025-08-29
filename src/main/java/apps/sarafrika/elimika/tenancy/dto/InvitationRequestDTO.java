package apps.sarafrika.elimika.tenancy.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

@Schema(description = "Request body for creating a new organization or branch invitation.")
public record InvitationRequestDTO(
        @Schema(description = "Email address of the person being invited. Must be a valid email format.", example = "john.doe@example.com", required = true)
        @NotBlank(message = "Recipient email cannot be blank")
        @Email(message = "Invalid email format")
        String recipientEmail,

        @Schema(description = "Full name of the person being invited. Used in email templates and invitation records.", example = "John Doe", required = true)
        @NotBlank(message = "Recipient name cannot be blank")
        @Size(max = 150, message = "Recipient name cannot exceed 150 characters")
        String recipientName,

        @Schema(description = "Role/domain name being offered to the recipient. Valid values:<ul><li><b>student</b>: A learner enrolled in courses.</li><li><b>instructor</b>: A teacher or facilitator for courses.</li><li><b>admin</b>: An administrator with full control over the organization.</li><li><b>organisation_user</b>: A general user within the organization with basic permissions.</li></ul>", example = "instructor", required = true)
        @NotBlank(message = "Domain name cannot be blank")
        String domainName,

        @Schema(description = "UUID of the user who is sending this invitation. Must be an existing user with appropriate permissions.", example = "550e8400-e29b-41d4-a716-446655440004", required = true)
        @NotNull(message = "Inviter UUID cannot be null")
        UUID inviterUuid,

        @Schema(description = "Optional personal message or notes to include with the invitation email. Maximum 500 characters.", example = "Welcome to our training program!")
        @Size(max = 500, message = "Notes cannot exceed 500 characters")
        String notes
) {
}