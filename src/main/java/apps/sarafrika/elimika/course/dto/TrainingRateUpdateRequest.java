package apps.sarafrika.elimika.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** An approved applicant's proposed card: the complete card after the change, validated like a new application's. */
@Schema(
        name = "TrainingRateUpdateRequest",
        description = "Proposes a replacement rate card on an approved training application for the course creator to approve",
        example = """
        {
          "rate_card": {
            "currency": "KES",
            "private_online_hourly_rate": null,
            "private_online_session_rate": null,
            "private_online_daily_rate": null,
            "group_online_hourly_rate": 3000.0000,
            "group_online_session_rate": 5000.0000,
            "group_online_daily_rate": 12000.0000
          },
          "note": "Venue costs rose this term."
        }
        """
)
public record TrainingRateUpdateRequest(

        @Schema(description = "**[REQUIRED]** The full rate card as it should read once approved.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("rate_card")
        @NotNull(message = "Rate card is required")
        @Valid
        CourseTrainingRateCardDTO rateCard,

        @Schema(description = "Why the rates are changing, for the course creator.", maxLength = 2000, nullable = true)
        @JsonProperty("note")
        @Size(max = 2000, message = "Note must not exceed 2000 characters")
        String note
) {
}
