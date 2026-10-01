package apps.sarafrika.elimika.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Matches per level, under every active filter except {@code level}. A programme counts under every
 * level one of its member courses has.
 */
@Schema(name = "CatalogueLevelFacet", description = "Matches per level, ignoring the level selection.")
public record CatalogueLevelFacet(
        @JsonProperty("beginner") long beginner,
        @JsonProperty("intermediate") long intermediate,
        @JsonProperty("advanced") long advanced
) {
}
