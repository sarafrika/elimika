package apps.sarafrika.elimika.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

@Schema(name = "CategoryInterestsRequest", description = "The full set of interests; replaces what was saved before")
public record CategoryInterestsRequest(
        @Schema(description = "Course category UUIDs. Duplicates are dropped; an empty list clears the interests.")
        @NotNull(message = "Category UUIDs are required")
        @JsonProperty("category_uuids")
        List<UUID> categoryUuids
) {
}
