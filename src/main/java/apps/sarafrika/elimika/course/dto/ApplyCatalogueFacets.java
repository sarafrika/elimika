package apps.sarafrika.elimika.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/** Facet counts for the apply-to-train catalogue; each group ignores its own selection. */
@Schema(name = "ApplyCatalogueFacets", description = "Counts per filter value; each group ignores its own selection.")
public record ApplyCatalogueFacets(
        @JsonProperty("show") CatalogueShowFacet show,
        @Schema(description = "Categories with at least one match, plus any selected ones; most matches first.")
        @JsonProperty("category") List<CatalogueCategoryFacet> category,
        @JsonProperty("fit") ApplyCatalogueFitFacet fit
) {
}
