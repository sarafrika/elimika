package apps.sarafrika.elimika.course.dto;

import apps.sarafrika.elimika.course.util.enums.CourseTrainingApplicantType;
import apps.sarafrika.elimika.course.util.enums.CourseTrainingApplicationStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Data Transfer Object representing a course training application.
 */
@Schema(
        name = "CourseTrainingApplication",
        description = "Represents an instructor or organisation request to deliver a course",
        example = """
        {
            "uuid": "b9c6e44f-37d4-4cf9-aa1c-3cfc1ffdd520",
            "course_uuid": "c1o2u3r4-5s6e-7d8a-9t10-abcdefghijkl",
            "applicant_type": "instructor",
            "applicant_uuid": "inst-1234-5678-90ab-cdef12345678",
            "status": "pending",
            "rate_card": {
                "currency": "KES",
                "private_individual_rate": 3500.0000,
                "private_group_rate": 2800.0000,
                "public_individual_rate": 3000.0000,
                "public_group_rate": 2400.0000
            },
            "application_notes": "I have delivered similar courses for 5 years.",
            "review_notes": null,
            "reviewed_by": null,
            "reviewed_at": null,
            "created_date": "2025-10-24T13:16:00",
            "created_by": "instructor@sarafrika.com",
            "updated_date": null,
            "updated_by": null
        }
        """
)
public record CourseTrainingApplicationDTO(

        @Schema(
                description = "**[READ-ONLY]** Unique identifier for this application.",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty(value = "uuid", access = JsonProperty.Access.READ_ONLY)
        UUID uuid,

        @Schema(
                description = "**[READ-ONLY]** The course this application targets.",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty(value = "course_uuid", access = JsonProperty.Access.READ_ONLY)
        UUID courseUuid,

        @Schema(
                description = "**[READ-ONLY]** Applicant type making the request.",
                allowableValues = {"instructor", "organisation"},
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty(value = "applicant_type", access = JsonProperty.Access.READ_ONLY)
        CourseTrainingApplicantType applicantType,

        @Schema(
                description = "**[READ-ONLY]** UUID of the applicant (Instructor or Organisation).",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty(value = "applicant_uuid", access = JsonProperty.Access.READ_ONLY)
        UUID applicantUuid,

        @Schema(
                description = "**[READ-ONLY]** Current status of the application.",
                allowableValues = {"pending", "approved", "rejected"},
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty(value = "status", access = JsonProperty.Access.READ_ONLY)
        CourseTrainingApplicationStatus status,

        @Schema(
                description = "**[READ-ONLY]** Approved rate card for this application.",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty(value = "rate_card", access = JsonProperty.Access.READ_ONLY)
        CourseTrainingRateCardDTO rateCard,

        @Schema(
                description = "Submission notes provided by the applicant.",
                nullable = true
        )
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonProperty("application_notes")
        String applicationNotes,

        @Schema(
                description = "Decision notes provided by the course creator.",
                nullable = true
        )
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonProperty("review_notes")
        String reviewNotes,

        @Schema(
                description = "Reviewer identifier captured when the request is approved or rejected.",
                nullable = true
        )
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonProperty("reviewed_by")
        String reviewedBy,

        @Schema(
                description = "Timestamp of the review decision.",
                nullable = true,
                format = "date-time"
        )
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonProperty("reviewed_at")
        LocalDateTime reviewedAt,

        @Schema(
                description = "**[READ-ONLY]** When the application was submitted.",
                format = "date-time",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty(value = "created_date", access = JsonProperty.Access.READ_ONLY)
        LocalDateTime createdDate,

        @Schema(
                description = "**[READ-ONLY]** Audit user who submitted the application.",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty(value = "created_by", access = JsonProperty.Access.READ_ONLY)
        String createdBy,

        @Schema(
                description = "**[READ-ONLY]** When the application was last updated.",
                format = "date-time",
                nullable = true,
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonProperty(value = "updated_date", access = JsonProperty.Access.READ_ONLY)
        LocalDateTime updatedDate,

        @Schema(
                description = "**[READ-ONLY]** Audit user who last modified the application.",
                nullable = true,
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonProperty(value = "updated_by", access = JsonProperty.Access.READ_ONLY)
        String updatedBy
) {
}
