package apps.sarafrika.elimika.course.dto;

import apps.sarafrika.elimika.course.util.enums.ContentStatus;
import apps.sarafrika.elimika.course.util.enums.PracticeActivityGrouping;
import apps.sarafrika.elimika.course.util.enums.PracticeActivityType;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(
        name = "LessonPracticeActivity",
        description = "Reusable practice activity template attached to a lesson",
        example = """
        {
            "uuid": "f0f57b88-6a16-4b75-a39d-ecb1a04c2ac6",
            "lesson_uuid": "30b49de6-f266-4a5e-8e81-fd706a601a15",
            "title": "Think-pair-share: API contract review",
            "instructions": "Students review the sample API payload individually, discuss issues in pairs, then share one improvement with the class.",
            "activity_type": "DISCUSSION",
            "grouping": "PAIR",
            "estimated_minutes": 15,
            "materials": ["Sample API payload", "Review checklist"],
            "expected_output": "Each pair identifies one contract risk and one improvement.",
            "display_order": 1,
            "status": "published",
            "active": true,
            "created_date": "2026-04-29T18:04:00",
            "created_by": "creator@sarafrika.com",
            "updated_date": "2026-04-29T18:30:00",
            "updated_by": "creator@sarafrika.com",
            "is_published": true,
            "estimated_duration": "15 minutes"
        }
        """
)
public record LessonPracticeActivityDTO(

        @Schema(
                description = "**[READ-ONLY]** Unique system identifier for the practice activity.",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty(value = "uuid", access = JsonProperty.Access.READ_ONLY)
        UUID uuid,

        @Schema(
                description = "**[OPTIONAL]** Lesson UUID. When used through nested lesson endpoints, the path lesson is authoritative.",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        @JsonProperty("lesson_uuid")
        UUID lessonUuid,

        @Schema(
                description = "**[REQUIRED]** Descriptive title of the practice activity.",
                example = "Think-pair-share: API contract review",
                maxLength = 255,
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "Practice activity title is required")
        @Size(max = 255, message = "Title must not exceed 255 characters")
        @JsonProperty("title")
        String title,

        @Schema(
                description = "**[REQUIRED]** Facilitator-facing instructions for running the practice activity.",
                example = "Students review the sample API payload individually, discuss issues in pairs, then share one improvement with the class.",
                maxLength = 5000,
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "Practice activity instructions are required")
        @Size(max = 5000, message = "Instructions must not exceed 5000 characters")
        @JsonProperty("instructions")
        String instructions,

        @Schema(
                description = "**[OPTIONAL]** Practice activity format.",
                example = "DISCUSSION",
                allowableValues = {"EXERCISE", "DISCUSSION", "CASE_STUDY", "ROLE_PLAY", "REFLECTION", "HANDS_ON"}
        )
        @JsonProperty("activity_type")
        PracticeActivityType activityType,

        @Schema(
                description = "**[OPTIONAL]** Student grouping mode for the activity.",
                example = "PAIR",
                allowableValues = {"INDIVIDUAL", "PAIR", "SMALL_GROUP", "WHOLE_CLASS"}
        )
        @JsonProperty("grouping")
        PracticeActivityGrouping grouping,

        @Schema(
                description = "**[OPTIONAL]** Estimated time needed to run the activity.",
                example = "15",
                minimum = "1"
        )
        @Min(value = 1, message = "Estimated minutes must be at least 1")
        @JsonProperty("estimated_minutes")
        Integer estimatedMinutes,

        @Schema(
                description = "**[OPTIONAL]** Materials, handouts, links, or tools needed for the activity.",
                example = "[\"Sample API payload\", \"Review checklist\"]"
        )
        @JsonProperty("materials")
        String[] materials,

        @Schema(
                description = "**[OPTIONAL]** Expected learner output or facilitator debrief artifact.",
                example = "Each pair identifies one contract risk and one improvement.",
                maxLength = 2000
        )
        @Size(max = 2000, message = "Expected output must not exceed 2000 characters")
        @JsonProperty("expected_output")
        String expectedOutput,

        @Schema(
                description = "**[OPTIONAL]** Display order within the lesson. If omitted, the system appends the activity.",
                example = "1",
                minimum = "1"
        )
        @Min(value = 1, message = "Display order must be at least 1")
        @JsonProperty("display_order")
        Integer displayOrder,

        @Schema(
                description = "**[OPTIONAL]** Content lifecycle status.",
                example = "published",
                allowableValues = {"draft", "in_review", "published", "archived"}
        )
        @JsonProperty("status")
        ContentStatus status,

        @Schema(
                description = "**[OPTIONAL]** Whether the practice activity is visible for use. Can only be true when status is published.",
                example = "true"
        )
        @JsonProperty("active")
        Boolean active,

        @Schema(description = "**[READ-ONLY]** Timestamp when the practice activity was created.", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "created_date", access = JsonProperty.Access.READ_ONLY)
        LocalDateTime createdDate,

        @Schema(description = "**[READ-ONLY]** User who created the practice activity.", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "created_by", access = JsonProperty.Access.READ_ONLY)
        String createdBy,

        @Schema(description = "**[READ-ONLY]** Timestamp when the practice activity was last updated.", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "updated_date", access = JsonProperty.Access.READ_ONLY)
        LocalDateTime updatedDate,

        @Schema(description = "**[READ-ONLY]** User who last updated the practice activity.", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "updated_by", access = JsonProperty.Access.READ_ONLY)
        String updatedBy
) {

    @JsonProperty(value = "is_published", access = JsonProperty.Access.READ_ONLY)
    @Schema(description = "**[READ-ONLY]** Whether the activity is published.", accessMode = Schema.AccessMode.READ_ONLY)
    public boolean isPublished() {
        return status == ContentStatus.PUBLISHED;
    }

    @JsonProperty(value = "estimated_duration", access = JsonProperty.Access.READ_ONLY)
    @Schema(description = "**[READ-ONLY]** Human-readable estimated duration.", example = "15 minutes", accessMode = Schema.AccessMode.READ_ONLY)
    public String getEstimatedDuration() {
        if (estimatedMinutes == null) {
            return "Not specified";
        }
        return estimatedMinutes == 1 ? "1 minute" : estimatedMinutes + " minutes";
    }
}
