package apps.sarafrika.elimika.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(
        description = "Represents a prerequisite requirement for a course",
        name = "Prerequisite"
)
@Builder
public record PrerequisiteDTO(

        @JsonProperty("uuid")
        @Schema(
                description = "Unique identifier for the prerequisite",
                example = "123e4567-e89b-12d3-a456-426614174000",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        UUID uuid,

        @NotNull(message = "Course UUID is required")
        @JsonProperty("course_uuid")
        @Schema(
                description = "UUID of the course that is a prerequisite",
                example = "456e7890-e89b-12d3-a456-426614174000",
                requiredMode = Schema.RequiredMode.REQUIRED,
                externalDocs = @io.swagger.v3.oas.annotations.ExternalDocumentation(
                        description = "Learn more about courses",
                        url = "/api/docs#tag/Courses"
                )
        )
        UUID courseUuid,

        @NotNull(message = "Required for course UUID is required")
        @JsonProperty("required_for_course_uuid")
        @Schema(
                description = "UUID of the course that requires this prerequisite",
                example = "789e0123-e89b-12d3-a456-426614174000",
                requiredMode = Schema.RequiredMode.REQUIRED,
                externalDocs = @io.swagger.v3.oas.annotations.ExternalDocumentation(
                        description = "Learn more about courses",
                        url = "/api/docs#tag/Courses"
                )
        )
        UUID requiredForCourseUuid,

        @DecimalMin(value = "0.0", inclusive = true, message = "Minimum score must be zero or positive")
        @JsonProperty("minimum_score")
        @Schema(
                description = "Minimum score required to satisfy this prerequisite",
                example = "70.5",
                minimum = "0.0"
        )
        BigDecimal minimumScore,

        @NotNull(message = "Prerequisite type UUID is required")
        @JsonProperty("prerequisite_type_uuid")
        @Schema(
                description = "UUID of the prerequisite type (e.g., course completion, assessment, etc.)",
                example = "321e6547-e89b-12d3-a456-426614174000",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        UUID prerequisiteTypeUuid,

        @JsonProperty(value = "created_by", access = JsonProperty.Access.READ_ONLY)
        @Schema(
                description = "Username of the user who created this prerequisite",
                example = "john.doe",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        String createdBy,

        @JsonProperty(value = "updated_by", access = JsonProperty.Access.READ_ONLY)
        @Schema(
                description = "Username of the user who last modified this prerequisite",
                example = "jane.smith",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        String lastModifiedBy,

        @JsonProperty(value = "created_date", access = JsonProperty.Access.READ_ONLY)
        @Schema(
                description = "Timestamp when the prerequisite was created",
                example = "2025-03-20T10:30:45.123Z",
                accessMode = Schema.AccessMode.READ_ONLY,
                type = "string",
                format = "date-time"
        )
        LocalDateTime createdDate,

        @JsonProperty(value = "updated_date", access = JsonProperty.Access.READ_ONLY)
        @Schema(
                description = "Timestamp when the prerequisite was last updated",
                example = "2025-03-21T14:15:22.456Z",
                accessMode = Schema.AccessMode.READ_ONLY,
                type = "string",
                format = "date-time"
        )
        LocalDateTime lastModifiedDate

) {
}