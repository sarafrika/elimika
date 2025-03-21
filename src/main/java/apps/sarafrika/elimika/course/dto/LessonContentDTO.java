package apps.sarafrika.elimika.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(
        description = "Represents a content item within a lesson such as text, video, or interactive element",
        name = "LessonContent"
)
@Builder
public record LessonContentDTO(

        @JsonProperty("uuid")
        @Schema(
                description = "Unique identifier for the lesson content item",
                example = "123e4567-e89b-12d3-a456-426614174000",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        UUID uuid,

        @NotBlank(message = "Title is required")
        @Size(max = 255, message = "Title must not exceed 255 characters")
        @JsonProperty("title")
        @Schema(
                description = "Title of the content section",
                example = "Understanding Variables in Java",
                requiredMode = Schema.RequiredMode.REQUIRED,
                maxLength = 255
        )
        String title,

        @NotBlank(message = "Content is required")
        @JsonProperty("content")
        @Schema(
                description = "The actual content - could be formatted text, HTML, markdown or other supported format",
                example = "Variables in Java are containers that store data values. A variable is assigned with a data type...",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String content,

        @PositiveOrZero(message = "Display order must be zero or a positive number")
        @JsonProperty("display_order")
        @Schema(
                description = "Position of this content item within the lesson (0-based)",
                example = "2",
                defaultValue = "0",
                minimum = "0"
        )
        int displayOrder,

        @Min(value = 0, message = "Duration must be zero or a positive number")
        @JsonProperty("duration")
        @Schema(
                description = "Estimated time in minutes to complete this content item",
                example = "15",
                minimum = "0"
        )
        BigDecimal duration,

        @JsonProperty("lesson_uuid")
        @Schema(
                description = "UUID of the parent lesson this content belongs to",
                example = "123e4567-e89b-12d3-a456-426614174000",
                requiredMode = Schema.RequiredMode.REQUIRED,
                externalDocs = @io.swagger.v3.oas.annotations.ExternalDocumentation(
                        description = "Learn more about lessons",
                        url = "/api/docs#tag/Lessons"
                )
        )
        UUID lessonUuid,

        @JsonProperty("content_type_uuid")
        @Schema(
                description = "UUID representing the type of content (e.g., text, video, quiz, etc.)",
                example = "123e4567-e89b-12d3-a456-426614174000",
                requiredMode = Schema.RequiredMode.REQUIRED,
                externalDocs = @io.swagger.v3.oas.annotations.ExternalDocumentation(
                        description = "Available content types",
                        url = "/api/docs#tag/ContentTypes"
                )
        )
        UUID contentTypeUuid,

        @JsonProperty(value = "created_by", access = JsonProperty.Access.READ_ONLY)
        @Schema(
                description = "Username of the user who created this content item",
                example = "john.doe",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        String createdBy,

        @JsonProperty(value = "updated_by", access = JsonProperty.Access.READ_ONLY)
        @Schema(
                description = "Username of the user who last modified this content item",
                example = "jane.smith",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        String lastModifiedBy,

        @JsonProperty(value = "created_date", access = JsonProperty.Access.READ_ONLY)
        @Schema(
                description = "Timestamp when the content item was created",
                example = "2025-03-20T10:30:45.123Z",
                accessMode = Schema.AccessMode.READ_ONLY,
                type = "string",
                format = "date-time"
        )
        LocalDateTime createdDate,

        @JsonProperty(value = "updated_date", access = JsonProperty.Access.READ_ONLY)
        @Schema(
                description = "Timestamp when the content item was last updated",
                example = "2025-03-21T14:15:22.456Z",
                accessMode = Schema.AccessMode.READ_ONLY,
                type = "string",
                format = "date-time"
        )
        LocalDateTime lastModifiedDate

) {
}