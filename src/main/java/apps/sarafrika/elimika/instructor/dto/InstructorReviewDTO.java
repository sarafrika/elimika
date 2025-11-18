package apps.sarafrika.elimika.instructor.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(
        name = "InstructorReview",
        description = "Student review and rating for an instructor, scoped to a specific enrollment.",
        example = """
        {
          "uuid": "rev-1234-5678-90ab-cdef12345678",
          "instructor_uuid": "inst-1234-5678-90ab-cdef12345678",
          "student_uuid": "stud-1234-5678-90ab-cdef12345678",
          "enrollment_uuid": "enr-1234-5678-90ab-cdef12345678",
          "rating": 5,
          "headline": "Incredible instructor!",
          "comments": "Very clear explanations and engaging sessions.",
          "clarity_rating": 5,
          "engagement_rating": 5,
          "punctuality_rating": 4,
          "is_anonymous": false,
          "created_date": "2025-11-18T09:00:00",
          "created_by": "student@example.com",
          "updated_date": "2025-11-18T09:00:00",
          "updated_by": "student@example.com"
        }
        """
)
public record InstructorReviewDTO(

        @Schema(
                description = "**[READ-ONLY]** Unique identifier for the review.",
                example = "rev-1234-5678-90ab-cdef12345678",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty(value = "uuid", access = JsonProperty.Access.READ_ONLY)
        UUID uuid,

        @Schema(
                description = "**[REQUIRED]** Instructor being reviewed.",
                example = "inst-1234-5678-90ab-cdef12345678"
        )
        @NotNull(message = "Instructor UUID is required")
        @JsonProperty("instructor_uuid")
        UUID instructorUuid,

        @Schema(
                description = "**[REQUIRED]** Student leaving the review.",
                example = "stud-1234-5678-90ab-cdef12345678"
        )
        @NotNull(message = "Student UUID is required")
        @JsonProperty("student_uuid")
        UUID studentUuid,

        @Schema(
                description = "**[REQUIRED]** Enrollment that this review is tied to.",
                example = "enr-1234-5678-90ab-cdef12345678"
        )
        @NotNull(message = "Enrollment UUID is required")
        @JsonProperty("enrollment_uuid")
        UUID enrollmentUuid,

        @Schema(
                description = "Overall rating for the instructor (1-5).",
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
                example = "Incredible instructor!",
                maxLength = 255
        )
        @Size(max = 255, message = "Headline must not exceed 255 characters")
        @JsonProperty("headline")
        String headline,

        @Schema(
                description = "Detailed feedback from the student.",
                example = "Very clear explanations and engaging sessions.",
                maxLength = 5000
        )
        @Size(max = 5000, message = "Comments must not exceed 5000 characters")
        @JsonProperty("comments")
        String comments,

        @Schema(
                description = "Optional clarity rating (1-5).",
                minimum = "1",
                maximum = "5",
                nullable = true
        )
        @Min(value = 1, message = "Clarity rating must be at least 1")
        @Max(value = 5, message = "Clarity rating must be at most 5")
        @JsonProperty("clarity_rating")
        Integer clarityRating,

        @Schema(
                description = "Optional engagement rating (1-5).",
                minimum = "1",
                maximum = "5",
                nullable = true
        )
        @Min(value = 1, message = "Engagement rating must be at least 1")
        @Max(value = 5, message = "Engagement rating must be at most 5")
        @JsonProperty("engagement_rating")
        Integer engagementRating,

        @Schema(
                description = "Optional punctuality rating (1-5).",
                minimum = "1",
                maximum = "5",
                nullable = true
        )
        @Min(value = 1, message = "Punctuality rating must be at least 1")
        @Max(value = 5, message = "Punctuality rating must be at most 5")
        @JsonProperty("punctuality_rating")
        Integer punctualityRating,

        @Schema(
                description = "Whether the review should be shown anonymously in public views.",
                example = "false"
        )
        @JsonProperty("is_anonymous")
        Boolean isAnonymous,

        @Schema(
                description = "**[READ-ONLY]** Timestamp when the review was created.",
                example = "2025-11-18T09:00:00",
                format = "date-time",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty(value = "created_date", access = JsonProperty.Access.READ_ONLY)
        LocalDateTime createdDate,

        @Schema(
                description = "**[READ-ONLY]** Created by identifier (typically the student email or system).",
                example = "student@example.com",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty(value = "created_by", access = JsonProperty.Access.READ_ONLY)
        String createdBy,

        @Schema(
                description = "**[READ-ONLY]** Timestamp when the review was last updated.",
                example = "2025-11-18T09:00:00",
                format = "date-time",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty(value = "updated_date", access = JsonProperty.Access.READ_ONLY)
        LocalDateTime updatedDate,

        @Schema(
                description = "**[READ-ONLY]** Updated by identifier.",
                example = "student@example.com",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty(value = "updated_by", access = JsonProperty.Access.READ_ONLY)
        String updatedBy
) {
}
