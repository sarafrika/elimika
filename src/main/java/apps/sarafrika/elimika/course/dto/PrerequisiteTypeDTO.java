package apps.sarafrika.elimika.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(
        description = "Represents a type of prerequisite that can be applied to courses",
        name = "PrerequisiteType"
)
@Builder
public record PrerequisiteTypeDTO(

        @JsonProperty("uuid")
        @Schema(
                description = "Unique identifier for the prerequisite type",
                example = "123e4567-e89b-12d3-a456-426614174000",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        UUID uuid,

        @NotBlank(message = "Name is required")
        @Size(max = 255, message = "Name must not exceed 255 characters")
        @JsonProperty("name")
        @Schema(
                description = "Name of the prerequisite type",
                example = "Course Completion",
                requiredMode = Schema.RequiredMode.REQUIRED,
                maxLength = 255
        )
        String name,

        @JsonProperty(value = "created_by", access = JsonProperty.Access.READ_ONLY)
        @Schema(
                description = "Username of the user who created this prerequisite type",
                example = "john.doe",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        String createdBy,

        @JsonProperty(value = "updated_by", access = JsonProperty.Access.READ_ONLY)
        @Schema(
                description = "Username of the user who last modified this prerequisite type",
                example = "jane.smith",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        String lastModifiedBy,

        @JsonProperty(value = "created_date", access = JsonProperty.Access.READ_ONLY)
        @Schema(
                description = "Timestamp when the prerequisite type was created",
                example = "2025-03-20T10:30:45.123Z",
                accessMode = Schema.AccessMode.READ_ONLY,
                type = "string",
                format = "date-time"
        )
        LocalDateTime createdDate,

        @JsonProperty(value = "updated_date", access = JsonProperty.Access.READ_ONLY)
        @Schema(
                description = "Timestamp when the prerequisite type was last updated",
                example = "2025-03-21T14:15:22.456Z",
                accessMode = Schema.AccessMode.READ_ONLY,
                type = "string",
                format = "date-time"
        )
        LocalDateTime lastModifiedDate

) {
}