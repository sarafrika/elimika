package apps.sarafrika.elimika.course.dto;

import apps.sarafrika.elimika.course.util.enums.CourseTrainingRequirementProvider;
import apps.sarafrika.elimika.course.util.enums.CourseTrainingRequirementType;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(
        name = "CourseTrainingRequirement",
        description = "Operational resources that must be available when delivering the course (materials, equipment, facilities).",
        example = """
        {
            "uuid": "5a8074cc-8893-497b-8d58-4b151c994a80",
            "course_uuid": "c1o2u3r4-5s6e-7d8a-9t10-abcdefghijkl",
            "requirement_type": "equipment",
            "name": "Dual-screen instructor workstation",
            "description": "Instructor requires dual monitors with HDMI input and adjustable desk mount.",
            "quantity": 1,
            "unit": "workstation",
            "provided_by": "organisation",
            "is_mandatory": true
        }
        """
)
public record CourseTrainingRequirementDTO(

        @Schema(
                description = "**[READ-ONLY]** Identifier for this training requirement.",
                format = "uuid",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty(value = "uuid", access = JsonProperty.Access.READ_ONLY)
        UUID uuid,

        @Schema(
                description = "**[REQUIRED]** Course identifier this requirement belongs to.",
                example = "c1o2u3r4-5s6e-7d8a-9t10-abcdefghijkl",
                format = "uuid",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "Course UUID is required")
        @JsonProperty("course_uuid")
        UUID courseUuid,

        @Schema(
                description = "**[REQUIRED]** Resource category.",
                example = "equipment",
                allowableValues = {"material", "equipment", "facility", "other"},
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "Requirement type is required")
        @JsonProperty("requirement_type")
        CourseTrainingRequirementType requirementType,

        @Schema(
                description = "**[REQUIRED]** Concise label for the resource or material.",
                example = "Lab safety goggles",
                maxLength = 255,
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "Requirement name is required")
        @Size(max = 255, message = "Requirement name must not exceed 255 characters")
        @JsonProperty("name")
        String name,

        @Schema(
                description = "**[OPTIONAL]** Extra details or specifications for the resource.",
                example = "Provide ANSI Z87.1 certified safety goggles for each participant.",
                maxLength = 2000,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        @Size(max = 2000, message = "Description must not exceed 2000 characters")
        @JsonProperty("description")
        String description,

        @Schema(
                description = "**[OPTIONAL]** Quantity needed for each class session.",
                example = "15",
                minimum = "1",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        @Min(value = 1, message = "Quantity must be at least 1")
        @JsonProperty("quantity")
        Integer quantity,

        @Schema(
                description = "**[OPTIONAL]** Unit that the quantity refers to.",
                example = "sets",
                maxLength = 50,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        @Size(max = 50, message = "Unit must not exceed 50 characters")
        @JsonProperty("unit")
        String unit,

        @Schema(
                description = "**[OPTIONAL]** Party responsible for providing this requirement.",
                example = "organisation",
                allowableValues = {"course_creator", "instructor", "organisation", "student"},
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        @JsonProperty("provided_by")
        CourseTrainingRequirementProvider providedBy,

        @Schema(
                description = "**[OPTIONAL]** Indicates if the requirement is mandatory.",
                example = "true",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        @JsonProperty("is_mandatory")
        Boolean isMandatory,

        @Schema(
                description = "**[READ-ONLY]** Timestamp when the requirement was created.",
                format = "date-time",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty(value = "created_date", access = JsonProperty.Access.READ_ONLY)
        LocalDateTime createdDate,

        @Schema(
                description = "**[READ-ONLY]** User who created the requirement.",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty(value = "created_by", access = JsonProperty.Access.READ_ONLY)
        String createdBy,

        @Schema(
                description = "**[READ-ONLY]** Timestamp when the requirement was last updated.",
                format = "date-time",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty(value = "updated_date", access = JsonProperty.Access.READ_ONLY)
        LocalDateTime updatedDate,

        @Schema(
                description = "**[READ-ONLY]** User who last updated the requirement.",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty(value = "updated_by", access = JsonProperty.Access.READ_ONLY)
        String updatedBy
) {
}
