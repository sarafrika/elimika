package apps.sarafrika.elimika.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Facet counts for the catalogue filters. Each group is counted under every other active filter with
 * its own selection left out (disjunctive faceting), so picking a value never zeroes its siblings.
 */
@Schema(name = "CatalogueFacets", description = "Counts per filter value; each group ignores its own selection.")
public record CatalogueFacets(
        @JsonProperty("show") CatalogueShowFacet show,
        @Schema(description = "Categories with at least one match, plus any selected ones; most matches first.")
        @JsonProperty("category") List<CatalogueCategoryFacet> category,
        @JsonProperty("level") CatalogueLevelFacet level,
        @JsonProperty("price") CataloguePriceFacet price
) {
}
