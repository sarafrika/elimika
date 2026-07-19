package apps.sarafrika.elimika.classes.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(
        name = "ClassReview",
        description = "Student review and rating for a class.",
        example = """
        {
          "uuid": "d41707bc-e652-4ea4-8db0-ea9c85ff7d5c",
          "class_definition_uuid": "640d0a57-76cc-46f2-ad46-72f5635d973a",
          "student_uuid": "4d91801f-0d0f-4078-9b70-7f68f7531c8a",
          "rating": 5,
          "headline": "Practical class",
          "comments": "The class session was clear and hands-on.",
          "is_anonymous": false,
          "created_date": "2026-06-22T17:00:00",
          "created_by": "student@example.com",
          "updated_date": "2026-06-22T17:00:00",
          "updated_by": "student@example.com"
        }
        """
)
public record ClassReviewDTO(

        @Schema(
                description = "**[READ-ONLY]** Unique identifier for the review.",
                example = "d41707bc-e652-4ea4-8db0-ea9c85ff7d5c",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty(value = "uuid", access = JsonProperty.Access.READ_ONLY)
        UUID uuid,

        @Schema(
                description = "**[READ-ONLY]** Class definition being reviewed.",
                example = "640d0a57-76cc-46f2-ad46-72f5635d973a",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty(value = "class_definition_uuid", access = JsonProperty.Access.READ_ONLY)
        UUID classDefinitionUuid,

        @Schema(
                description = "**[READ-ONLY]** Student who left the review. Null for anonymous public responses.",
                example = "4d91801f-0d0f-4078-9b70-7f68f7531c8a",
                nullable = true,
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty(value = "student_uuid", access = JsonProperty.Access.READ_ONLY)
        UUID studentUuid,

        @Schema(
                description = "Overall rating for the class (1-5).",
                example = "5",
                minimum = "1",
                maximum = "5"
        )
        @JsonProperty("rating")
        Integer rating,

        @Schema(
                description = "Optional short headline for the review.",
                example = "Practical class",
                maxLength = 255
        )
        @JsonProperty("headline")
        String headline,

        @Schema(
                description = "Detailed feedback from the student.",
                example = "The class session was clear and hands-on.",
                maxLength = 5000
        )
        @JsonProperty("comments")
        String comments,

        @Schema(
                description = "Whether the review should be shown anonymously in public views.",
                example = "false"
        )
        @JsonProperty("is_anonymous")
        Boolean isAnonymous,

        @Schema(
                description = "**[READ-ONLY]** Timestamp when the review was created.",
                example = "2026-06-22T17:00:00Z",
                format = "date-time",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty(value = "created_date", access = JsonProperty.Access.READ_ONLY)
        LocalDateTime createdDate,

        @Schema(
                description = "**[READ-ONLY]** Created by identifier. Null for anonymous public responses.",
                example = "student@example.com",
                nullable = true,
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty(value = "created_by", access = JsonProperty.Access.READ_ONLY)
        String createdBy,

        @Schema(
                description = "**[READ-ONLY]** Timestamp when the review was last updated.",
                example = "2026-06-22T17:00:00Z",
                format = "date-time",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty(value = "updated_date", access = JsonProperty.Access.READ_ONLY)
        LocalDateTime updatedDate,

        @Schema(
                description = "**[READ-ONLY]** Updated by identifier. Null for anonymous public responses.",
                example = "student@example.com",
                nullable = true,
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty(value = "updated_by", access = JsonProperty.Access.READ_ONLY)
        String updatedBy
) {
}
