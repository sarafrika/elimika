package apps.sarafrika.elimika.course.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request payload used by an applicant to update their own PENDING program training application.
 */
@Schema(
        name = "ProgramTrainingApplicationUpdateRequest",
        description = "Payload for an applicant editing the rate card or notes on a pending program training application",
        example = """
        {
          "rate_card": {
            "currency": "KES",
            "private_online_rate": 3500.0000,
            "private_inperson_rate": 3600.0000,
            "group_online_rate": 2800.0000,
            "group_inperson_rate": 3000.0000
          },
          "application_notes": "Updated availability for the January cohort."
        }
        """
)
public record ProgramTrainingApplicationUpdateRequest(

        @Schema(
                description = "**[REQUIRED]** Updated rate card across session format and delivery modality combinations.",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @JsonProperty("rate_card")
        @NotNull(message = "Rate card is required")
        @Valid
        CourseTrainingRateCardDTO rateCard,

        @Schema(
                description = "Optional notes to help the program creator evaluate the request.",
                maxLength = 2000,
                nullable = true
        )
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonProperty("application_notes")
        @Size(max = 2000, message = "Application notes must not exceed 2000 characters")
        String applicationNotes
) {
}
