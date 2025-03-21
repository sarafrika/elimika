package apps.sarafrika.elimika.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(
        description = "Data transfer object representing a digital resource attached to a lesson",
        name = "LessonResource"
)
@Builder
public record LessonResourceDTO(

        @JsonProperty("uuid")
        @Schema(
                description = "Unique identifier for the lesson resource",
                example = "123e4567-e89b-12d3-a456-426614174000",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        UUID uuid,

        @NotBlank(message = "Title is required")
        @Size(max = 255, message = "Title must not exceed 255 characters")
        @JsonProperty("title")
        @Schema(
                description = "Title of the resource that will be displayed to students",
                example = "Introduction to Java Programming",
                requiredMode = Schema.RequiredMode.REQUIRED,
                maxLength = 255
        )
        String title,

        @NotBlank(message = "Resource URL is required")
        @Size(max = 2083, message = "Resource URL must not exceed 2083 characters")
        @JsonProperty("resource_url")
        @Schema(
                description = "URL to the learning resource (video, document, etc.)",
                example = "https://example.com/learn-java/intro-video",
                requiredMode = Schema.RequiredMode.REQUIRED,
                maxLength = 2083,
                pattern = "^(https?|ftp)://.*$"
        )
        String resourceUrl,

        @PositiveOrZero(message = "Display order must be zero or a positive number")
        @JsonProperty("display_order")
        @Schema(
                description = "Position of this resource in the lesson's resource list (0-based)",
                example = "2",
                defaultValue = "0",
                minimum = "0"
        )
        int displayOrder,

        @JsonProperty("lesson_uuid")
        @Schema(
                description = "ID of the parent lesson this resource belongs to",
                example = "123e4567-e89b-12d3-a456-426614174000",
                requiredMode = Schema.RequiredMode.REQUIRED,
                externalDocs = @io.swagger.v3.oas.annotations.ExternalDocumentation(
                        description = "Learn more about lessons",
                        url = "/api/docs#tag/Lessons"
                )
        )
        UUID lessonUuid,

        @JsonProperty(value = "created_by", access = JsonProperty.Access.READ_ONLY)
        @Schema(
                description = "Username of the user who created this resource",
                example = "john.doe",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        String createdBy,

        @JsonProperty(value = "updated_by", access = JsonProperty.Access.READ_ONLY)
        @Schema(
                description = "Username of the user who last modified this resource",
                example = "jane.smith",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        String lastModifiedBy,

        @JsonProperty(value = "created_date", access = JsonProperty.Access.READ_ONLY)
        @Schema(
                description = "Timestamp when the resource was created",
                example = "2025-03-20T10:30:45.123Z",
                accessMode = Schema.AccessMode.READ_ONLY,
                type = "string",
                format = "date-time"
        )
        LocalDateTime createdDate,

        @JsonProperty(value = "updated_date", access = JsonProperty.Access.READ_ONLY)
        @Schema(
                description = "Timestamp when the resource was last updated",
                example = "2025-03-21T14:15:22.456Z",
                accessMode = Schema.AccessMode.READ_ONLY,
                type = "string",
                format = "date-time"
        )
        LocalDateTime lastModifiedDate

) {
}