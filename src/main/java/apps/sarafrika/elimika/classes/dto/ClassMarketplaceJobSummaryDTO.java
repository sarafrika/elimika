package apps.sarafrika.elimika.classes.dto;

import apps.sarafrika.elimika.classes.util.enums.ClassMarketplaceJobStatus;
import apps.sarafrika.elimika.shared.enums.LocationType;
import apps.sarafrika.elimika.shared.enums.SessionFormat;
import apps.sarafrika.elimika.shared.utils.enums.RateBasis;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** The job facts an application row shows, loaded for a whole page at once. */
@Schema(name = "ClassMarketplaceJobSummary",
        description = "Compact read-only summary of the job an application was made to")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ClassMarketplaceJobSummaryDTO(

        @Schema(description = "**[READ-ONLY]** Job title.", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "title", access = JsonProperty.Access.READ_ONLY)
        String title,

        @Schema(description = "**[READ-ONLY]** Job status.", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "status", access = JsonProperty.Access.READ_ONLY)
        ClassMarketplaceJobStatus status,

        @Schema(description = "**[READ-ONLY]** Course the class teaches; absent for a program job.", nullable = true,
                accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "course_uuid", access = JsonProperty.Access.READ_ONLY)
        UUID courseUuid,

        @Schema(description = "**[READ-ONLY]** Name of the course.", nullable = true, accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "course_name", access = JsonProperty.Access.READ_ONLY)
        String courseName,

        @Schema(description = "**[READ-ONLY]** Training program the class teaches; absent for a course job.", nullable = true,
                accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "program_uuid", access = JsonProperty.Access.READ_ONLY)
        UUID programUuid,

        @Schema(description = "**[READ-ONLY]** Title of the training program.", nullable = true,
                accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "program_name", access = JsonProperty.Access.READ_ONLY)
        String programName,

        @Schema(description = "**[READ-ONLY]** Organisation that posted the job.", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "organisation_uuid", access = JsonProperty.Access.READ_ONLY)
        UUID organisationUuid,

        @Schema(description = "**[READ-ONLY]** Name of the organisation.", nullable = true, accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "organisation_name", access = JsonProperty.Access.READ_ONLY)
        String organisationName,

        @Schema(description = "**[READ-ONLY]** Training branch the class is delivered at.", nullable = true,
                accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "branch_uuid", access = JsonProperty.Access.READ_ONLY)
        UUID branchUuid,

        @Schema(description = "**[READ-ONLY]** Name of the training branch.", nullable = true, accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "branch_name", access = JsonProperty.Access.READ_ONLY)
        String branchName,

        @Schema(description = "**[READ-ONLY]** How the class is delivered.", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "location_type", access = JsonProperty.Access.READ_ONLY)
        LocationType locationType,

        @Schema(description = "**[READ-ONLY]** Private or group class.", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "session_format", access = JsonProperty.Access.READ_ONLY)
        SessionFormat sessionFormat,

        @Schema(description = "**[READ-ONLY]** The basis instructor_pay is quoted on.", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "rate_basis", access = JsonProperty.Access.READ_ONLY)
        RateBasis rateBasis,

        @Schema(description = "**[READ-ONLY]** Pay per rate_basis; absent under the same rule as the job read.",
                nullable = true, accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "instructor_pay", access = JsonProperty.Access.READ_ONLY)
        BigDecimal instructorPay,

        @Schema(description = "**[READ-ONLY]** Start of the earliest planned session (UTC).", format = "date-time",
                nullable = true, accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "first_session_start", access = JsonProperty.Access.READ_ONLY)
        LocalDateTime firstSessionStart,

        @Schema(description = "**[READ-ONLY]** Number of planned sessions.", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "session_count", access = JsonProperty.Access.READ_ONLY)
        Integer sessionCount,

        @Schema(description = "**[READ-ONLY]** The class created for the job, once there is one.", nullable = true,
                accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "class_definition_uuid", access = JsonProperty.Access.READ_ONLY)
        UUID classDefinitionUuid,

        @Schema(description = "**[READ-ONLY]** The branch's contact person; only for the job's hired instructor, the organisation's managers and platform admins.",
                nullable = true, accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "contact_name", access = JsonProperty.Access.READ_ONLY)
        String contactName,

        @Schema(description = "**[READ-ONLY]** The contact person's phone; same visibility as contact_name.", nullable = true,
                accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "contact_phone", access = JsonProperty.Access.READ_ONLY)
        String contactPhone,

        @Schema(description = "**[READ-ONLY]** The contact person's email; same visibility as contact_name.", nullable = true,
                accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "contact_email", access = JsonProperty.Access.READ_ONLY)
        String contactEmail
) {
}
