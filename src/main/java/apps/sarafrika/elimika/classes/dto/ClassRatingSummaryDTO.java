package apps.sarafrika.elimika.classes.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(
        name = "ClassRatingSummary",
        description = "Aggregate review metrics for a class.",
        example = """
        {
          "class_definition_uuid": "640d0a57-76cc-46f2-ad46-72f5635d973a",
          "average_rating": 4.7,
          "review_count": 12
        }
        """
)
public record ClassRatingSummaryDTO(

        @Schema(
                description = "UUID of the class definition.",
                example = "640d0a57-76cc-46f2-ad46-72f5635d973a"
        )
        @JsonProperty("class_definition_uuid")
        UUID classDefinitionUuid,

        @Schema(
                description = "Average overall rating across all class reviews (1-5). Null when there are no reviews.",
                example = "4.7",
                nullable = true
        )
        @JsonProperty("average_rating")
        Double averageRating,

        @Schema(
                description = "Total number of reviews for this class.",
                example = "12"
        )
        @JsonProperty("review_count")
        Long reviewCount
) {
}
