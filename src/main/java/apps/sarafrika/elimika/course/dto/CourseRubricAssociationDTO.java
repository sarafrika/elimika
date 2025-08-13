package apps.sarafrika.elimika.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Course Rubric Association Data Transfer Object
 * <p>
 * Represents the association between a course and a rubric,
 * supporting rubric reuse across multiple courses.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2024-08-13
 */
@Schema(
        name = "CourseRubricAssociation",
        description = "Association between a course and a rubric for assessment purposes",
        example = """
        {
            "uuid": "a1b2c3d4-5e6f-7g8h-9i10-abcdefghijkl",
            "course_uuid": "c1o2u3r4-5s6e-7d8a-9t10-abcdefghijkl",
            "rubric_uuid": "r1u2b3r4-5i6c-7d8a-9t10-abcdefghijkl",
            "associated_by": "i1n2s3t4-5r6u-7c8t-9o10-abcdefghijkl",
            "association_date": "2024-08-13T10:30:00",
            "is_primary_rubric": true,
            "usage_context": "final_assessment",
            "created_date": "2024-08-13T10:30:00",
            "created_by": "instructor@sarafrika.com",
            "updated_date": "2024-08-13T10:30:00",
            "updated_by": "instructor@sarafrika.com"
        }
        """
)
public record CourseRubricAssociationDTO(

        @Schema(
                description = "**[READ-ONLY]** Unique identifier for this association",
                example = "a1b2c3d4-5e6f-7g8h-9i10-abcdefghijkl",
                accessMode = Schema.AccessMode.READ_ONLY,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        @JsonProperty(value = "uuid", access = JsonProperty.Access.READ_ONLY)
        UUID uuid,

        @Schema(
                description = "**[REQUIRED]** UUID of the course that will use this rubric",
                example = "c1o2u3r4-5s6e-7d8a-9t10-abcdefghijkl",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "Course UUID is required")
        @JsonProperty("course_uuid")
        UUID courseUuid,

        @Schema(
                description = "**[REQUIRED]** UUID of the rubric to be associated with the course",
                example = "r1u2b3r4-5i6c-7d8a-9t10-abcdefghijkl",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "Rubric UUID is required")
        @JsonProperty("rubric_uuid")
        UUID rubricUuid,

        @Schema(
                description = "**[REQUIRED]** UUID of the instructor who created this association",
                example = "i1n2s3t4-5r6u-7c8t-9o10-abcdefghijkl",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "Associated by UUID is required")
        @JsonProperty("associated_by")
        UUID associatedBy,

        @Schema(
                description = "**[READ-ONLY]** Timestamp when this association was created",
                example = "2024-08-13T10:30:00",
                format = "date-time",
                accessMode = Schema.AccessMode.READ_ONLY,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        @JsonProperty(value = "association_date", access = JsonProperty.Access.READ_ONLY)
        LocalDateTime associationDate,

        @Schema(
                description = "**[OPTIONAL]** Whether this is the primary rubric for the course",
                example = "true",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        @JsonProperty("is_primary_rubric")
        Boolean isPrimaryRubric,

        @Schema(
                description = "**[OPTIONAL]** Context of rubric usage (e.g., midterm, final, assignment)",
                example = "final_assessment",
                maxLength = 100,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        @Size(max = 100, message = "Usage context must not exceed 100 characters")
        @JsonProperty("usage_context")
        String usageContext,

        @Schema(
                description = "**[READ-ONLY]** Timestamp when the association was created",
                example = "2024-08-13T10:30:00",
                format = "date-time",
                accessMode = Schema.AccessMode.READ_ONLY,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        @JsonProperty(value = "created_date", access = JsonProperty.Access.READ_ONLY)
        LocalDateTime createdDate,

        @Schema(
                description = "**[READ-ONLY]** User who created this association",
                example = "instructor@sarafrika.com",
                accessMode = Schema.AccessMode.READ_ONLY,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        @JsonProperty(value = "created_by", access = JsonProperty.Access.READ_ONLY)
        String createdBy,

        @Schema(
                description = "**[READ-ONLY]** Timestamp when the association was last updated",
                example = "2024-08-13T10:30:00",
                format = "date-time",
                accessMode = Schema.AccessMode.READ_ONLY,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        @JsonProperty(value = "updated_date", access = JsonProperty.Access.READ_ONLY)
        LocalDateTime updatedDate,

        @Schema(
                description = "**[READ-ONLY]** User who last updated this association",
                example = "instructor@sarafrika.com",
                accessMode = Schema.AccessMode.READ_ONLY,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        @JsonProperty(value = "updated_by", access = JsonProperty.Access.READ_ONLY)
        String updatedBy
) {
}