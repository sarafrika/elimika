package apps.sarafrika.elimika.coursecreator.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public record CourseCreatorCategoryPreferenceDTO(
        @JsonProperty("category_uuid")
        UUID categoryUuid
) {
}
