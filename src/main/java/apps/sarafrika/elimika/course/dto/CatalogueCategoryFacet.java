package apps.sarafrika.elimika.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/** One category value with its match count, under every active filter except {@code category_uuid}. */
@Schema(name = "CatalogueCategoryFacet", description = "A category and how many results it would show.")
public record CatalogueCategoryFacet(
        @JsonProperty("uuid") UUID uuid,
        @JsonProperty("name") String name,
        @JsonProperty("count") long count
) {
}
