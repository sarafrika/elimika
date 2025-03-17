package apps.sarafrika.elimika.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public record CourseCategoryDTO(
        @JsonProperty("course_uuid")
        UUID courseUuid,
        @JsonProperty("category_uuid")
        UUID categoryUuid
) {
}
