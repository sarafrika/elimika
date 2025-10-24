package apps.sarafrika.elimika.course.dto;

import apps.sarafrika.elimika.course.util.enums.CourseTrainingApplicantType;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Request payload for submitting a course training application.
 */
@Schema(
        name = "CourseTrainingApplicationRequest",
        description = "Payload for instructors or organisations applying to deliver a course",
        example = """
        {
          "applicant_type": "instructor",
          "applicant_uuid": "inst-1234-5678-90ab-cdef12345678",
          "application_notes": "I hold the vendor certification required for this course."
        }
        """
)
public record CourseTrainingApplicationRequest(

        @Schema(
                description = "**[REQUIRED]** Applicant type initiating the request.",
                allowableValues = {"instructor", "organisation"},
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @JsonProperty("applicant_type")
        @NotNull(message = "Applicant type is required")
        CourseTrainingApplicantType applicantType,

        @Schema(
                description = "**[REQUIRED]** UUID of the instructor or organisation applying.",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @JsonProperty("applicant_uuid")
        @NotNull(message = "Applicant UUID is required")
        UUID applicantUuid,

        @Schema(
                description = "Optional notes to help the course creator evaluate the request.",
                maxLength = 2000,
                nullable = true
        )
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonProperty("application_notes")
        @Size(max = 2000, message = "Application notes must not exceed 2000 characters")
        String applicationNotes
) {
}
