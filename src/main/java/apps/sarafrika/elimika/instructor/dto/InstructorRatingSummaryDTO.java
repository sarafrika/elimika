package apps.sarafrika.elimika.instructor.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(
        name = "InstructorRatingSummary",
        description = "Aggregate review metrics for an instructor (average rating and review count).",
        example = """
        {
          "instructor_uuid": "inst-1234-5678-90ab-cdef12345678",
          "average_rating": 4.7,
          "review_count": 12
        }
        """
)
public record InstructorRatingSummaryDTO(

        @Schema(
                description = "UUID of the instructor.",
                example = "inst-1234-5678-90ab-cdef12345678"
        )
        @JsonProperty("instructor_uuid")
        UUID instructorUuid,

        @Schema(
                description = "Average overall rating across all reviews (1-5). Null when there are no reviews.",
                example = "4.7",
                nullable = true
        )
        @JsonProperty("average_rating")
        Double averageRating,

        @Schema(
                description = "Total number of reviews for this instructor.",
                example = "12"
        )
        @JsonProperty("review_count")
        Long reviewCount
) {
}

