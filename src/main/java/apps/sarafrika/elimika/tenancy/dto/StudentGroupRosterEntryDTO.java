package apps.sarafrika.elimika.tenancy.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * One row of the organisation student roster: a member joined to the student's user record and to
 * the group's structure, so the Groups table renders from a single paginated response instead of a
 * groups call plus a members call per group plus batched user lookups.
 * <p>
 * Age is deliberately absent — the frontend derives it from {@code dob}, which keeps it correct as
 * the page stays open and avoids a value that is stale the moment it is serialised.
 */
@Schema(name = "StudentGroupRosterEntry",
        description = "A student's roster row: identity, contact details and the group they sit in.")
public record StudentGroupRosterEntryDTO(

        @Schema(description = "The student's user UUID.")
        @JsonProperty("student_uuid")
        UUID studentUuid,

        @Schema(description = "Group the student belongs to.")
        @JsonProperty("group_uuid")
        UUID groupUuid,

        @Schema(description = "Name of the group.")
        @JsonProperty("group_name")
        String groupName,

        @Schema(description = "Academic tier name of the group, or null when the group is unassigned.",
                nullable = true)
        @JsonProperty("tier")
        String tier,

        @Schema(description = "Stream label of the group (student_groups.group_type).", nullable = true)
        @JsonProperty("stream_label")
        String streamLabel,

        @Schema(description = "Student's full name.")
        @JsonProperty("full_name")
        String fullName,

        @Schema(description = "Student's email address.", nullable = true)
        @JsonProperty("email")
        String email,

        @Schema(description = "Student's phone number.", nullable = true)
        @JsonProperty("phone_number")
        String phoneNumber,

        @Schema(description = "Date of birth. Age is derived client-side from this.", nullable = true)
        @JsonProperty("dob")
        LocalDate dob,

        @Schema(description = "Profile image URL.", nullable = true)
        @JsonProperty("profile_image_url")
        String profileImageUrl,

        @Schema(description = "When the student joined the group.")
        @JsonProperty("joined_date")
        LocalDateTime joinedDate
) {

    /** Same row with the persisted storage key replaced by the public URL clients can fetch. */
    public StudentGroupRosterEntryDTO withProfileImageUrl(String resolvedProfileImageUrl) {
        return new StudentGroupRosterEntryDTO(
                studentUuid, groupUuid, groupName, tier, streamLabel, fullName, email, phoneNumber,
                dob, resolvedProfileImageUrl, joinedDate);
    }
}
