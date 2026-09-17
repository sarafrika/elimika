package apps.sarafrika.elimika.course.dto;

import apps.sarafrika.elimika.course.util.enums.CourseTrainingApplicantType;
import apps.sarafrika.elimika.course.util.enums.TrainingApplicationType;
import apps.sarafrika.elimika.course.util.enums.TrainingRateUpdateStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

/** A proposed rate update, shown beside the rate card currently in force. */
@Schema(name = "TrainingRateUpdate", description = "A proposed replacement rate card on an approved training application")
public record TrainingRateUpdateDTO(

        @Schema(description = "**[READ-ONLY]** Identifier of the rate update.", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "uuid", access = JsonProperty.Access.READ_ONLY)
        UUID uuid,

        @Schema(description = "**[READ-ONLY]** The training application whose rates would change.", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "application_uuid", access = JsonProperty.Access.READ_ONLY)
        UUID applicationUuid,

        @Schema(description = "**[READ-ONLY]** Whether the application targets a course or a program.",
                allowableValues = {"course", "program"}, accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "application_type", access = JsonProperty.Access.READ_ONLY)
        TrainingApplicationType applicationType,

        @Schema(description = "**[READ-ONLY]** The course, for a course application; otherwise null.", nullable = true,
                accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "course_uuid", access = JsonProperty.Access.READ_ONLY)
        UUID courseUuid,

        @Schema(description = "**[READ-ONLY]** The program, for a program application; otherwise null.", nullable = true,
                accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "program_uuid", access = JsonProperty.Access.READ_ONLY)
        UUID programUuid,

        @Schema(description = "**[READ-ONLY]** Applicant kind.", allowableValues = {"instructor", "organisation"},
                accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "applicant_type", access = JsonProperty.Access.READ_ONLY)
        CourseTrainingApplicantType applicantType,

        @Schema(description = "**[READ-ONLY]** Instructor or organisation UUID.", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "applicant_uuid", access = JsonProperty.Access.READ_ONLY)
        UUID applicantUuid,

        @Schema(description = "**[READ-ONLY]** Instructor display name or organisation name.", nullable = true,
                accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "applicant_name", access = JsonProperty.Access.READ_ONLY)
        String applicantName,

        @Schema(description = "**[READ-ONLY]** The rate card in force on the application right now.",
                accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "current_rate_card", access = JsonProperty.Access.READ_ONLY)
        CourseTrainingRateCardDTO currentRateCard,

        @Schema(description = "**[READ-ONLY]** The full rate card proposed.", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "proposed_rate_card", access = JsonProperty.Access.READ_ONLY)
        CourseTrainingRateCardDTO proposedRateCard,

        @Schema(description = "**[READ-ONLY]** The applicant's reason for the change.", nullable = true,
                accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "note", access = JsonProperty.Access.READ_ONLY)
        String note,

        @Schema(description = "**[READ-ONLY]** Review status.", allowableValues = {"pending", "approved", "rejected", "withdrawn"},
                accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "status", access = JsonProperty.Access.READ_ONLY)
        TrainingRateUpdateStatus status,

        @Schema(description = "**[READ-ONLY]** When the update was proposed (UTC).", format = "date-time",
                accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "created_date", access = JsonProperty.Access.READ_ONLY)
        LocalDateTime createdDate,

        @Schema(description = "**[READ-ONLY]** Who approved or rejected it.", nullable = true, accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "reviewed_by", access = JsonProperty.Access.READ_ONLY)
        String reviewedBy,

        @Schema(description = "**[READ-ONLY]** When it was approved, rejected or closed (UTC).", format = "date-time",
                nullable = true, accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "reviewed_at", access = JsonProperty.Access.READ_ONLY)
        LocalDateTime reviewedAt,

        @Schema(description = "**[READ-ONLY]** The course creator's notes.", nullable = true, accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "review_notes", access = JsonProperty.Access.READ_ONLY)
        String reviewNotes
) {
}
