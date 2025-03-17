package apps.sarafrika.elimika.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Builder
public record LessonDTO(

        @JsonProperty("uuid")
        UUID uuid,

        @NotBlank(message = "Title is required")
        @Size(max = 255, message = "Title must not exceed 255 characters")
        @JsonProperty("title")
        String title,

        @Size(max = 1000, message = "Description must not exceed 1000 characters")
        @JsonProperty("description")
        String description,

        @PositiveOrZero(message = "Lesson order must be zero or a positive number")
        @JsonProperty("lesson_order")
        int lessonOrder,

        @JsonProperty("is_published")
        boolean isPublished,

        @JsonProperty("course_uuid")
        UUID courseId,

        @JsonProperty("content")
        List<LessonContentDTO> content,

        @JsonProperty("resources")
        List<LessonResourceDTO> resources,

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
