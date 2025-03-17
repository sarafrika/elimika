package apps.sarafrika.elimika.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record LessonContentDTO(

        @JsonProperty("uuid")
        UUID uuid,

        @NotBlank(message = "Title is required")
        @Size(max = 255, message = "Title must not exceed 255 characters")
        @JsonProperty("title")
        String title,

        @NotBlank(message = "Content is required")
        @JsonProperty("content")
        String content,

        @PositiveOrZero(message = "Display order must be zero or a positive number")
        @JsonProperty("display_order")
        int displayOrder,

        @Min(value = 0, message = "Duration must be zero or a positive number")
        @JsonProperty("duration")
        int duration,

        @JsonProperty("lesson_id")
        Long lessonId,

        @JsonProperty("content_type_id")
        Long contentTypeId,

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
