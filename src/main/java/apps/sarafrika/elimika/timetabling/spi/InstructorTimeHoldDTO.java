package apps.sarafrika.elimika.timetabling.spi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A claim an instructor's marketplace application makes on their diary. Read separately from the
 * schedule on purpose: a hold carries no enrolment, attendance or pay, and widening the schedule
 * response would have made every consumer of it filter holds out one by one.
 */
@Schema(
        name = "InstructorTimeHold",
        description = "A tentative or firm claim on an instructor's diary raised by a marketplace class job application",
        example = """
        {
            "uuid": "ith12345-6789-abcd-ef01-234567890abc",
            "instructor_uuid": "inst1234-5678-90ab-cdef-123456789abc",
            "job_uuid": "job12345-6789-abcd-ef01-234567890abc",
            "application_uuid": "app12345-6789-abcd-ef01-234567890abc",
            "organisation_uuid": "org12345-6789-abcd-ef01-234567890abc",
            "organisation_name": "Sarafrika Technical College",
            "title": "Grade 5 Piano - Term 2",
            "start_time": "2026-09-16T09:00:00",
            "end_time": "2026-09-16T10:00:00",
            "timezone": "UTC",
            "status": "TENTATIVE"
        }
        """
)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record InstructorTimeHoldDTO(

        @Schema(description = "**[READ-ONLY]** Unique identifier of the hold", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "uuid", access = JsonProperty.Access.READ_ONLY)
        UUID uuid,

        @Schema(description = "Instructor whose diary the hold sits on")
        @JsonProperty("instructor_uuid")
        UUID instructorUuid,

        @Schema(description = "Marketplace job the hold was raised for")
        @JsonProperty("job_uuid")
        UUID jobUuid,

        @Schema(description = "Application that raised the hold")
        @JsonProperty("application_uuid")
        UUID applicationUuid,

        @Schema(description = "Organisation recruiting for the job", nullable = true)
        @JsonProperty("organisation_uuid")
        UUID organisationUuid,

        @Schema(description = "**[READ-ONLY]** Display name of the recruiting organisation, so a calendar can say "
                + "whose work the held time would be without a second lookup.",
                accessMode = Schema.AccessMode.READ_ONLY, nullable = true)
        @JsonProperty(value = "organisation_name", access = JsonProperty.Access.READ_ONLY)
        String organisationName,

        @Schema(description = "Title of the job the held window would deliver", nullable = true)
        @JsonProperty("title")
        String title,

        @Schema(description = "Held window start (UTC)")
        @JsonProperty("start_time")
        LocalDateTime startTime,

        @Schema(description = "Held window end (UTC)")
        @JsonProperty("end_time")
        LocalDateTime endTime,

        @Schema(description = "Timezone the window was authored in", example = "UTC")
        @JsonProperty("timezone")
        String timezone,

        @Schema(description = "Hold lifecycle state; only FIRM counts as a scheduling clash", example = "TENTATIVE")
        @JsonProperty("status")
        InstructorTimeHoldStatus status,

        @Schema(description = "Class definition the hold became, once confirmed", nullable = true)
        @JsonProperty("class_definition_uuid")
        UUID classDefinitionUuid,

        @Schema(description = "Scheduled instance the hold became, once confirmed", nullable = true)
        @JsonProperty("scheduled_instance_uuid")
        UUID scheduledInstanceUuid
) {

    /**
     * Copies this hold with the recruiting organisation's name, resolved per query rather than
     * stored on the row so a rename does not leave two truths to keep in step.
     */
    public InstructorTimeHoldDTO withOrganisationName(String organisationName) {
        return new InstructorTimeHoldDTO(
                uuid,
                instructorUuid,
                jobUuid,
                applicationUuid,
                organisationUuid,
                organisationName,
                title,
                startTime,
                endTime,
                timezone,
                status,
                classDefinitionUuid,
                scheduledInstanceUuid
        );
    }
}
