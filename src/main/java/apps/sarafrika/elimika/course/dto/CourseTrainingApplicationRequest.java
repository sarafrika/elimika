package apps.sarafrika.elimika.course.dto;

import apps.sarafrika.elimika.course.util.enums.CourseTrainingApplicantType;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
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
          "rate_per_hour_per_head": 2500.0000,
          "rate_currency": "KES",
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
                description = "**[REQUIRED]** Proposed compensation per trainee per hour for delivering this course.",
                example = "2500.0000",
                requiredMode = Schema.RequiredMode.REQUIRED,
                minimum = "0"
        )
        @JsonProperty("rate_per_hour_per_head")
        @NotNull(message = "Rate per hour per head is required")
        @DecimalMin(value = "0.0000", message = "Rate per hour per head cannot be negative")
        @Digits(integer = 8, fraction = 4, message = "Rate per hour per head must have at most 8 digits and 4 decimals")
        BigDecimal ratePerHourPerHead,

        @Schema(
                description = "**[OPTIONAL]** ISO 4217 currency code drawn from the platform approved list. Defaults to the platform currency when omitted.",
                example = "KES",
                nullable = true,
                maxLength = 3
        )
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonProperty("rate_currency")
        @Pattern(regexp = "^[A-Za-z]{3}$", message = "Rate currency must be a 3-letter ISO code")
        String rateCurrency,

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
