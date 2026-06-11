package apps.sarafrika.elimika.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(
        name = "ProgramRatingSummary",
        description = "Aggregate review metrics for a training program.",
        example = """
        {
          "program_uuid": "640d0a57-76cc-46f2-ad46-72f5635d973a",
          "average_rating": 4.7,
          "review_count": 12
        }
        """
)
public record ProgramRatingSummaryDTO(

        @Schema(
                description = "UUID of the training program.",
                example = "640d0a57-76cc-46f2-ad46-72f5635d973a"
        )
        @JsonProperty("program_uuid")
        UUID programUuid,

        @Schema(
                description = "Average overall rating across all program reviews (1-5). Null when there are no reviews.",
                example = "4.7",
                nullable = true
        )
        @JsonProperty("average_rating")
        Double averageRating,

        @Schema(
                description = "Total number of reviews for this program.",
                example = "12"
        )
        @JsonProperty("review_count")
        Long reviewCount
) {
}
