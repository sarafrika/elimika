package apps.sarafrika.elimika.tenancy.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * Request payload for inviting one or more people to join an organisation.
 * <p>
 * Sending an invitation creates an offer only. No account is provisioned and no
 * affiliation exists until the recipient - or, for a minor, their guardian - accepts.
 */
@Schema(
        name = "SendOrganisationInvitationsRequest",
        description = "Invites one or more recipients to join an organisation. Supply `recipients`, " +
                "`student_group_uuids`, or both - at least one must yield somebody to invite."
)
public record SendOrganisationInvitationsRequestDTO(

        @Schema(description = "**[OPTIONAL when student_group_uuids is supplied]** People to invite by address.")
        @Size(max = 200, message = "A single send cannot exceed 200 recipients")
        @Valid
        @JsonProperty("recipients")
        List<Recipient> recipients,

        @Schema(description = "**[OPTIONAL]** Organisation student groups to invite. Every member of each " +
                "group is invited individually, so each person still decides for themselves. " +
                "Combined with `recipients`, and de-duplicated by email.",
                nullable = true)
        @JsonProperty("student_group_uuids")
        List<UUID> studentGroupUuids,

        @Schema(
                description = "**[REQUIRED]** Org-scoped domain the recipients are invited into.",
                example = "student",
                allowableValues = {"student", "instructor", "admin", "organisation_user"},
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "domain_name is required")
        @JsonProperty("domain_name")
        String domainName,

        @Schema(description = "**[OPTIONAL]** Training branch to scope the invitation to.",
                format = "uuid", nullable = true)
        @JsonProperty("branch_uuid")
        UUID branchUuid,

        @Schema(description = "**[OPTIONAL]** Classes to surface to the recipient once they accept. " +
                "These are never auto-enrolled.", nullable = true)
        @JsonProperty("class_uuids")
        List<UUID> classUuids,

        @Schema(description = "**[OPTIONAL]** Personal note included in the invitation email.",
                nullable = true)
        @Size(max = 2000, message = "Message cannot exceed 2000 characters")
        @JsonProperty("message")
        String message,

        @Schema(description = "**[OPTIONAL]** Days until the invitation lapses. Defaults to 14.",
                example = "14", nullable = true)
        @Min(value = 1, message = "Invitations must be valid for at least a day")
        @Max(value = 90, message = "Invitations cannot be valid for more than 90 days")
        @JsonProperty("expires_in_days")
        Integer expiresInDays
) {

    /**
     * A single invitee. Only the email is required; the name is used to personalise the
     * email and to pre-fill registration for someone new to the platform.
     */
    @Schema(name = "OrganisationInvitationRecipient", description = "A single invitee.")
    public record Recipient(

            @Schema(description = "**[REQUIRED]** Email address to invite.",
                    example = "jane.doe@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank(message = "Recipient email is required")
            @Email(message = "Recipient email must be valid")
            @Size(max = 150, message = "Email cannot exceed 150 characters")
            @JsonProperty("email")
            String email,

            @Schema(description = "**[OPTIONAL]** Display name for the invitee.",
                    example = "Jane Doe", nullable = true)
            @Size(max = 150, message = "Name cannot exceed 150 characters")
            @JsonProperty("name")
            String name
    ) {
    }
}
