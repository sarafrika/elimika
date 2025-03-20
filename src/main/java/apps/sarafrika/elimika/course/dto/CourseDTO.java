package apps.sarafrika.elimika.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Schema(
        description = "Represents a complete course offering with all its associated metadata and content",
        name = "Course"
)
@Builder
public record CourseDTO(

        @JsonProperty("uuid")
        @Schema(
                description = "Unique identifier for the course",
                example = "123e4567-e89b-12d3-a456-426614174000",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        UUID uuid,

        @NotBlank(message = "Name is required")
        @Size(max = 255, message = "Name must not exceed 255 characters")
        @JsonProperty("name")
        @Schema(
                description = "Full name of the course as displayed to students",
                example = "Advanced Java Programming for Enterprise Applications",
                requiredMode = Schema.RequiredMode.REQUIRED,
                maxLength = 255
        )
        String name,

        @NotBlank(message = "Code is required")
        @Size(max = 50, message = "Code must not exceed 50 characters")
        @JsonProperty("code")
        @Schema(
                description = "Unique course code used for identification and reference",
                example = "JAVA-ENT-301",
                requiredMode = Schema.RequiredMode.REQUIRED,
                maxLength = 50
        )
        String code,

        @Size(max = 1000, message = "Description must not exceed 1000 characters")
        @JsonProperty("description")
        @Schema(
                description = "Detailed description of the course content and learning outcomes",
                example = "This comprehensive course covers advanced Java concepts including concurrency, design patterns, and enterprise integration patterns...",
                maxLength = 1000
        )
        String description,

        @Size(max = 500, message = "Thumbnail URL must not exceed 500 characters")
        @JsonProperty("thumbnail_url")
        @Schema(
                description = "URL to the course thumbnail image",
                example = "https://example.com/courses/java-enterprise/thumbnail.jpg",
                maxLength = 500
        )
        String thumbnailUrl,

        @PositiveOrZero(message = "Duration must be a positive number")
        @JsonProperty("duration_hours")
        @Schema(
                description = "Estimated total duration of the course in hours",
                example = "42.5",
                minimum = "0"
        )
        BigDecimal durationHours,

        @NotBlank(message = "Difficulty level is required")
        @Size(max = 50, message = "Difficulty level must not exceed 50 characters")
        @JsonProperty("difficulty_level")
        @Schema(
                description = "Difficulty level of the course content",
                example = "Advanced",
                requiredMode = Schema.RequiredMode.REQUIRED,
                maxLength = 50,
                allowableValues = {"Beginner", "Intermediate", "Advanced", "Expert"}
        )
        String difficultyLevel,

        @JsonProperty("is_free")
        @Schema(
                description = "Indicates whether the course is available for free",
                example = "false",
                defaultValue = "false"
        )
        boolean isFree,

        @PositiveOrZero(message = "Original price must be a positive number or zero")
        @JsonProperty("original_price")
        @Schema(
                description = "Original price of the course before any discounts",
                example = "199.99",
                minimum = "0"
        )
        BigDecimal originalPrice,

        @PositiveOrZero(message = "Sale price must be a positive number or zero")
        @JsonProperty("sale_price")
        @Schema(
                description = "Current sale price of the course (if on sale)",
                example = "149.99",
                minimum = "0"
        )
        BigDecimal salePrice,

        @Min(value = 0, message = "Minimum age must be 0 or greater")
        @JsonProperty("min_age")
        @Schema(
                description = "Minimum recommended age for students taking this course",
                example = "16",
                minimum = "0"
        )
        int minAge,

        @Min(value = 0, message = "Maximum age must be 0 or greater")
        @JsonProperty("max_age")
        @Schema(
                description = "Maximum recommended age for students taking this course (0 means no upper limit)",
                example = "0",
                minimum = "0"
        )
        int maxAge,

        @Min(value = 1, message = "Class size must be 1 or greater")
        @JsonProperty("class_size")
        @Schema(
                description = "Maximum number of students allowed in the course",
                example = "30",
                minimum = "1"
        )
        int classSize,

        @JsonProperty("instructors")
        @ArraySchema(
                schema = @Schema(type = "string", format = "uuid",
                        description = "List of instructor UUIDs assigned to teach this course"),
                minItems = 0
        )
        List<UUID> instructorIds,

        @JsonProperty("learning_objectives")
        @ArraySchema(
                schema = @Schema(implementation = CourseLearningObjectiveDTO.class,
                        description = "List of learning objectives for this course"),
                minItems = 0
        )
        List<CourseLearningObjectiveDTO> learningObjectives,

        @JsonProperty("course_categories")
        @ArraySchema(
                schema = @Schema(implementation = CourseCategoryDTO.class,
                        description = "Categories this course belongs to"),
                minItems = 0
        )
        List<CourseCategoryDTO> courseCategories,

        @JsonProperty(value = "created_by", access = JsonProperty.Access.READ_ONLY)
        @Schema(
                description = "Username of the user who created this course",
                example = "john.doe",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        String createdBy,

        @JsonProperty(value = "updated_by", access = JsonProperty.Access.READ_ONLY)
        @Schema(
                description = "Username of the user who last modified this course",
                example = "jane.smith",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        String lastModifiedBy,

        @JsonProperty(value = "created_date", access = JsonProperty.Access.READ_ONLY)
        @Schema(
                description = "Timestamp when the course was created",
                example = "2025-03-20T10:30:45.123Z",
                accessMode = Schema.AccessMode.READ_ONLY,
                type = "string",
                format = "date-time"
        )
        LocalDateTime createdDate,

        @JsonProperty(value = "updated_date", access = JsonProperty.Access.READ_ONLY)
        @Schema(
                description = "Timestamp when the course was last updated",
                example = "2025-03-21T14:15:22.456Z",
                accessMode = Schema.AccessMode.READ_ONLY,
                type = "string",
                format = "date-time"
        )
        LocalDateTime lastModifiedDate

) {
}