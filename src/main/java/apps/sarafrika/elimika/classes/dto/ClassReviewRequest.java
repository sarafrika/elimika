package apps.sarafrika.elimika.classes.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

@Schema(
        name = "ClassReviewRequest",
        description = "Payload for submitting or updating a student review for a class.",
        example = """
        {
          "student_uuid": "4d91801f-0d0f-4078-9b70-7f68f7531c8a",
          "rating": 5,
          "headline": "Practical class",
          "comments": "The class session was clear and hands-on.",
          "is_anonymous": false
        }
        """
)
public record ClassReviewRequest(

        @Schema(
                description = "**[REQUIRED]** Student leaving the review.",
                example = "4d91801f-0d0f-4078-9b70-7f68f7531c8a",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "Student UUID is required")
        @JsonProperty("student_uuid")
        UUID studentUuid,

        @Schema(
                description = "**[REQUIRED]** Overall rating for the class (1-5).",
                example = "5",
                minimum = "1",
                maximum = "5",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "Rating is required")
        @Min(value = 1, message = "Rating must be at least 1")
        @Max(value = 5, message = "Rating must be at most 5")
        @JsonProperty("rating")
        Integer rating,

        @Schema(
                description = "Optional short headline for the review.",
                example = "Practical class",
                maxLength = 255
        )
        @Size(max = 255, message = "Headline must not exceed 255 characters")
        @JsonProperty("headline")
        String headline,

        @Schema(
                description = "Detailed feedback from the student.",
                example = "The class session was clear and hands-on.",
                maxLength = 5000
        )
        @Size(max = 5000, message = "Comments must not exceed 5000 characters")
        @JsonProperty("comments")
        String comments,

        @Schema(
                description = "Whether the review should be shown anonymously in public views.",
                example = "false"
        )
        @JsonProperty("is_anonymous")
        Boolean isAnonymous
) {
}
