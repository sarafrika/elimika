package apps.sarafrika.elimika.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/** Free and paid matches, under every active filter except {@code price}. */
@Schema(name = "CataloguePriceFacet", description = "Free and paid matches, ignoring the price selection.")
public record CataloguePriceFacet(
        @JsonProperty("free") long free,
        @JsonProperty("paid") long paid
) {
}
