package apps.sarafrika.elimika.course.dto;

import apps.sarafrika.elimika.course.model.PrerequisiteGroup;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(
        description = "Represents a logical grouping of course prerequisites",
        name = "PrerequisiteGroup"
)
@Builder
public record PrerequisiteGroupDTO(

        @JsonProperty("uuid")
        @Schema(
                description = "Unique identifier for the prerequisite group",
                example = "123e4567-e89b-12d3-a456-426614174000",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        UUID uuid,

        @NotNull(message = "Course UUID is required")
        @JsonProperty("course_uuid")
        @Schema(
                description = "UUID of the course this prerequisite group belongs to",
                example = "456e7890-e89b-12d3-a456-426614174000",
                requiredMode = Schema.RequiredMode.REQUIRED,
                externalDocs = @io.swagger.v3.oas.annotations.ExternalDocumentation(
                        description = "Learn more about courses",
                        url = "/api/docs#tag/Courses"
                )
        )
        UUID courseUuid,

        @NotNull(message = "Group type is required")
        @JsonProperty("group_type")
        @Schema(
                description = "Logical operator for this prerequisite group (AND/OR)",
                example = "AND",
                requiredMode = Schema.RequiredMode.REQUIRED,
                allowableValues = {"AND", "OR"}
        )
        PrerequisiteGroup.GroupType groupType,

        @JsonProperty(value = "created_by", access = JsonProperty.Access.READ_ONLY)
        @Schema(
                description = "Username of the user who created this prerequisite group",
                example = "john.doe",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        String createdBy,

        @JsonProperty(value = "updated_by", access = JsonProperty.Access.READ_ONLY)
        @Schema(
                description = "Username of the user who last modified this prerequisite group",
                example = "jane.smith",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        String lastModifiedBy,

        @JsonProperty(value = "created_date", access = JsonProperty.Access.READ_ONLY)
        @Schema(
                description = "Timestamp when the prerequisite group was created",
                example = "2025-03-20T10:30:45.123Z",
                accessMode = Schema.AccessMode.READ_ONLY,
                type = "string",
                format = "date-time"
        )
        LocalDateTime createdDate,

        @JsonProperty(value = "updated_date", access = JsonProperty.Access.READ_ONLY)
        @Schema(
                description = "Timestamp when the prerequisite group was last updated",
                example = "2025-03-21T14:15:22.456Z",
                accessMode = Schema.AccessMode.READ_ONLY,
                type = "string",
                format = "date-time"
        )
        LocalDateTime lastModifiedDate
) {
}