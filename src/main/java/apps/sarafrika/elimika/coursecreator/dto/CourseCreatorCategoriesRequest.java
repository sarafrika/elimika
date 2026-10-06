package apps.sarafrika.elimika.coursecreator.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record CourseCreatorCategoriesRequest(
        @NotNull(message = "Category UUIDs are required")
        @JsonProperty("category_uuids")
        List<UUID> categoryUuids
) {
}
