package apps.sarafrika.elimika.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Builder
public record CourseDTO(

        @JsonProperty("uuid")
        UUID uuid,

        @NotBlank(message = "Name is required")
        @Size(max = 255, message = "Name must not exceed 255 characters")
        @JsonProperty("name")
        String name,

        @NotBlank(message = "Code is required")
        @Size(max = 50, message = "Code must not exceed 50 characters")
        @JsonProperty("code")
        String code,

        @Size(max = 1000, message = "Description must not exceed 1000 characters")
        @JsonProperty("description")
        String description,

        @Size(max = 500, message = "Thumbnail URL must not exceed 500 characters")
        @JsonProperty("thumbnail_url")
        String thumbnailUrl,

        @PositiveOrZero(message = "Duration must be a positive number")
        @JsonProperty("duration_hours")
        BigDecimal durationHours,

        @NotBlank(message = "Difficulty level is required")
        @Size(max = 50, message = "Difficulty level must not exceed 50 characters")
        @JsonProperty("difficulty_level")
        String difficultyLevel,

        @JsonProperty("is_free")
        boolean isFree,

        @PositiveOrZero(message = "Original price must be a positive number or zero")
        @JsonProperty("original_price")
        BigDecimal originalPrice,

        @PositiveOrZero(message = "Sale price must be a positive number or zero")
        @JsonProperty("sale_price")
        BigDecimal salePrice,

        @Min(value = 0, message = "Minimum age must be 0 or greater")
        @JsonProperty("min_age")
        int minAge,

        @Min(value = 0, message = "Maximum age must be 0 or greater")
        @JsonProperty("max_age")
        int maxAge,

        @Min(value = 1, message = "Class size must be 1 or greater")
        @JsonProperty("class_size")
        int classSize,

        @JsonProperty("instructors")
        List<UUID> instructorIds,

        @JsonProperty("learning_objectives")
        List<CourseLearningObjectiveDTO> learningObjectives,

        @JsonProperty("course_categories")
        List<CourseCategoryDTO> courseCategories,

        @JsonProperty(value = "created_by", access = JsonProperty.Access.READ_ONLY)
        String createdBy,

        @JsonProperty(value = "updated_by", access = JsonProperty.Access.READ_ONLY)
        String lastModifiedBy,

        @JsonProperty(value = "created_date", access = JsonProperty.Access.READ_ONLY)
        LocalDateTime createdDate,

        @JsonProperty(value = "updated_date", access = JsonProperty.Access.READ_ONLY)
        LocalDateTime lastModifiedDate

) {
}
