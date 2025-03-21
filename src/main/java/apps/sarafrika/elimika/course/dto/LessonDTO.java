package apps.sarafrika.elimika.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Schema(
        description = "Represents a lesson within a course, containing educational content and resources",
        name = "Lesson"
)
@Builder
public record LessonDTO(

        @JsonProperty("uuid")
        @Schema(
                description = "Unique identifier for the lesson",
                example = "123e4567-e89b-12d3-a456-426614174000",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        UUID uuid,

        @NotBlank(message = "Title is required")
        @Size(max = 255, message = "Title must not exceed 255 characters")
        @JsonProperty("title")
        @Schema(
                description = "Title of the lesson displayed to students",
                example = "Introduction to Java Programming",
                requiredMode = Schema.RequiredMode.REQUIRED,
                maxLength = 255
        )
        String title,

        @Size(max = 1000, message = "Description must not exceed 1000 characters")
        @JsonProperty("description")
        @Schema(
                description = "Brief overview of the lesson content and learning objectives",
                example = "This lesson introduces basic Java syntax and object-oriented programming concepts",
                maxLength = 1000
        )
        String description,

        @PositiveOrZero(message = "Lesson order must be zero or a positive number")
        @JsonProperty("lesson_order")
        @Schema(
                description = "Position of this lesson in the course curriculum (0-based)",
                example = "2",
                defaultValue = "0",
                minimum = "0"
        )
        int lessonOrder,

        @JsonProperty("is_published")
        @Schema(
                description = "Indicates whether the lesson is visible to students",
                example = "true",
                defaultValue = "false"
        )
        boolean isPublished,

        @JsonProperty("course_uuid")
        @Schema(
                description = "UUID of the parent course this lesson belongs to",
                example = "456e7890-e89b-12d3-a456-426614174000",
                requiredMode = Schema.RequiredMode.REQUIRED,
                externalDocs = @io.swagger.v3.oas.annotations.ExternalDocumentation(
                        description = "Learn more about courses",
                        url = "/api/docs#tag/Courses"
                )
        )
        UUID courseUuid,

        @JsonProperty("content")
        @ArraySchema(
                schema = @Schema(implementation = LessonContentDTO.class),
                arraySchema = @Schema(
                        description = "List of content items (text, video, etc.) in this lesson"
                ),
                minItems = 0
        )
        List<LessonContentDTO> content,

        @JsonProperty("resources")
        @ArraySchema(
                schema = @Schema(implementation = LessonResourceDTO.class),
                arraySchema = @Schema(
                        description = "List of supplementary resources for this lesson"
                ),
                minItems = 0
        )
        List<LessonResourceDTO> resources,

        @JsonProperty(value = "created_by", access = JsonProperty.Access.READ_ONLY)
        @Schema(
                description = "Username of the user who created this lesson",
                example = "john.doe",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        String createdBy,

        @JsonProperty(value = "updated_by", access = JsonProperty.Access.READ_ONLY)
        @Schema(
                description = "Username of the user who last modified this lesson",
                example = "jane.smith",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        String lastModifiedBy,

        @JsonProperty(value = "created_date", access = JsonProperty.Access.READ_ONLY)
        @Schema(
                description = "Timestamp when the lesson was created",
                example = "2025-03-20T10:30:45.123Z",
                accessMode = Schema.AccessMode.READ_ONLY,
                type = "string",
                format = "date-time"
        )
        LocalDateTime createdDate,

        @JsonProperty(value = "updated_date", access = JsonProperty.Access.READ_ONLY)
        @Schema(
                description = "Timestamp when the lesson was last updated",
                example = "2025-03-21T14:15:22.456Z",
                accessMode = Schema.AccessMode.READ_ONLY,
                type = "string",
                format = "date-time"
        )
        LocalDateTime lastModifiedDate

) {
}