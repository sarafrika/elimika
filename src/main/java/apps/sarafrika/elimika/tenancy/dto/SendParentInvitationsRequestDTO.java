package apps.sarafrika.elimika.tenancy.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Invites parents to approve their children, under 18, joining an organisation as students. The organisation
 * names each child; nothing is created until the parent approves, and the child then gets a learner profile
 * with no login, managed by that parent.
 */
@Schema(
        name = "SendParentInvitationsRequest",
        description = "One invitation per child, emailed to the child's parent for approval."
)
public record SendParentInvitationsRequestDTO(

        @Schema(description = "**[REQUIRED]** The children to invite, each with their parent.")
        @NotEmpty(message = "At least one child is required")
        @Size(max = 200, message = "A single send cannot exceed 200 children")
        @Valid
        @JsonProperty("children")
        List<Child> children,

        @Schema(description = "**[OPTIONAL]** Training branch to scope the invitations to.",
                format = "uuid", nullable = true)
        @JsonProperty("branch_uuid")
        UUID branchUuid,

        @Schema(description = "**[OPTIONAL]** Classes to surface once the parent approves. Never auto-enrolled.",
                nullable = true)
        @JsonProperty("class_uuids")
        List<UUID> classUuids,

        @Schema(description = "**[OPTIONAL]** Personal note for the parents.", nullable = true)
        @Size(max = 2000, message = "Message cannot exceed 2000 characters")
        @JsonProperty("message")
        String message,

        @Schema(description = "**[OPTIONAL]** Days until the invitations lapse. Defaults to 14.",
                example = "14", nullable = true)
        @Min(value = 1, message = "Invitations must be valid for at least a day")
        @Max(value = 90, message = "Invitations cannot be valid for more than 90 days")
        @JsonProperty("expires_in_days")
        Integer expiresInDays
) {

    @Schema(name = "ParentInvitationChild", description = "A child under 18 and the parent who decides for them.")
    public record Child(

            @Schema(description = "**[REQUIRED]** Child's first name.", example = "Wanjiru")
            @NotBlank(message = "Child first name is required")
            @Size(max = 50, message = "First name cannot exceed 50 characters")
            @JsonProperty("first_name")
            String firstName,

            @Schema(description = "**[REQUIRED]** Child's last name.", example = "Kamau")
            @NotBlank(message = "Child last name is required")
            @Size(max = 50, message = "Last name cannot exceed 50 characters")
            @JsonProperty("last_name")
            String lastName,

            @Schema(description = "**[REQUIRED]** Child's date of birth; the child must be under 18 and pass the "
                    + "learner age gate.", example = "2016-04-12")
            @NotNull(message = "Child date of birth is required")
            @Past(message = "Child date of birth must be in the past")
            @JsonProperty("dob")
            LocalDate dob,

            @Schema(description = "**[REQUIRED]** Parent's email; the approval link is sent here.",
                    example = "mary.kamau@example.com")
            @NotBlank(message = "Parent email is required")
            @Email(message = "Parent email must be valid")
            @Size(max = 150, message = "Email cannot exceed 150 characters")
            @JsonProperty("guardian_email")
            String guardianEmail,

            @Schema(description = "**[OPTIONAL]** Parent's name, used to greet them.", example = "Mary Kamau",
                    nullable = true)
            @Size(max = 150, message = "Name cannot exceed 150 characters")
            @JsonProperty("guardian_name")
            String guardianName,

            @Schema(description = "**[OPTIONAL]** PARENT or GUARDIAN. Defaults to PARENT.", example = "PARENT",
                    allowableValues = {"PARENT", "GUARDIAN"}, nullable = true)
            @JsonProperty("relationship_type")
            String relationshipType
    ) {
    }
}
