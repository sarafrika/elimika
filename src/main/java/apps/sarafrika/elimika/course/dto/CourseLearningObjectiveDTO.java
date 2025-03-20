package apps.sarafrika.elimika.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(
        description = "Represents a learning objective or outcome for a course",
        name = "CourseLearningObjective"
)
@Builder
public record CourseLearningObjectiveDTO(

        @JsonProperty("uuid")
        @Schema(
                description = "Unique identifier for the course learning objective",
                example = "123e4567-e89b-12d3-a456-426614174000",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        UUID uuid,

        @JsonProperty("course_uuid")
        @Schema(
                description = "UUID of the course this learning objective belongs to",
                example = "456e7890-e89b-12d3-a456-426614174000",
                requiredMode = Schema.RequiredMode.REQUIRED,
                externalDocs = @io.swagger.v3.oas.annotations.ExternalDocumentation(
                        description = "Learn more about courses",
                        url = "/api/docs#tag/Courses"
                )
        )
        UUID courseUuid,

        @NotBlank(message = "Objective is required")
        @Size(max = 500, message = "Objective must not exceed 500 characters")
        @JsonProperty("objective")
        @Schema(
                description = "Statement describing what students will be able to do after completing the course",
                example = "Develop and deploy Spring Boot REST APIs with proper security and documentation",
                requiredMode = Schema.RequiredMode.REQUIRED,
                maxLength = 500
        )
        String objective,

        @JsonProperty(value = "created_by", access = JsonProperty.Access.READ_ONLY)
        @Schema(
                description = "Username of the user who created this learning objective",
                example = "john.doe",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        String createdBy,

        @JsonProperty(value = "updated_by", access = JsonProperty.Access.READ_ONLY)
        @Schema(
                description = "Username of the user who last modified this learning objective",
                example = "jane.smith",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        String lastModifiedBy,

        @JsonProperty(value = "created_date", access = JsonProperty.Access.READ_ONLY)
        @Schema(
                description = "Timestamp when the learning objective was created",
                example = "2025-03-20T10:30:45.123Z",
                accessMode = Schema.AccessMode.READ_ONLY,
                type = "string",
                format = "date-time"
        )
        LocalDateTime createdDate,

        @JsonProperty(value = "updated_date", access = JsonProperty.Access.READ_ONLY)
        @Schema(
                description = "Timestamp when the learning objective was last updated",
                example = "2025-03-21T14:15:22.456Z",
                accessMode = Schema.AccessMode.READ_ONLY,
                type = "string",
                format = "date-time"
        )
        LocalDateTime lastModifiedDate

) {
}